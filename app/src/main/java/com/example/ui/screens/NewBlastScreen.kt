package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.engine.PhilippineCarrier
import com.example.engine.PhilippineCarrierClassifier
import com.example.engine.SimRoutingMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CsvParser
import com.example.engine.TemplateEngine
import com.example.ui.viewmodel.CampaignViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBlastScreen(
    viewModel: CampaignViewModel,
    onBlastLaunched: (Long) -> Unit,
    onRequestDefaultSms: () -> Unit = {}
) {
    val context = LocalContext.current
    val form by viewModel.formState.collectAsState()
    val templates by viewModel.allTemplates.collectAsState()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsState()
    val availableSims by viewModel.availableSimCards.collectAsState()
    val pendingImportSheet by viewModel.pendingImportSheet.collectAsState()

    var showPasteDialog by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var showPreviewList by remember { mutableStateOf(false) }
    var showTemplatePickerDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var saveTemplateTitle by remember { mutableStateOf("") }
    var saveTemplateCategory by remember { mutableStateOf("Billing") }

    // Quick Add Single Philippine Recipient State
    var showQuickAdd by remember { mutableStateOf(false) }
    var quickPhone by remember { mutableStateOf("") }
    var quickPrefix by remember { mutableStateOf("+63") }
    var quickName by remember { mutableStateOf("") }
    var quickAmount by remember { mutableStateOf("₱1,250.00") }
    var quickAddError by remember { mutableStateOf<String?>(null) }

    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.loadFileForImport(uri)
        }
    }

    val lengthInfo = TemplateEngine.calculateSmsLength(form.messageBody)
    val totalContacts = form.contacts.size

    val dateTimeFormatter = remember { SimpleDateFormat("EEE, MMM d, yyyy @ hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step 1: Recipients Header & Import Tools
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "1. Target Audience",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Upload or import up to 1,000 numbers with Name & Amount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$totalContacts Numbers",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Import Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { csvFileLauncher.launch("*/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("import_csv_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CSV / Excel", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showPasteDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("paste_numbers_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste (+63/09)", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showQuickAdd = !showQuickAdd },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("quick_add_contact_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add PH", fontSize = 11.sp)
                        }
                    }

                    // Quick Add Philippine Recipient Card
                    AnimatedVisibility(visible = showQuickAdd) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Add Philippine Recipient (+63 / 09)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Prefix Selector
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("+63", "09").forEach { prefix ->
                                            val isSelected = quickPrefix == prefix
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { quickPrefix = prefix }
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = prefix,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = quickPhone,
                                        onValueChange = { quickPhone = it },
                                        placeholder = { Text(if (quickPrefix == "+63") "9171234567" else "171234567") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = quickName,
                                        onValueChange = { quickName = it },
                                        placeholder = { Text("Name (e.g. Maria Santos)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )

                                    OutlinedTextField(
                                        value = quickAmount,
                                        onValueChange = { quickAmount = it },
                                        placeholder = { Text("Amount (₱)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                quickAddError?.let { err ->
                                    Text(err, color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        val rawInput = quickPhone.trim()
                                        if (rawInput.isBlank()) {
                                            quickAddError = "Please enter mobile number"
                                            return@Button
                                        }
                                        val combined = if (rawInput.startsWith("+") || rawInput.startsWith("09")) {
                                            rawInput
                                        } else {
                                            "$quickPrefix$rawInput"
                                        }
                                        val normalized = CsvParser.normalizePhoneNumber(combined)
                                        if (!CsvParser.isValidPhoneNumber(normalized)) {
                                            quickAddError = "Invalid Philippine mobile number ($normalized)"
                                            return@Button
                                        }
                                        val amt = if (quickAmount.startsWith("₱") || quickAmount.startsWith("PHP")) quickAmount.trim() else "₱${quickAmount.trim()}"
                                        val newContact = com.example.engine.ParsedContact(
                                            phoneNumber = normalized,
                                            name = quickName.trim(), // Empty name uses complete phone number in messages
                                            amount = amt
                                        )
                                        viewModel.updateForm { f ->
                                            f.copy(contacts = listOf(newContact) + f.contacts)
                                        }
                                        quickPhone = ""
                                        quickName = ""
                                        quickAddError = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Recipient")
                                }
                            }
                        }
                    }

                    if (form.contacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${form.contacts.size} Recipients Loaded",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    viewModel.updateForm { f -> f.copy(contacts = emptyList(), parseSummary = null) }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Clear List", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Parse Summary Strip
                    form.parseSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Parsed: ${summary.validContacts.size} valid contacts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = { showPreviewList = !showPreviewList },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text(if (showPreviewList) "Hide List" else "View Contacts", fontSize = 11.sp)
                                    }
                                }

                                if (summary.duplicatesRemoved > 0 || summary.invalidRowsSkipped > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Duplicates purged: ${summary.duplicatesRemoved} • Invalid skipped: ${summary.invalidRowsSkipped}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    // Expandable Contact Preview List
                    AnimatedVisibility(visible = showPreviewList && form.contacts.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Contact Preview (First 4 rows):", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                form.contacts.take(4).forEach { contact ->
                                    val carrier = PhilippineCarrierClassifier.detectCarrier(contact.phoneNumber)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(contact.phoneNumber, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = carrier.composeColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = carrier.shortName,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = carrier.composeColor,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(contact.name.ifEmpty { "Customer" }, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(contact.amount, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (form.contacts.size > 4) {
                                    Text(
                                        text = "... and ${form.contacts.size - 4} more recipients",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 2: Campaign Details & Message Composer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Message & Dynamic Content",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showTemplatePickerDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("choose_template_button")
                            ) {
                                Text("Templates", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    saveTemplateTitle = form.title.ifBlank { "Custom Template" }
                                    showSaveTemplateDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("save_as_template_button")
                            ) {
                                Text("Save as Template", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Campaign Title input (Optional - defaults to date and time sent)
                    val defaultCampaignTitlePreview = remember(form.scheduledTimeEpoch) {
                        viewModel.generateDefaultCampaignTitle(form.scheduledTimeEpoch)
                    }
                    OutlinedTextField(
                        value = form.title,
                        onValueChange = { viewModel.updateForm { f -> f.copy(title = it) } },
                        label = { Text("Campaign Name (Optional)") },
                        placeholder = { Text(defaultCampaignTitlePreview) },
                        supportingText = {
                            Text(
                                text = if (form.title.isBlank()) "Default: \"$defaultCampaignTitlePreview\"" else "Custom campaign name",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("campaign_title_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Message Text Area
                    OutlinedTextField(
                        value = form.messageBody,
                        onValueChange = { viewModel.updateForm { f -> f.copy(messageBody = it) } },
                        label = { Text("SMS Body (supports {Name} and {Amount})") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("message_body_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Variables / Merge Tags ({Name} and {Amount} only)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Insert Personalization Tag:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("{Name}", "{Amount}").forEach { tag ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            viewModel.updateForm { f ->
                                                f.copy(messageBody = f.messageBody + " $tag")
                                            }
                                        },
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Character counter & segment breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${lengthInfo.charCount} chars (${lengthInfo.charsRemainingInSegment} left)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${lengthInfo.segmentCount} SMS Segment(s)${if (lengthInfo.isUnicode) " [Unicode]" else " [GSM-7]"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lengthInfo.segmentCount > 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Step 3: Scheduling & Throttle Speed
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Delivery Timing & Scheduling",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val isScheduled = form.scheduledTimeEpoch != null

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = !isScheduled,
                            onClick = { viewModel.updateForm { f -> f.copy(scheduledTimeEpoch = null) } },
                            label = { Text("Send Immediately") },
                            leadingIcon = if (!isScheduled) {
                                { Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = isScheduled,
                            onClick = {
                                val future = System.currentTimeMillis() + 3600000L * 2 // default 2 hours from now
                                viewModel.updateForm { f -> f.copy(scheduledTimeEpoch = future) }
                            },
                            label = { Text("Schedule in Advance") },
                            leadingIcon = if (isScheduled) {
                                { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    if (isScheduled && form.scheduledTimeEpoch != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Scheduled Launch Time:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dateTimeFormatter.format(Date(form.scheduledTimeEpoch!!)),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF78350F)
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            val c = Calendar.getInstance()
                                            c.timeInMillis = form.scheduledTimeEpoch ?: System.currentTimeMillis()
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    c.set(Calendar.YEAR, year)
                                                    c.set(Calendar.MONTH, month)
                                                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    TimePickerDialog(
                                                        context,
                                                        { _, hourOfDay, minute ->
                                                            c.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                            c.set(Calendar.MINUTE, minute)
                                                            viewModel.updateForm { f -> f.copy(scheduledTimeEpoch = c.timeInMillis) }
                                                        },
                                                        c.get(Calendar.HOUR_OF_DAY),
                                                        c.get(Calendar.MINUTE),
                                                        false
                                                    ).show()
                                                },
                                                c.get(Calendar.YEAR),
                                                c.get(Calendar.MONTH),
                                                c.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Change", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Android OS Rate Limiter Bypass Card (>30 msgs / 30s)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (form.bypassAndroidRateLimit) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (form.bypassAndroidRateLimit) Color(0xFFBBF7D0) else Color.LightGray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (form.bypassAndroidRateLimit) Color(0xFF16A34A) else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bypass Android Rate Limiter (>30 msgs/30s)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (form.bypassAndroidRateLimit) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isDefaultSmsApp) {
                                        "✓ Primary SMS App Active: Android OS exempts default app from 30 msgs/30s confirmation barrier."
                                    } else if (form.bypassAndroidRateLimit) {
                                        "✓ Adaptive safe spacing (1.05s) prevents triggering the Android OS 30-message alert dialog."
                                    } else {
                                        "Standard unthrottled dispatch. May trigger Android OS alert."
                                    },
                                    fontSize = 10.sp,
                                    color = if (form.bypassAndroidRateLimit) Color(0xFF166534) else MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = form.bypassAndroidRateLimit,
                                onCheckedChange = { isChecked ->
                                    viewModel.updateForm { f -> f.copy(bypassAndroidRateLimit = isChecked) }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dispatch Speed Control
                    Text(
                        text = "Throttle Speed (Local SIM Radio Pacing):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Safe", 1050L, "1.05s (Safe)"),
                            Triple("Standard", 300L, "300ms/msg"),
                            Triple("Fast", 100L, "100ms/msg"),
                            Triple("Turbo", 50L, "50ms (Max)")
                        ).forEach { (label, delayMs, desc) ->
                            val isSelected = form.throttleSpeed == delayMs
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updateForm { f -> f.copy(throttleSpeed = delayMs) }
                                    }
                                    .padding(vertical = 8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(text = desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Step 4: Dual SIM Cellular Radio & Network Routing
        item {
            val carrierBreakdown = remember(form.contacts) {
                viewModel.getCarrierBreakdown(form.contacts)
            }
            val globeCount = carrierBreakdown[PhilippineCarrier.GLOBE_TM_GOMO] ?: 0
            val smartCount = carrierBreakdown[PhilippineCarrier.SMART_TNT_SUN] ?: 0
            val ditoCount = carrierBreakdown[PhilippineCarrier.DITO] ?: 0
            val otherCount = carrierBreakdown[PhilippineCarrier.UNKNOWN] ?: 0
            val totalContacts = form.contacts.size

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SimCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "4. Dual SIM Network Routing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Dual SIM Toggle Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (form.isDualSimEnabled) "Dual SIM ON" else "Single SIM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (form.isDualSimEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = form.isDualSimEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.updateForm { f -> f.copy(isDualSimEnabled = isEnabled) }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (form.isDualSimEnabled) {
                        // Routing Strategy Selector
                        Text(
                            text = "Dual SIM Routing Strategy:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(SimRoutingMode.SMART_PREFIX, "Prefix Auto-Route", "SIM 1=Globe, SIM 2=Smart"),
                                Triple(SimRoutingMode.ROUND_ROBIN, "Round-Robin", "Alternate 50/50"),
                                Triple(SimRoutingMode.SINGLE_SIM, "Single Fixed", "One SIM only")
                            ).forEach { (mode, title, desc) ->
                                val isSelected = form.dualSimMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.updateForm { f -> f.copy(dualSimMode = mode) }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(text = desc, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (form.dualSimMode == SimRoutingMode.SMART_PREFIX) {
                            // Live Carrier Breakdown Banner
                            if (totalContacts > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Carrier Prefix Auto-Distribution ($totalContacts recipients):",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Horizontal Segmented Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                        ) {
                                            if (globeCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(globeCount.toFloat())
                                                        .fillMaxHeight()
                                                        .background(PhilippineCarrier.GLOBE_TM_GOMO.composeColor)
                                                )
                                            }
                                            if (smartCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(smartCount.toFloat())
                                                        .fillMaxHeight()
                                                        .background(PhilippineCarrier.SMART_TNT_SUN.composeColor)
                                                )
                                            }
                                            if (ditoCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(ditoCount.toFloat())
                                                        .fillMaxHeight()
                                                        .background(PhilippineCarrier.DITO.composeColor)
                                                )
                                            }
                                            if (otherCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(otherCount.toFloat())
                                                        .fillMaxHeight()
                                                        .background(PhilippineCarrier.UNKNOWN.composeColor)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🔵 Globe/TM: $globeCount (${if (totalContacts > 0) (globeCount * 100) / totalContacts else 0}%)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PhilippineCarrier.GLOBE_TM_GOMO.composeColor
                                            )
                                            Text(
                                                text = "🟢 Smart/TNT: $smartCount (${if (totalContacts > 0) (smartCount * 100) / totalContacts else 0}%)",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PhilippineCarrier.SMART_TNT_SUN.composeColor
                                            )
                                            if (ditoCount > 0 || otherCount > 0) {
                                                Text(
                                                    text = "🔴 DITO/Other: ${ditoCount + otherCount}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PhilippineCarrier.DITO.composeColor
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Rule 1: Globe / TM / GOMO
                            val globeSimName = if (form.globeSimSlot == 0) form.sim1Label else form.sim2Label
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PhilippineCarrier.GLOBE_TM_GOMO.composeColor.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PhilippineCarrier.GLOBE_TM_GOMO.composeColor.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CellTower,
                                                contentDescription = null,
                                                tint = PhilippineCarrier.GLOBE_TM_GOMO.composeColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "Globe Telecom / TM / GOMO",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PhilippineCarrier.GLOBE_TM_GOMO.composeColor
                                                )
                                                Text(
                                                    text = "$globeCount contacts matched",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Swap Button
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.updateForm { f ->
                                                    f.copy(globeSimSlot = if (f.globeSimSlot == 0) 1 else 0)
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SIM ${form.globeSimSlot + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Prefixes: 0817, 0905, 0906, 0915, 0916, 0917, 0926, 0927, 0935, 0936, 0945, 0953-0956, 0965-0967, 0975-0979, 0995, 0997 & Postpaid (09175, 09253...)",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        lineHeight = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PhilippineCarrier.GLOBE_TM_GOMO.composeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "✓ Routed to $globeSimName (Slot ${form.globeSimSlot + 1})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PhilippineCarrier.GLOBE_TM_GOMO.composeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Rule 2: Smart / TNT / Sun
                            val smartSimName = if (form.smartSimSlot == 1) form.sim2Label else form.sim1Label
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PhilippineCarrier.SMART_TNT_SUN.composeColor.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PhilippineCarrier.SMART_TNT_SUN.composeColor.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CellTower,
                                                contentDescription = null,
                                                tint = PhilippineCarrier.SMART_TNT_SUN.composeColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "Smart / TNT / Sun Cellular",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PhilippineCarrier.SMART_TNT_SUN.composeColor
                                                )
                                                Text(
                                                    text = "$smartCount contacts matched",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Swap Button
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.updateForm { f ->
                                                    f.copy(smartSimSlot = if (f.smartSimSlot == 1) 0 else 1)
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SIM ${form.smartSimSlot + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Prefixes: 0813, 0907-0914, 0918-0921, 0928-0930, 0938, 0939, 0946-0950, 0961, 0962, 0963, 0968, 0970, 0981, 0989, 0992, 0998, 0999 • Sun: 0922-0925, 0931-0934, 0940-0944, 0973, 0974",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        lineHeight = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PhilippineCarrier.SMART_TNT_SUN.composeColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "✓ Routed to $smartSimName (Slot ${form.smartSimSlot + 1})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PhilippineCarrier.SMART_TNT_SUN.composeColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Rule 3: DITO / Fallback
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DITO Telecommunity (0895-0898, 0991-0994) & Others",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${ditoCount + otherCount} contacts • Auto-routed to SIM 1 as fallback",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDC2626).copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "SIM 1 (Fallback)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        } else if (form.dualSimMode == SimRoutingMode.ROUND_ROBIN) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🔄 Round-Robin Radio Balancing Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Alternates transmissions evenly: 1st contact sends via SIM 1, 2nd via SIM 2, 3rd via SIM 1, etc.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Fixed Single SIM in Dual SIM container
                            Text(
                                text = "Select SIM slot for fixed dispatch:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            availableSims.forEach { sim ->
                                val isSelected = form.selectedSubscriptionId == sim.subscriptionId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            viewModel.updateForm { f ->
                                                f.copy(
                                                    selectedSubscriptionId = sim.subscriptionId,
                                                    selectedSimLabel = "${sim.displayName} (${sim.carrierName})"
                                                )
                                            }
                                        }
                                        .padding(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "${sim.displayName} • ${sim.carrierName} (Slot ${sim.slotIndex + 1})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Single SIM Mode
                        Text(
                            text = "Single SIM Selected. Select slot to transmit all messages:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (availableSims.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Default Device SIM (System Default Carrier)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                availableSims.forEach { sim ->
                                    val isSelected = form.selectedSubscriptionId == sim.subscriptionId
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                viewModel.updateForm { f ->
                                                    f.copy(
                                                        selectedSubscriptionId = sim.subscriptionId,
                                                        selectedSimLabel = "${sim.displayName} (${sim.carrierName})"
                                                    )
                                                }
                                            }
                                            .padding(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${sim.displayName} • ${sim.carrierName}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Slot ${sim.slotIndex + 1}${if (sim.phoneNumber.isNotEmpty()) " • ${sim.phoneNumber}" else ""}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isDefaultSmsApp) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "App is not Primary SMS App yet",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Set as default to enable direct cellular SIM card transmission.",
                                        fontSize = 10.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Button(
                                    onClick = onRequestDefaultSms,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Set Default", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Handset Preview Bubble
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Handset SMS Inbox Preview",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val firstContact = form.contacts.firstOrNull()
                    val samplePreview = TemplateEngine.resolveMessageBody(
                        template = form.messageBody.ifBlank { "Message preview will appear here once composed..." },
                        recipientName = firstContact?.name ?: "",
                        amount = firstContact?.amount ?: "₱0.00",
                        phoneNumber = firstContact?.phoneNumber ?: ""
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (firstContact != null) {
                                        if (firstContact.name.isNotBlank()) "${firstContact.name} (${firstContact.phoneNumber})" else firstContact.phoneNumber
                                    } else {
                                        "Recipient Preview"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = samplePreview,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Now • Delivered via ${form.selectedSimLabel.substringBefore(" ")}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message if any
        form.errorMessage?.let { error ->
            item {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = error, color = Color(0xFF991B1B), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Launch Action Button
        item {
            Button(
                onClick = {
                    viewModel.launchCampaign { id ->
                        onBlastLaunched(id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("launch_blast_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !form.isSubmitting
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Queuing Transmissions...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = if (form.scheduledTimeEpoch != null) Icons.Default.Schedule else Icons.Default.Send,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (form.scheduledTimeEpoch != null) {
                            "Schedule Blast ($totalContacts Targets)"
                        } else {
                            "Send $totalContacts SMS via ${form.selectedSimLabel.substringBefore("(").trim()}"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Paste Numbers Modal Dialog
    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste Mobile Numbers", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Paste phone numbers separated by commas or newlines. Optionally include name & amount: 'phone,name,amount'",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("paste_text_area"),
                        placeholder = { Text("Phone Number, Name (optional), Amount (optional)\n\ne.g.\n09170000000, Juan Dela Cruz, ₱1,500.00\n09180000000,, ₱750.00") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.loadTextForImport(pasteText)
                        showPasteDialog = false
                    },
                    modifier = Modifier.testTag("confirm_paste_button")
                ) {
                    Text("Map Columns & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Contact Import & Column Mapping Utility Dialog
    pendingImportSheet?.let { sheet ->
        com.example.ui.components.ContactImportUtilityDialog(
            sheet = sheet,
            onDismiss = { viewModel.dismissImportSheet() },
            onConfirmImport = { config ->
                viewModel.applyColumnMapping(config)
            }
        )
    }

    // Template Picker Dialog
    if (showTemplatePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTemplatePickerDialog = false },
            title = { Text("Choose from Template Library", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Select a reusable template to populate your message body:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { template ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        viewModel.applyTemplate(template)
                                        showTemplatePickerDialog = false
                                    }
                                    .padding(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = template.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = template.category,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = template.body,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTemplatePickerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Save As Template Dialog
    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Save Current Message as Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Save this message to your reusable template library for future campaigns:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    OutlinedTextField(
                        value = saveTemplateTitle,
                        onValueChange = { saveTemplateTitle = it },
                        label = { Text("Template Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = saveTemplateCategory,
                        onValueChange = { saveTemplateCategory = it },
                        label = { Text("Category (Billing, Reminders, Notices, etc.)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = form.messageBody,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (saveTemplateTitle.isNotBlank()) {
                            viewModel.saveCurrentFormAsTemplate(saveTemplateTitle, saveTemplateCategory)
                            showSaveTemplateDialog = false
                        }
                    }
                ) {
                    Text("Save to Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
