package com.example.engine

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class RawImportSheet(
    val fileName: String,
    val headers: List<String>,
    val rows: List<List<String>>,
    val detectedPhoneColumn: Int = -1,
    val detectedNameColumn: Int = -1,
    val detectedAmountColumn: Int = -1,
    val hasHeaderRow: Boolean = true,
    val totalRawRows: Int = 0
)

data class ColumnMappingConfig(
    val phoneColumnIndex: Int,
    val nameColumnIndex: Int = -1, // -1 means unmapped (will use complete phone number)
    val amountColumnIndex: Int = -1, // -1 means unmapped (default ₱0.00)
    val skipFirstRow: Boolean = true,
    val removeDuplicates: Boolean = true,
    val autoNormalizePhilippine: Boolean = true
)

object ContactImportService {

    /**
     * Parses an input stream of either CSV, TSV, or Excel (.xlsx) file.
     */
    fun parseStream(inputStream: InputStream, fileName: String): RawImportSheet {
        val buffer = toByteArray(inputStream)
        val isXlsx = isZipArchive(buffer) || fileName.endsWith(".xlsx", ignoreCase = true)

        if (isXlsx) {
            try {
                val sheet = parseXlsx(ByteArrayInputStream(buffer), fileName)
                if (sheet.rows.isNotEmpty()) {
                    return sheet
                }
            } catch (e: Exception) {
                // If XLSX parsing fails or was plain text with .xls extension, fallback to delimited text
            }
        }

        return parseDelimitedText(ByteArrayInputStream(buffer), fileName)
    }

    /**
     * Parses raw pasted text (CSV, TSV, tab-delimited, comma-separated).
     */
    fun parseRawText(rawText: String, sourceName: String = "Pasted Contact Data"): RawImportSheet {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return RawImportSheet(
                fileName = sourceName,
                headers = emptyList(),
                rows = emptyList(),
                totalRawRows = 0
            )
        }

