package com.example.engine

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ParsedContact(
    val phoneNumber: String,
    val name: String = "",
    val amount: String = "₱0.00"
)

data class CsvParseResult(
    val validContacts: List<ParsedContact>,
    val totalRowsParsed: Int,
    val duplicatesRemoved: Int,
    val invalidRowsSkipped: Int,
    val previewHeaders: List<String> = emptyList(),
    val sampleRows: List<List<String>> = emptyList()
)

object CsvParser {

    fun parseStream(inputStream: InputStream): CsvParseResult {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines()
        return parseLines(lines)
    }

    fun parseText(rawText: String): CsvParseResult {
        val lines = rawText.lines()
        return parseLines(lines)
    }

    private fun parseLines(rawLines: List<String>): CsvParseResult {
        val cleanLines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanLines.isEmpty()) {
            return CsvParseResult(emptyList(), 0, 0, 0)
        }

        // Detect delimiter
        val firstLine = cleanLines.first()
        val delimiter = when {
            firstLine.contains('\t') -> "\t"
            firstLine.contains(';') -> ";"
            firstLine.contains('|') -> "|"
            else -> ","
        }

        var headerIndexPhone = -1
        var headerIndexName = -1
        var headerIndexAmount = -1
        var startIndex = 0

        val firstRowTokens = splitCsvRow(firstLine, delimiter)
        val hasHeader = firstRowTokens.any { token ->
            val lower = token.lowercase().trim()
            lower.contains("phone") || lower.contains("mobile") || lower.contains("number") ||
                    lower.contains("tel") || lower.contains("contact") || lower.contains("name") ||
                    lower.contains("amount") || lower.contains("price") || lower.contains("balance") ||
                    lower.contains("bayad") || lower.contains("singil")
        }

        if (hasHeader) {
            startIndex = 1
            firstRowTokens.forEachIndexed { index, header ->
                val lower = header.lowercase().trim()
                if (headerIndexPhone == -1 && (lower.contains("phone") || lower.contains("mobile") || lower.contains("num") || lower.contains("tel"))) {
                    headerIndexPhone = index
                } else if (headerIndexName == -1 && (lower.contains("name") || lower.contains("recipient") || lower.contains("contact") || lower.contains("pangalan"))) {
                    headerIndexName = index
                } else if (headerIndexAmount == -1 && (lower.contains("amount") || lower.contains("price") || lower.contains("due") || lower.contains("balance") || lower.contains("fee") || lower.contains("bayad"))) {
                    headerIndexAmount = index
                }
            }
        }

        // If phone column wasn't determined by header, look at token characteristics
        if (headerIndexPhone == -1) {
            headerIndexPhone = 0
            if (firstRowTokens.size > 1) {
                headerIndexName = 1
            }
            if (firstRowTokens.size > 2) {
                headerIndexAmount = 2
            }
        }

        val seenPhones = HashSet<String>()
        val validContacts = ArrayList<ParsedContact>()
        var duplicates = 0
        var invalid = 0
        var totalRows = 0
        val sampleList = ArrayList<List<String>>()

        for (i in startIndex until cleanLines.size) {
            val line = cleanLines[i]
            val tokens = splitCsvRow(line, delimiter)
            if (tokens.isEmpty()) continue
            totalRows++

            if (sampleList.size < 5) {
                sampleList.add(tokens)
            }

            val rawPhone = if (tokens.size > headerIndexPhone && headerIndexPhone >= 0) tokens[headerIndexPhone] else ""
            val name = if (headerIndexName in tokens.indices) tokens[headerIndexName].trim() else ""
            val amountRaw = if (headerIndexAmount in tokens.indices) tokens[headerIndexAmount].trim() else "₱0.00"

            val cleanAmount = when {
                amountRaw.startsWith("₱") || amountRaw.startsWith("PHP") -> amountRaw
                amountRaw.startsWith("$") -> "₱" + amountRaw.removePrefix("$")
                amountRaw.any { it.isDigit() } -> "₱${amountRaw.trim()}"
                else -> "₱0.00"
            }

            val normalizedPhone = normalizePhoneNumber(rawPhone)
            if (isValidPhoneNumber(normalizedPhone)) {
                if (seenPhones.add(normalizedPhone)) {
                    validContacts.add(
                        ParsedContact(
                            phoneNumber = normalizedPhone,
                            name = name,
                            amount = cleanAmount
                        )
                    )
                } else {
                    duplicates++
                }
            } else {
                invalid++
            }
        }

        return CsvParseResult(
            validContacts = validContacts,
            totalRowsParsed = totalRows,
            duplicatesRemoved = duplicates,
            invalidRowsSkipped = invalid,
            previewHeaders = if (hasHeader) firstRowTokens else listOf("Phone", "Name", "Amount"),
            sampleRows = sampleList
        )
    }

    private fun splitCsvRow(row: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()
        var i = 0
        while (i < row.length) {
            val c = row[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes && row.startsWith(delimiter, i)) {
                result.add(sb.toString().trim().removeSurrounding("\""))
                sb.clear()
                i += delimiter.length - 1
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString().trim().removeSurrounding("\""))
        return result
    }

    /**
     * Normalizes Philippine mobile numbers (+63 / 09) and international formats.
     */
    fun normalizePhoneNumber(input: String): String {
        val trimmed = input.trim()
        val digitsOnly = trimmed.filter { it.isDigit() }

        return when {
            // Local Philippine number starting with 09 (e.g. 09171234567 -> 11 digits)
            trimmed.startsWith("09") && digitsOnly.length == 11 -> {
                "09${digitsOnly.substring(2)}"
            }
            // 10 digits starting with 9 (e.g. 9171234567) -> automatically prepend 0
            digitsOnly.length == 10 && digitsOnly.startsWith("9") -> {
                "0$digitsOnly"
            }
            // International Philippine format with +63 (e.g. +639171234567)
            trimmed.startsWith("+63") && digitsOnly.length == 12 -> {
                "+63${digitsOnly.substring(2)}"
            }
            // 639171234567 without plus
            trimmed.startsWith("639") && digitsOnly.length == 12 -> {
                "+63${digitsOnly.substring(2)}"
            }
            trimmed.startsWith("+") -> {
                "+$digitsOnly"
            }
            else -> {
                if (digitsOnly.length == 10 && digitsOnly.startsWith("9")) {
                    "0$digitsOnly"
                } else {
                    digitsOnly
                }
            }
        }
    }

    /**
     * Validates Philippine numbers (11 digits starting with 09, or 12 digits starting with +639)
     * as well as valid standard international numbers (7 to 15 digits).
     */
    fun isValidPhoneNumber(normalized: String): Boolean {
        val digits = normalized.filter { it.isDigit() }
        if (normalized.startsWith("09") && digits.length == 11) return true
        if (normalized.startsWith("+639") && digits.length == 12) return true
        return digits.length in 7..15
    }

    /**
     * Standard CSV template header string.
     */
    fun getCsvHeaderTemplate(): String {
        return "phone,name,amount\n"
    }
}
