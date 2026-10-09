package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.engine.DeliveryFeedbackBus

class SmsDeliveryReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SMS_SENT = "com.example.SMS_SENT_ACTION"
        const val ACTION_SMS_DELIVERED = "com.example.SMS_DELIVERED_ACTION"
        const val EXTRA_CAMPAIGN_ID = "extra_campaign_id"
        const val EXTRA_RECIPIENT_ID = "extra_recipient_id"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val phone = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        val recipientId = intent.getLongExtra(EXTRA_RECIPIENT_ID, -1L)
        val code = resultCode

        Log.d("SmsDeliveryReceiver", "Broadcast intent received: action=$action, recipientId=$recipientId, phone=$phone, resultCode=$code")

        // Forward broadcast intent feedback to central telemetry bus
        DeliveryFeedbackBus.handleBroadcast(
            context = context,
            intent = intent,
            resultCode = code
        )
    }
}