        val delimiter = detectDelimiter(lines.first())
        val allRows = lines.map { splitDelimitedRow(it, delimiter) }
        return analyzeSheet(allRows, sourceName)
    }

    private fun parseDelimitedText(inputStream: InputStream, fileName: String): RawImportSheet {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return RawImportSheet(
                fileName = fileName,
                headers = emptyList(),
                rows = emptyList(),
                totalRawRows = 0
            )
        }

        val delimiter = detectDelimiter(lines.first())
        val allRows = lines.map { splitDelimitedRow(it, delimiter) }
        return analyzeSheet(allRows, fileName)
    }

    /**
     * Fast, zero-dependency Excel (.xlsx) reader.
     * Extracts shared strings from xl/sharedStrings.xml and sheet cells from xl/worksheets/sheet1.xml.
     */
    fun parseXlsx(inputStream: InputStream, fileName: String): RawImportSheet {
        val buffer = toByteArray(inputStream)
        val sharedStrings = extractSharedStrings(ByteArrayInputStream(buffer))
        val rows = extractSheetRows(ByteArrayInputStream(buffer), sharedStrings)
        return analyzeSheet(rows, fileName)
    }

    private fun extractSharedStrings(inputStream: InputStream): List<String> {
        val list = mutableListOf<String>()
        val zip = ZipInputStream(inputStream)
        var entry: ZipEntry? = zip.nextEntry

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false

        while (entry != null) {
            if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                val parser = factory.newPullParser()
                parser.setInput(zip, "UTF-8")

                var eventType = parser.eventType
                var inSi = false
                var inT = false
                val currentText = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val name = parser.name
                            if (name.equals("si", ignoreCase = true)) {
                                inSi = true
                                currentText.clear()
                            } else if (inSi && name.equals("t", ignoreCase = true)) {
                                inT = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inT) {
                                currentText.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val name = parser.name
                            if (name.equals("t", ignoreCase = true)) {
                                inT = false
                            } else if (name.equals("si", ignoreCase = true)) {
                                inSi = false
                                list.add(currentText.toString())
                            }
                        }
                    }
                    eventType = parser.next()
                }
                break
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        return list
    }

    private fun extractSheetRows(inputStream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val allRows = mutableListOf<List<String>>()
        val zip = ZipInputStream(inputStream)
        var entry: ZipEntry? = zip.nextEntry

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false

        while (entry != null) {
            // Pick sheet1.xml or the first worksheet found
            if (entry.name.startsWith("xl/worksheets/sheet", ignoreCase = true) && entry.name.endsWith(".xml", ignoreCase = true)) {
                val parser = factory.newPullParser()
                parser.setInput(zip, "UTF-8")

                var eventType = parser.eventType
                var inRow = false
                var inC = false
                var inV = false
                var inIs = false
                var inT = false

                var cellType: String? = null
                var cellRef: String? = null
                val cellValue = StringBuilder()
                val currentRowMap = mutableMapOf<Int, String>()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tag = parser.name
                            if (tag.equals("row", ignoreCase = true)) {
                                inRow = true
                                currentRowMap.clear()
                            } else if (inRow && tag.equals("c", ignoreCase = true)) {
                                inC = true
                                cellType = parser.getAttributeValue(null, "t")
                                cellRef = parser.getAttributeValue(null, "r")
                                cellValue.clear()
                            } else if (inC && tag.equals("v", ignoreCase = true)) {
                                inV = true
                            } else if (inC && tag.equals("is", ignoreCase = true)) {
                                inIs = true
                            } else if ((inV || inIs) && tag.equals("t", ignoreCase = true)) {
                                inT = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inV || inT) {
                                cellValue.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val tag = parser.name
                            if (tag.equals("v", ignoreCase = true)) {
                                inV = false
                            } else if (tag.equals("t", ignoreCase = true)) {
                                inT = false
                            } else if (tag.equals("is", ignoreCase = true)) {
                                inIs = false
                            } else if (tag.equals("c", ignoreCase = true)) {
                                inC = false
                                val colIndex = if (cellRef != null) columnLetterToIndex(cellRef!!) else currentRowMap.size
                                val rawText = cellValue.toString().trim()
                                val resolvedValue = when (cellType) {
                                    "s" -> {
                                        val idx = rawText.toIntOrNull()
                                        if (idx != null && idx in sharedStrings.indices) {
                                            sharedStrings[idx]
                                        } else {
                                            rawText
                                        }
                                    }
                                    "b" -> if (rawText == "1") "TRUE" else "FALSE"
                                    else -> rawText
                                }
                                currentRowMap[colIndex] = resolvedValue
                            } else if (tag.equals("row", ignoreCase = true)) {
                                inRow = false
                                if (currentRowMap.isNotEmpty()) {
                                    val maxCol = currentRowMap.keys.maxOrNull() ?: 0
                                    val rowList = mutableListOf<String>()
                                    for (c in 0..maxCol) {
                                        rowList.add(currentRowMap[c] ?: "")
                                    }
                                    allRows.add(rowList)
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                break
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        return allRows
    }

    private fun columnLetterToIndex(cellRef: String): Int {
        val colLetters = cellRef.takeWhile { it.isLetter() }.uppercase()
        if (colLetters.isEmpty()) return 0
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun analyzeSheet(rows: List<List<String>>, fileName: String): RawImportSheet {
        if (rows.isEmpty()) {
            return RawImportSheet(fileName, emptyList(), emptyList(), -1, -1, -1, false, 0)
        }

        val firstRow = rows.first()
        val hasHeader = firstRow.any { token ->
            val lower = token.lowercase().trim()
            lower.contains("phone") || lower.contains("mobile") || lower.contains("number") ||
                    lower.contains("tel") || lower.contains("contact") || lower.contains("name") ||
                    lower.contains("amount") || lower.contains("price") || lower.contains("balance") ||
                    lower.contains("bayad") || lower.contains("singil") || lower.contains("pangalan") ||
                    lower.contains("recipient") || lower.contains("client") || lower.contains("due")
        }

        var detectedPhone = -1
        var detectedName = -1
        var detectedAmount = -1

        if (hasHeader) {
            firstRow.forEachIndexed { index, header ->
                val lower = header.lowercase().trim()
                if (detectedPhone == -1 && (lower.contains("phone") || lower.contains("mobile") || lower.contains("tel") || lower.contains("cel") || lower.contains("number") || lower.contains("contact"))) {
                    detectedPhone = index
                } else if (detectedName == -1 && (lower.contains("name") || lower.contains("recipient") || lower.contains("pangalan") || lower.contains("client") || lower.contains("customer"))) {
                    detectedName = index
                } else if (detectedAmount == -1 && (lower.contains("amount") || lower.contains("balance") || lower.contains("due") || lower.contains("price") || lower.contains("fee") || lower.contains("bayad") || lower.contains("singil") || lower.contains("total"))) {
                    detectedAmount = index
                }
            }
        }

        // Content-based heuristic if phone wasn't detected by header
        if (detectedPhone == -1) {
            val sampleRows = rows.drop(if (hasHeader) 1 else 0).take(10)
            if (sampleRows.isNotEmpty()) {
                val maxCols = sampleRows.maxOfOrNull { it.size } ?: 0
                for (col in 0 until maxCols) {
                    val phoneMatches = sampleRows.count { r ->
                        if (col in r.indices) {
                            val digits = r[col].filter { it.isDigit() }
                            (digits.startsWith("09") && digits.length == 11) ||
                                    (digits.startsWith("639") && digits.length == 12) ||
                                    (digits.length in 10..12)
                        } else false
                    }
                    if (phoneMatches >= (sampleRows.size / 2).coerceAtLeast(1)) {
                        detectedPhone = col
                        break
                    }
                }
            }
        }

        if (detectedPhone == -1) {
            detectedPhone = 0
        }

        if (detectedName == -1 && firstRow.size > 1 && detectedPhone != 1) {
            detectedName = if (detectedPhone == 0) 1 else 0
        }

        val generatedHeaders = if (hasHeader) {
            firstRow.mapIndexed { idx, title ->
                val trimmed = title.trim()
                if (trimmed.isNotBlank()) trimmed else "Column ${columnNameForIndex(idx)}"
            }
        } else {
            val maxCol = rows.maxOfOrNull { it.size } ?: 1
            (0 until maxCol).map { "Column ${columnNameForIndex(it)}" }
        }

        return RawImportSheet(
            fileName = fileName,
            headers = generatedHeaders,
            rows = rows,
            detectedPhoneColumn = detectedPhone,
            detectedNameColumn = detectedName,
            detectedAmountColumn = detectedAmount,
            hasHeaderRow = hasHeader,
            totalRawRows = rows.size
        )
    }

    fun columnNameForIndex(index: Int): String {
        var num = index
        val sb = StringBuilder()
        while (num >= 0) {
            sb.append(('A'.code + (num % 26)).toChar())
            num = (num / 26) - 1
        }
        return sb.reverse().toString()
    }

    /**
     * Maps raw spreadsheet rows according to user's column mapping configuration into a recipient list.
     * Note: If recipient name is not mapped or empty, name is kept blank so that TemplateEngine
     * dynamically resolves {Name} to the complete phone number!
     */
    fun mapToRecipients(raw: RawImportSheet, config: ColumnMappingConfig): CsvParseResult {
        val startIndex = if (config.skipFirstRow && raw.hasHeaderRow) 1 else 0
        val seenPhones = HashSet<String>()
        val validContacts = ArrayList<ParsedContact>()
        var duplicates = 0
        var invalid = 0
        var totalRows = 0
        val sampleList = ArrayList<List<String>>()

        for (i in startIndex until raw.rows.size) {
            val row = raw.rows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue
            totalRows++

            val rawPhone = if (config.phoneColumnIndex in row.indices) row[config.phoneColumnIndex].trim() else ""
            val rawName = if (config.nameColumnIndex in row.indices) row[config.nameColumnIndex].trim() else ""
            val rawAmount = if (config.amountColumnIndex in row.indices) row[config.amountColumnIndex].trim() else ""

            val normalizedPhone = if (config.autoNormalizePhilippine) {
                CsvParser.normalizePhoneNumber(rawPhone)
            } else {
                rawPhone.filter { it.isDigit() || it == '+' }
            }

            // If name is blank or equals "Suki", treat as empty so complete phone number is used
            val cleanName = if (rawName.equals("Suki", ignoreCase = true)) "" else rawName

            val cleanAmount = when {
                rawAmount.startsWith("₱") || rawAmount.startsWith("PHP") -> rawAmount
                rawAmount.startsWith("$") -> "₱" + rawAmount.removePrefix("$")
                rawAmount.any { it.isDigit() } -> "₱${rawAmount.trim()}"
                else -> "₱0.00"
            }

            if (sampleList.size < 5) {
                sampleList.add(
                    listOf(
                        normalizedPhone.ifBlank { rawPhone },
                        cleanName.ifBlank { "(No Name - Uses Phone)" },
                        cleanAmount
                    )
                )
            }

            if (CsvParser.isValidPhoneNumber(normalizedPhone)) {
                if (config.removeDuplicates) {
                    if (seenPhones.add(normalizedPhone)) {
                        validContacts.add(
                            ParsedContact(
                                phoneNumber = normalizedPhone,
                                name = cleanName,
                                amount = cleanAmount
                            )
                        )
                    } else {
                        duplicates++
                    }
                } else {
                    validContacts.add(
                        ParsedContact(
                            phoneNumber = normalizedPhone,
                            name = cleanName,
                            amount = cleanAmount
                        )
                    )
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
            previewHeaders = listOf("Phone Number", "Recipient Name", "Amount"),
            sampleRows = sampleList
        )
    }

    private fun detectDelimiter(line: String): String {
        return when {
            line.contains('\t') -> "\t"
            line.contains(';') -> ";"
            line.contains('|') -> "|"
            else -> ","
        }
    }

    private fun splitDelimitedRow(row: String, delimiter: String): List<String> {
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

    private fun isZipArchive(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        // Magic header 0x50 0x4B 0x03 0x04 ('PK\003\004')
        return bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() && bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()
    }

    private fun toByteArray(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(8192)
        var nRead: Int
        while (inputStream.read(data, 0, data.size).also { nRead = it } != -1) {
            buffer.write(data, 0, nRead)
        }
        return buffer.toByteArray()
    }
}
