package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.CsvParser
import com.example.engine.SmsInboxManager
import com.example.engine.TemplateEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SMSBlast Pro", appName)
    }

    @Test
    fun `contact import service parses and maps custom columns to recipient list`() {
        val rawCsv = """
            Account No,Customer Name,Mobile No,Amount Due
            A001,Juan Dela Cruz,09171234567,₱450.00
            A002,,+639189876543,₱1,200.00
            A003,Maria Santos,09171234567,₱450.00
        """.trimIndent()
        val sheet = com.example.engine.ContactImportService.parseRawText(rawCsv, "test.csv")
        assertEquals(4, sheet.headers.size)

        val result = com.example.engine.ContactImportService.mapToRecipients(
            raw = sheet,
            config = com.example.engine.ColumnMappingConfig(
                phoneColumnIndex = 2,
                nameColumnIndex = 1,
                amountColumnIndex = 3,
                skipFirstRow = true,
                removeDuplicates = true,
                autoNormalizePhilippine = true
            )
        )

        assertEquals(2, result.validContacts.size)
        assertEquals(1, result.duplicatesRemoved)
        assertEquals("09171234567", result.validContacts[0].phoneNumber)
        assertEquals("Juan Dela Cruz", result.validContacts[0].name)
        // Second row had no name -> name is empty string so complete phone number is used in message resolution
        assertEquals("", result.validContacts[1].name)
        assertEquals("+639189876543", result.validContacts[1].phoneNumber)
    }

    @Test
    fun `template engine changes suki to complete phone number if name is not part of contacts`() {
        val template = "Kumusta {Name}, your pending balance is {Amount}."

        // Case 1: Name is not part of contacts (empty string)
        val resolvedWithPhone = TemplateEngine.resolveMessageBody(
            template = template,
            recipientName = "",
            amount = "₱500.00",
            phoneNumber = "09171234567"
        )
        assertEquals("Kumusta 09171234567, your pending balance is ₱500.00.", resolvedWithPhone)

        // Case 2: Name is explicitly "Suki"
        val resolvedFromSuki = TemplateEngine.resolveMessageBody(
            template = template,
            recipientName = "Suki",
            amount = "₱1,200.00",
            phoneNumber = "+639189876543"
        )
        assertEquals("Kumusta +639189876543, your pending balance is ₱1,200.00.", resolvedFromSuki)

        // Case 3: Name is provided in contacts
        val resolvedWithName = TemplateEngine.resolveMessageBody(
            template = template,
            recipientName = "Maria Santos",
            amount = "₱1,200.00",
            phoneNumber = "09171234567"
        )
        assertEquals("Kumusta Maria Santos, your pending balance is ₱1,200.00.", resolvedWithName)
    }

    @Test
    fun `csv parser parses Philippine mobile numbers and converts currency to Pesos`() {
        val csv = """
            phone,name,amount
            09171234567,Juan Dela Cruz,₱450.00
            +639189876543,Maria Santos,₱1,200.00
            09171234567,Juan Duplicate,₱450.00
        """.trimIndent()
        val result = CsvParser.parseText(csv)
        assertEquals(2, result.validContacts.size)
        assertEquals(1, result.duplicatesRemoved)
        assertEquals("₱450.00", result.validContacts[0].amount)
        assertEquals("09171234567", result.validContacts[0].phoneNumber)
        assertEquals("+639189876543", result.validContacts[1].phoneNumber)
    }

    @Test
    fun `csv parser normalizes 10-digit Philippine number starting with 9 to 09 prefix`() {
        val normalized = CsvParser.normalizePhoneNumber("9171234567")
        assertEquals("09171234567", normalized)
        assertTrue(CsvParser.isValidPhoneNumber(normalized))
    }

    @Test
    fun `template engine dynamic token replacement with Name and Amount only in Pesos`() {
        val template = "Hello {Name}, your statement balance of {Amount} is due."
        val resolved = TemplateEngine.resolveMessageBody(
            template = template,
            recipientName = "Juan Dela Cruz",
            amount = "₱1,500.00"
        )
        assertEquals("Hello Juan Dela Cruz, your statement balance of ₱1,500.00 is due.", resolved)
    }

    @Test
    fun `placeholder extraction finds Name and Amount only`() {
        val body = "Hi {Name}, your code {Code} and amount {Amount} are ready."
        val placeholders = TemplateEngine.extractPlaceholders(body)
        assertEquals(2, placeholders.size)
        assertTrue(placeholders.contains("{Name}"))
        assertTrue(placeholders.contains("{Amount}"))
    }

    @Test
    fun `resolvePreview substitutes sample data for Name and Amount in Philippine format`() {
        val template = "Dear {Name}, you have {Amount} due."
        val preview = TemplateEngine.resolvePreview(template)
        assertTrue(preview.contains("Maria Santos"))
        assertTrue(preview.contains("₱1,250.00"))
    }

    @Test
    fun `sim card manager returns available sim info`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sims = com.example.engine.SimCardManager.getAvailableSimCards(context)
        assertTrue(sims.isNotEmpty())
    }

    @Test
    fun `inbox manager loads Philippine messages and groups into conversations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        SmsInboxManager.addIncomingMessage("+639171234567", "Payment confirmed.")
        val messages = SmsInboxManager.readInboxMessages(context)
        assertTrue(messages.isNotEmpty())

        val conversations = SmsInboxManager.groupIntoConversations(messages)
        assertTrue(conversations.isNotEmpty())
        assertTrue(conversations.any { it.address.contains("0917") || it.address.contains("+63") })
    }

    @Test
    fun `delivery feedback bus processes intent broadcast feedback for sent and failed states`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sentIntent = android.content.Intent(com.example.receiver.SmsDeliveryReceiver.ACTION_SMS_SENT).apply {
            putExtra(com.example.receiver.SmsDeliveryReceiver.EXTRA_CAMPAIGN_ID, 99L)
            putExtra(com.example.receiver.SmsDeliveryReceiver.EXTRA_RECIPIENT_ID, 101L)
            putExtra(com.example.receiver.SmsDeliveryReceiver.EXTRA_PHONE_NUMBER, "09171234567")
        }

        // Test RESULT_OK -> SENT
        com.example.engine.DeliveryFeedbackBus.handleBroadcast(
            context = context,
            intent = sentIntent,
            resultCode = android.app.Activity.RESULT_OK
        )
        val summary1 = com.example.engine.DeliveryFeedbackBus.summary.value
        assertTrue(summary1.sentAcks >= 1)
        assertEquals(com.example.data.model.DeliveryStatus.SENT, summary1.lastEvent?.status)
        assertTrue(summary1.lastEvent?.statusDescription?.contains("confirmed") == true)

        // Test RESULT_ERROR_GENERIC_FAILURE -> FAILED
        val failedIntent = android.content.Intent(com.example.receiver.SmsDeliveryReceiver.ACTION_SMS_SENT).apply {
            putExtra(com.example.receiver.SmsDeliveryReceiver.EXTRA_CAMPAIGN_ID, 99L)
            putExtra(com.example.receiver.SmsDeliveryReceiver.EXTRA_RECIPIENT_ID, 102L)
            putExtra(com.example.receiver.SmsDeliveryReceiver.EXTRA_PHONE_NUMBER, "09189876543")
        }
        com.example.engine.DeliveryFeedbackBus.handleBroadcast(
            context = context,
            intent = failedIntent,
            resultCode = android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE
        )
        val summary2 = com.example.engine.DeliveryFeedbackBus.summary.value
        assertTrue(summary2.failedAcks >= 1)
        assertEquals(com.example.data.model.DeliveryStatus.FAILED, summary2.lastEvent?.status)
    }

    @Test
    fun `inbox manager sendReply adds message to conversation thread`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testRecipient = "09172341040"
        val replyText = "We received your inquiry regarding your ₱1,250.00 balance."

        val success = SmsInboxManager.sendReply(
            context = context,
            recipientAddress = testRecipient,
            messageText = replyText,
            subscriptionId = -1
        )
        assertTrue(success)

        val updated = SmsInboxManager.readInboxMessages(context)
        assertTrue(updated.any { it.address == testRecipient && it.body == replyText && !it.isIncoming })
    }

    @Test
    fun `inbox manager deleteMultipleConversations removes specified addresses`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testRecipient = "09183451030"
        val success = SmsInboxManager.deleteConversations(listOf(testRecipient), context)
        assertTrue(success)

        val updated = SmsInboxManager.readInboxMessages(context)
        assertFalse(updated.any { it.address == testRecipient })
    }
}
