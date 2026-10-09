package com.example.engine

object TemplateEngine {

    val AVAILABLE_PLACEHOLDERS = listOf(
        PlaceholderInfo("{Name}", "Recipient Full Name", "Maria Santos"),
        PlaceholderInfo("{Amount}", "Payment / Invoice Amount", "₱1,250.00")
    )

    data class PlaceholderInfo(
        val tag: String,
        val description: String,
        val exampleValue: String
    )

    /**
     * Replaces dynamic tokens {Name} and {Amount}.
     * If recipientName is not part of contacts (or is blank or "Suki"),
     * changes to the complete phone number.
     */
    fun resolveMessageBody(
        template: String,
        recipientName: String,
        amount: String,
        phoneNumber: String = ""
    ): String {
        val resolvedName = if (recipientName.isNotBlank() && !recipientName.equals("Suki", ignoreCase = true)) {
            recipientName.trim()
        } else if (phoneNumber.isNotBlank()) {
            phoneNumber.trim()
        } else {
            ""
        }
        return template
            .replace("{Name}", resolvedName)
            .replace("{Amount}", amount.ifEmpty { "₱0.00" })
    }

    /**
     * Resolves message for preview.
     */
    fun resolvePreview(
        template: String,
        recipientName: String = "Maria Santos",
        phoneNumber: String = "09171234567",
        amount: String = "₱1,250.00"
    ): String {
        return resolveMessageBody(
            template = template,
            recipientName = recipientName,
            amount = amount,
            phoneNumber = phoneNumber
        )
    }

    /**
     * Extracts placeholders found in the text ({Name} and {Amount} only).
     */
    fun extractPlaceholders(text: String): List<String> {
        val list = mutableListOf<String>()
        if (text.contains("{Name}")) list.add("{Name}")
        if (text.contains("{Amount}")) list.add("{Amount}")
        return list
    }

    data class SmsLengthInfo(
        val charCount: Int,
        val segmentCount: Int,
        val charsRemainingInSegment: Int,
        val isUnicode: Boolean
    )

    fun calculateSmsLength(text: String): SmsLengthInfo {
        val isUnicode = text.any { it.code > 127 }
        val len = text.length
        val maxSingle = if (isUnicode) 70 else 160
        val maxConcat = if (isUnicode) 67 else 153

        if (len == 0) {
            return SmsLengthInfo(0, 1, maxSingle, isUnicode)
        }

        if (len <= maxSingle) {
            return SmsLengthInfo(
                charCount = len,
                segmentCount = 1,
                charsRemainingInSegment = maxSingle - len,
                isUnicode = isUnicode
            )
        }

        val segments = (len + maxConcat - 1) / maxConcat
        val remaining = (segments * maxConcat) - len
        return SmsLengthInfo(
            charCount = len,
            segmentCount = segments,
            charsRemainingInSegment = remaining,
            isUnicode = isUnicode
        )
    }
}

// Backward compatibility alias for any existing references
typealias LinkTrackerEngine = TemplateEngine
