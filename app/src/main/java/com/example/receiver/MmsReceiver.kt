package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION ||
            intent.action == Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION
        ) {
            // Primary SMS apps must handle WAP PUSH deliveries
        }
    }
}
