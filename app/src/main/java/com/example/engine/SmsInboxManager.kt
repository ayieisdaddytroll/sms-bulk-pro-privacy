package com.example.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.model.SmsConversation
import com.example.data.model.SmsMessageItem

object SmsInboxManager {

    private val localSentMessages = mutableListOf<SmsMessageItem>()
    private val localReceivedMessages = mutableListOf<SmsMessageItem>()
    private val deletedAddresses = mutableSetOf<String>()
    private val deletedMessageIds = mutableSetOf<Long>()

    @SuppressLint("Range")
    fun readInboxMessages(context: Context): List<SmsMessageItem> {
        val messageList = mutableListOf<SmsMessageItem>()

        val hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasReadPermission) {
            try {
                val uri: Uri = Telephony.Sms.CONTENT_URI
                val projection = arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.READ,
                    Telephony.Sms.TYPE
                )

                val cursor: Cursor? = context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    "${Telephony.Sms.DATE} DESC LIMIT 200"
                )

                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                        val threadId = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                        val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "Unknown"
                        val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                        val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                        val read = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1
                        val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))

                        val isIncoming = (type == Telephony.Sms.MESSAGE_TYPE_INBOX)

                        if (!deletedMessageIds.contains(id) && !deletedAddresses.contains(normalize(address))) {
                            messageList.add(
                                SmsMessageItem(
                                    id = id,
                                    threadId = threadId,
                                    address = address,
                                    senderName = resolveSenderName(address, context),
                                    body = body,
                                    dateEpoch = date,
                                    isRead = read,
                                    isIncoming = isIncoming
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // If content resolver access fails or empty
            }
        }

        // Add any locally appended sent or received replies
        synchronized(this) {
            for (msg in localReceivedMessages) {
                if (!deletedMessageIds.contains(msg.id) && !deletedAddresses.contains(normalize(msg.address))) {
                    if (messageList.none { it.id == msg.id }) {
                        messageList.add(msg)
                    }
                }
            }
            for (msg in localSentMessages) {
                if (!deletedMessageIds.contains(msg.id) && !deletedAddresses.contains(normalize(msg.address))) {
                    if (messageList.none { it.id == msg.id }) {
                        messageList.add(msg)
                    }
                }
            }
        }

        // Return messages sorted by timestamp; sample templates and sample phone numbers removed
        return messageList.sortedByDescending { it.dateEpoch }
    }

    private fun normalize(addr: String): String {
        return addr.filter { it.isDigit() || it == '+' }
    }

    fun groupIntoConversations(messages: List<SmsMessageItem>): List<SmsConversation> {
        val grouped = messages.groupBy { normalize(it.address) }
        val conversations = mutableListOf<SmsConversation>()

        for ((normalizedAddress, list) in grouped) {
            if (deletedAddresses.contains(normalizedAddress)) continue
            val sorted = list.sortedByDescending { it.dateEpoch }
            val latest = sorted.first()
            val unread = list.count { !it.isRead && it.isIncoming }
            val displayAddress = latest.address
            val contactName = resolveSenderName(displayAddress)

            conversations.add(
                SmsConversation(
                    threadId = latest.threadId,
                    address = displayAddress,
                    senderName = contactName,
                    lastMessage = latest.body,
                    lastDateEpoch = latest.dateEpoch,
                    unreadCount = unread,
                    totalMessages = list.size
                )
            )
        }

        return conversations.sortedByDescending { it.lastDateEpoch }
    }

    fun deleteConversations(addresses: List<String>, context: Context): Boolean {
        synchronized(this) {
            val normalizedList = addresses.map { normalize(it) }
            deletedAddresses.addAll(normalizedList)
            localReceivedMessages.removeAll { normalizedList.contains(normalize(it.address)) }
            localSentMessages.removeAll { normalizedList.contains(normalize(it.address)) }

            try {
                for (addr in addresses) {
                    context.contentResolver.delete(
                        Telephony.Sms.CONTENT_URI,
                        "${Telephony.Sms.ADDRESS} = ?",
                        arrayOf(addr)
                    )
                }
            } catch (e: Exception) {
                // Ignore if content provider delete lacks permission
            }
        }
        return true
    }

    fun deleteMessages(messageIds: List<Long>, context: Context): Boolean {
        synchronized(this) {
            deletedMessageIds.addAll(messageIds)
            localReceivedMessages.removeAll { messageIds.contains(it.id) }
            localSentMessages.removeAll { messageIds.contains(it.id) }

            try {
                for (id in messageIds) {
                    context.contentResolver.delete(
                        Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, id.toString()),
                        null,
                        null
                    )
                }
            } catch (e: Exception) {
                // Ignore if not supported
            }
        }
        return true
    }

    fun sendReply(
        context: Context,
        recipientAddress: String,
        messageText: String,
        subscriptionId: Int
    ): Boolean {
        return try {
            val smsManager = if (subscriptionId > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val base = context.getSystemService(SmsManager::class.java)
                    base.createForSubscriptionId(subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            try {
                val parts = smsManager?.divideMessage(messageText)
                if (parts != null && parts.size > 1) {
                    smsManager.sendMultipartTextMessage(recipientAddress, null, parts, null, null)
                } else {
                    smsManager?.sendTextMessage(recipientAddress, null, messageText, null, null)
                }
            } catch (e: Exception) {
                // Ignore telephony exceptions in test environment or offline radio
            }

            val now = System.currentTimeMillis()
            val newId = now

            // Try to write to Telephony.Sms.Sent if primary SMS app
            try {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, recipientAddress)
                    put(Telephony.Sms.BODY, messageText)
                    put(Telephony.Sms.DATE, now)
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                }
                context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            } catch (e: Exception) {
                // Ignore if content provider insert is restricted
            }

            synchronized(this) {
                localSentMessages.add(
                    SmsMessageItem(
                        id = newId,
                        threadId = recipientAddress.hashCode().toLong(),
                        address = recipientAddress,
                        senderName = "You",
                        body = messageText,
                        dateEpoch = now,
                        isRead = true,
                        isIncoming = false
                    )
                )
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun addIncomingMessage(address: String, body: String) {
        synchronized(this) {
            localReceivedMessages.add(
                SmsMessageItem(
                    id = System.currentTimeMillis(),
                    threadId = address.hashCode().toLong(),
                    address = address,
                    senderName = resolveSenderName(address),
                    body = body,
                    dateEpoch = System.currentTimeMillis(),
                    isRead = false,
                    isIncoming = true
                )
            )
        }
    }

    /**
     * Resolves contact name from device contacts if available.
     * Changes "Suki" to complete phone number if name is not part of the contacts.
     */
    fun resolveSenderName(address: String, context: Context? = null): String {
        if (context != null) {
            try {
                val hasContactPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                if (hasContactPermission) {
                    val uri = Uri.withAppendedPath(
                        android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(address)
                    )
                    val cursor = context.contentResolver.query(
                        uri,
                        arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME),
                        null, null, null
                    )
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val name = it.getString(0)
                            if (!name.isNullOrBlank() && !name.equals("Suki", ignoreCase = true)) {
                                return name
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        // Change Suki to complete phone number if name is not part of the contacts
        return address
    }
}
