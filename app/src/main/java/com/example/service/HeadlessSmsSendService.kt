package com.example.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.telephony.SmsManager
import android.telephony.TelephonyManager

class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            val extras = intent.extras
            if (extras != null) {
                val message = extras.getString(Intent.EXTRA_TEXT)
                val uri: Uri? = intent.data
                val recipientNumber = uri?.schemeSpecificPart?.let { getNumberFromUri(it) }

                if (!recipientNumber.isNullOrBlank() && !message.isNullOrBlank()) {
                    try {
                        val smsManager = getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                        smsManager.sendTextMessage(recipientNumber, null, message, null, null)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun getNumberFromUri(data: String): String {
        return data.substringBefore('?')
    }
}
