package com.example

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CompactBulkProgressBar
import com.example.ui.screens.CampaignDetailScreen
import com.example.ui.screens.CampaignsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.NewBlastScreen
import com.example.ui.screens.TemplatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CampaignViewModel

enum class MainNavigationTab {
    DASHBOARD,
    INBOX,
    NEW_BLAST,
    CAMPAIGNS,
    TEMPLATES
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: CampaignViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(MainNavigationTab.DASHBOARD) }
    var inDetailView by remember { mutableStateOf(false) }

    val dispatchProgress by viewModel.dispatchProgress.collectAsState()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsState()
    val availableSims by viewModel.availableSimCards.collectAsState()
    val unreadInboxCount by viewModel.unreadInboxCount.collectAsState()

    // Multiple permissions launcher for default SMS app & SIM radio
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshSimCards()
        viewModel.loadInboxMessages()
    }

    // Role launcher for Default SMS App
    val defaultSmsRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        viewModel.refreshDefaultSmsAppStatus()
        viewModel.refreshSimCards()
        viewModel.loadInboxMessages()
    }

    fun promptSetAsDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                defaultSmsRoleLauncher.launch(intent)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
            }
            defaultSmsRoleLauncher.launch(intent)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionsLauncher.launch(missing.toTypedArray())
        }
        viewModel.refreshDefaultSmsAppStatus()
        viewModel.refreshSimCards()
        viewModel.loadInboxMessages()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDefaultSmsAppStatus()
                viewModel.refreshSimCards()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SMSBlast Pro",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Default SMS App indicator chip
                            if (isDefaultSmsApp) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFD1FAE5)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Primary SMS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                    }
                                }
                            }

                            if (dispatchProgress.isRunning) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(shape = CircleShape, color = Color(0xFF10B981), modifier = Modifier.size(6.dp)) {}
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${dispatchProgress.processedCount}/${dispatchProgress.totalCount}",
                                            color = Color(0xFF065F46),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // If not default SMS app, show top banner with quick action
                if (!isDefaultSmsApp) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { promptSetAsDefaultSmsApp() },
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SimCard, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tap to set as Primary SMS App to send via Local SIM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFD97706)
                            ) {
                                Text(
                                    text = "Set Default",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Global Visual Progress Bar for ongoing bulk SMS operations
                AnimatedVisibility(visible = dispatchProgress.isRunning) {
                    CompactBulkProgressBar(
                        progress = dispatchProgress,
                        onPause = { viewModel.pauseActiveDispatch() },
                        onResume = { viewModel.resumeActiveDispatch() },
                        onCancel = { viewModel.cancelActiveDispatch() },
                        onClick = {
                            currentTab = MainNavigationTab.DASHBOARD
                            inDetailView = false
                        }
                    )
                }
            }
        },
        bottomBar = {
            if (!inDetailView) {
                NavigationBar(
                    modifier = Modifier.testTag("main_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.DASHBOARD,
                        onClick = { currentTab = MainNavigationTab.DASHBOARD },
                        icon = {
                            Icon(
                                if (currentTab == MainNavigationTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Dashboard", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.INBOX,
                        onClick = { currentTab = MainNavigationTab.INBOX },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (unreadInboxCount > 0) {
                                        Badge {
                                            Text("$unreadInboxCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    if (currentTab == MainNavigationTab.INBOX) Icons.Filled.Inbox else Icons.Outlined.Inbox,
                                    contentDescription = "Inbox"
                                )
                            }
                        },
                        label = { Text("Inbox", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_inbox")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.NEW_BLAST,
                        onClick = { currentTab = MainNavigationTab.NEW_BLAST },
                        icon = {
                            Icon(
                                if (currentTab == MainNavigationTab.NEW_BLAST) Icons.Filled.AddCircle else Icons.Outlined.AddCircleOutline,
                                contentDescription = "New Blast"
                            )
                        },
                        label = { Text("New Blast", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_new_blast")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.CAMPAIGNS,
                        onClick = { currentTab = MainNavigationTab.CAMPAIGNS },
                        icon = {
                            Icon(
                                if (currentTab == MainNavigationTab.CAMPAIGNS) Icons.Filled.Analytics else Icons.Outlined.Analytics,
                                contentDescription = "Campaigns"
                            )
                        },
                        label = { Text("Campaigns", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_campaigns")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainNavigationTab.TEMPLATES,
                        onClick = { currentTab = MainNavigationTab.TEMPLATES },
                        icon = {
                            Icon(
                                if (currentTab == MainNavigationTab.TEMPLATES) Icons.Filled.FormatQuote else Icons.Outlined.FormatQuote,
                                contentDescription = "Templates"
                            )
                        },
                        label = { Text("Templates", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.testTag("nav_templates")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (inDetailView) {
                CampaignDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { inDetailView = false }
                )
            } else {
                when (currentTab) {
                    MainNavigationTab.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToNewBlast = { currentTab = MainNavigationTab.NEW_BLAST },
                        onNavigateToCampaignDetail = { id ->
                            viewModel.selectCampaign(id)
                            inDetailView = true
                        },
                        onRequestDefaultSms = { promptSetAsDefaultSmsApp() }
                    )
                    MainNavigationTab.INBOX -> InboxScreen(
                        viewModel = viewModel
                    )
                    MainNavigationTab.NEW_BLAST -> NewBlastScreen(
                        viewModel = viewModel,
                        onBlastLaunched = { id ->
                            viewModel.selectCampaign(id)
                            inDetailView = true
                        },
                        onRequestDefaultSms = { promptSetAsDefaultSmsApp() }
                    )
                    MainNavigationTab.CAMPAIGNS -> CampaignsScreen(
                        viewModel = viewModel,
                        onNavigateToDetail = { id ->
                            viewModel.selectCampaign(id)
                            inDetailView = true
                        }
                    )
                    MainNavigationTab.TEMPLATES -> TemplatesScreen(
                        viewModel = viewModel,
                        onUseTemplate = { currentTab = MainNavigationTab.NEW_BLAST }
                    )
                }
            }
        }
    }
}
