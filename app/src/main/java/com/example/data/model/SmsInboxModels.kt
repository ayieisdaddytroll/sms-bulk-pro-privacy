package com.example.data.model

data class SmsMessageItem(
    val id: Long,
    val threadId: Long,
    val address: String,
    val senderName: String = "",
    val body: String,
    val dateEpoch: Long,
    val isRead: Boolean,
    val isIncoming: Boolean,
    val simSlot: Int = 0
)

data class SmsConversation(
    val threadId: Long,
    val address: String,
    val senderName: String,
    val lastMessage: String,
    val lastDateEpoch: Long,
    val unreadCount: Int,
    val totalMessages: Int
)
