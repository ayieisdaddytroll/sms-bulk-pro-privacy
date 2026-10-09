package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // Billing, Reminders, Notices, Updates, Custom
    val body: String,
    val sampleVariables: String = "{Name}, {Amount}",
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val createdAtEpoch: Long = System.currentTimeMillis()
)
