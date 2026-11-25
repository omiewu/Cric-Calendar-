package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SportsCricket
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.NotificationHelper
import com.example.ui.AppTab
import com.example.ui.CricPulseViewModel
import com.example.ui.components.MatchDetailSheet
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.FixturesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CricPulseTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CricPulseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleDeepLinkIntent(intent)

        setContent {
            CricPulseApp(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val matchId = intent?.getStringExtra(NotificationHelper.EXTRA_MATCH_ID)
        if (!matchId.isNullOrBlank()) {
            viewModel.openMatchDetail(matchId)
        }
    }
}

@Composable
private fun CricPulseApp(viewModel: CricPulseViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by rememberSaveable { mutableStateOf(AppTab.FIXTURES) }

    // Android 13+ POST_NOTIFICATIONS runtime permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.refreshPermissions()
        }
    }

    // Android 12+ SCHEDULE_EXACT_ALARM settings intent
    val openExactAlarmSettings: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val exactIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            runCatching { context.startActivity(exactIntent) }
        }
    }

    // Refresh permissions whenever returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.requestNotificationPermissionEvent.collect {
            requestNotificationPermission()
        }
    }

    // Handle back button on secondary tabs to return to Fixtures
    BackHandler(enabled = currentTab != AppTab.FIXTURES) {
        currentTab = AppTab.FIXTURES
    }

    CricPulseTheme(
        themeMode = uiState.preferences.themeMode,
        dynamicColor = uiState.preferences.dynamicColorEnabled
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp
            val activeAlertsCount = uiState.alertedMatches.size

            val navItems = listOf(
                NavDestination(
                    tab = AppTab.FIXTURES,
                    label = stringResource(R.string.nav_fixtures),
                    selectedIcon = Icons.Rounded.SportsCricket,
                    unselectedIcon = Icons.Outlined.SportsCricket,
                    badgeCount = 0
                ),
                NavDestination(
                    tab = AppTab.ALERTS,
                    label = stringResource(R.string.nav_alerts),
                    selectedIcon = Icons.Rounded.NotificationsActive,
                    unselectedIcon = Icons.Outlined.Notifications,
                    badgeCount = activeAlertsCount
                ),
                NavDestination(
                    tab = AppTab.SETTINGS,
                    label = stringResource(R.string.nav_settings),
                    selectedIcon = Icons.Rounded.Tune,
                    unselectedIcon = Icons.Outlined.Tune,
                    badgeCount = 0
                )
            )

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                val selected = currentTab == item.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = item.tab },
                                    icon = {
                                        NavBadgeIcon(item = item, selected = selected)
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("nav_${item.tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { item ->
                                val selected = currentTab == item.tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = item.tab },
                                    icon = {
                                        NavBadgeIcon(item = item, selected = selected)
                                    },
                                    label = { Text(item.label) },
                                    modifier = Modifier.testTag("nav_rail_${item.tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentTab) {
                            AppTab.FIXTURES -> FixturesScreen(
                                uiState = uiState,
                                onSelectFormat = viewModel::selectFormat,
                                onSelectStatus = viewModel::selectStatus,
                                onSelectTeam = viewModel::selectTeamFilter,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onClearFilters = viewModel::clearFilters,
                                onSyncClick = { viewModel.syncFixtures(forceRemote = true, showToast = true) },
                                onMatchClick = { viewModel.openMatchDetail(it.id) },
                                onToggleNotification = viewModel::toggleMatchReminder,
                                onRequestNotificationPermission = requestNotificationPermission
                            )

                            AppTab.ALERTS -> AlertsScreen(
                                uiState = uiState,
                                onMatchClick = { viewModel.openMatchDetail(it.id) },
                                onDisableAlert = viewModel::toggleMatchReminder,
                                onGoToFixtures = { currentTab = AppTab.FIXTURES },
                                onGoToSettings = { currentTab = AppTab.SETTINGS },
                                onRequestNotificationPermission = requestNotificationPermission,
                                onOpenExactAlarmSettings = openExactAlarmSettings
                            )

                            AppTab.SETTINGS -> SettingsScreen(
                                uiState = uiState,
                                onSendTestNotification = viewModel::sendTestNotification,
                                onUpdateGlobalInterval = viewModel::updateGlobalInterval,
                                onToggleFavoriteTeam = viewModel::toggleFavoriteTeam,
                                onSetAutoNotifyFavorites = viewModel::setAutoNotifyFavorites,
                                onSetThemeMode = viewModel::setThemeMode,
                                onSetDynamicColor = viewModel::setDynamicColorEnabled,
                                onSaveApiConfig = viewModel::saveApiConfig,
                                onSyncFixtures = { viewModel.syncFixtures(forceRemote = true, showToast = true) },
                                onRequestNotificationPermission = requestNotificationPermission,
                                onOpenExactAlarmSettings = openExactAlarmSettings
                            )
                        }
                    }
                }
            }

            // Match Detail Bottom Sheet
            uiState.selectedMatch?.let { selectedMatch ->
                MatchDetailSheet(
                    match = selectedMatch,
                    preferences = uiState.preferences,
                    permissions = uiState.permissions,
                    nowEpochMs = uiState.nowEpochMs,
                    onDismiss = viewModel::closeMatchDetail,
                    onToggleMasterReminder = { viewModel.toggleMatchReminder(selectedMatch) },
                    onToggleStage = { stage, enabled ->
                        viewModel.updateMatchReminderStage(selectedMatch.id, stage, enabled)
                    },
                    onRequestNotificationPermission = requestNotificationPermission,
                    onOpenExactAlarmSettings = openExactAlarmSettings
                )
            }
        }
    }
}

private data class NavDestination(
    val tab: AppTab,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeCount: Int
)

@Composable
private fun NavBadgeIcon(
    item: NavDestination,
    selected: Boolean
) {
    val icon = if (selected) item.selectedIcon else item.unselectedIcon
    if (item.badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(text = item.badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = item.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = item.label)
    }
}
