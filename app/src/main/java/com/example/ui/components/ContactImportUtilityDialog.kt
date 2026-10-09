package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.ColumnMappingConfig
import com.example.engine.ContactImportService
import com.example.engine.RawImportSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactImportUtilityDialog(
    sheet: RawImportSheet,
    onDismiss: () -> Unit,
    onConfirmImport: (ColumnMappingConfig) -> Unit
) {
    var selectedPhoneCol by remember { mutableIntStateOf(sheet.detectedPhoneColumn.coerceAtLeast(0)) }
    var selectedNameCol by remember { mutableIntStateOf(sheet.detectedNameColumn) }
    var selectedAmountCol by remember { mutableIntStateOf(sheet.detectedAmountColumn) }

    var skipHeader by remember { mutableStateOf(sheet.hasHeaderRow) }
    var removeDuplicates by remember { mutableStateOf(true) }
    var autoNormalize by remember { mutableStateOf(true) }

    var phoneExpanded by remember { mutableStateOf(false) }
    var nameExpanded by remember { mutableStateOf(false) }
    var amountExpanded by remember { mutableStateOf(false) }

    val currentConfig by remember {
        derivedStateOf {
            ColumnMappingConfig(
                phoneColumnIndex = selectedPhoneCol,
                nameColumnIndex = selectedNameCol,
                amountColumnIndex = selectedAmountCol,
                skipFirstRow = skipHeader,
                removeDuplicates = removeDuplicates,
                autoNormalizePhilippine = autoNormalize
            )
        }
    }

    val previewResult by remember {
        derivedStateOf {
            ContactImportService.mapToRecipients(sheet, currentConfig)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp)
                .testTag("contact_import_mapping_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Contact List Importer",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Map CSV/Excel columns to recipient phone numbers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // File Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = sheet.fileName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${sheet.totalRawRows} total lines • ${sheet.headers.size} columns detected",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (sheet.fileName.endsWith(".xlsx", ignoreCase = true) || sheet.fileName.endsWith(".xls", ignoreCase = true)) {
                                Color(0xFF107C41).copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            }
                        ) {
                            Text(
                                text = if (sheet.fileName.endsWith(".xlsx", ignoreCase = true) || sheet.fileName.endsWith(".xls", ignoreCase = true)) "Excel" else "CSV",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sheet.fileName.endsWith(".xlsx", ignoreCase = true) || sheet.fileName.endsWith(".xls", ignoreCase = true)) Color(0xFF107C41) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Column Mapping",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Assign each column from your sheet to the corresponding recipient field:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Phone Number Dropdown (Required)
                ExposedDropdownMenuBox(
                    expanded = phoneExpanded,
                    onExpandedChange = { phoneExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedPhoneCol in sheet.headers.indices) {
                            "${ContactImportService.columnNameForIndex(selectedPhoneCol)}: ${sheet.headers[selectedPhoneCol]}"
                        } else "Select Phone Column",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Phone Number Column * (Required)") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = phoneExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("mapping_phone_column_dropdown"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = phoneExpanded,
                        onDismissRequest = { phoneExpanded = false }
                    ) {
                        sheet.headers.forEachIndexed { index, header ->
                            DropdownMenuItem(
                                text = {
                                    Text("${ContactImportService.columnNameForIndex(index)}: $header")
                                },
                                onClick = {
                                    selectedPhoneCol = index
                                    phoneExpanded = false
                                },
                                trailingIcon = {
                                    if (selectedPhoneCol == index) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Name Dropdown (Optional)
                ExposedDropdownMenuBox(
                    expanded = nameExpanded,
                    onExpandedChange = { nameExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedNameCol in sheet.headers.indices) {
                            "${ContactImportService.columnNameForIndex(selectedNameCol)}: ${sheet.headers[selectedNameCol]}"
                        } else "None (Auto-fallback to Complete Phone Number)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Recipient Name Column (Optional)") },
                        supportingText = {
                            Text("If unmapped or blank, {Name} automatically uses the complete phone number.")
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("mapping_name_column_dropdown"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = nameExpanded,
                        onDismissRequest = { nameExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Use Complete Phone Number)") },
                            onClick = {
                                selectedNameCol = -1
                                nameExpanded = false
                            },
                            trailingIcon = {
                                if (selectedNameCol == -1) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        sheet.headers.forEachIndexed { index, header ->
                            DropdownMenuItem(
                                text = {
                                    Text("${ContactImportService.columnNameForIndex(index)}: $header")
                                },
                                onClick = {
                                    selectedNameCol = index
                                    nameExpanded = false
                                },
                                trailingIcon = {
                                    if (selectedNameCol == index) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Amount Dropdown (Optional)
                ExposedDropdownMenuBox(
                    expanded = amountExpanded,
                    onExpandedChange = { amountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (selectedAmountCol in sheet.headers.indices) {
                            "${ContactImportService.columnNameForIndex(selectedAmountCol)}: ${sheet.headers[selectedAmountCol]}"
                        } else "None (Default ₱0.00)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Amount / Balance Column (Optional)") },
                        supportingText = {
                            Text("Mapped into {Amount} personalization tags.")
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = amountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .testTag("mapping_amount_column_dropdown"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = amountExpanded,
                        onDismissRequest = { amountExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Default ₱0.00)") },
                            onClick = {
                                selectedAmountCol = -1
                                amountExpanded = false
                            },
                            trailingIcon = {
                                if (selectedAmountCol == -1) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                        sheet.headers.forEachIndexed { index, header ->
                            DropdownMenuItem(
                                text = {
                                    Text("${ContactImportService.columnNameForIndex(index)}: $header")
                                },
                                onClick = {
                                    selectedAmountCol = index
                                    amountExpanded = false
                                },
                                trailingIcon = {
                                    if (selectedAmountCol == index) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Parser Settings Toggles
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("First row contains headers", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Skip first row during contact import", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = skipHeader,
                                onCheckedChange = { skipHeader = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Remove duplicate phone numbers", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Only keep unique recipients", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = removeDuplicates,
                                onCheckedChange = { removeDuplicates = it }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-format Philippine mobile numbers", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Normalizes 09xx / +639xx mobile prefixes", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Switch(
                                checked = autoNormalize,
                                onCheckedChange = { autoNormalize = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview Table
                Text(
                    text = "Live Mapping Preview (First 5 Rows)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("Phone", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                            Text("Name / Fallback", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.4f))
                            Text("Amount", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val previewItems = previewResult.validContacts.take(5)
                        if (previewItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("No valid phone numbers found with current mapping", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            previewItems.forEachIndexed { index, contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = contact.phoneNumber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    Text(
                                        text = if (contact.name.isNotBlank()) contact.name else "${contact.phoneNumber} (Phone)",
                                        fontSize = 11.sp,
                                        fontStyle = if (contact.name.isBlank()) FontStyle.Italic else FontStyle.Normal,
                                        color = if (contact.name.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    Text(
                                        text = contact.amount,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Stats Pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (previewResult.validContacts.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (previewResult.validContacts.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (previewResult.validContacts.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${previewResult.validContacts.size} valid recipients ready",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (previewResult.duplicatesRemoved > 0 || previewResult.invalidRowsSkipped > 0) {
                            Text(
                                text = "(${previewResult.duplicatesRemoved} dupes, ${previewResult.invalidRowsSkipped} invalid)",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onConfirmImport(currentConfig) },
                        enabled = previewResult.validContacts.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_import_recipients_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import (${previewResult.validContacts.size})")
                    }
                }
            }
        }
    }
}
