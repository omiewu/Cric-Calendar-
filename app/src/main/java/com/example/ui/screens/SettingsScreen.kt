package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ReminderStage
import com.example.model.TeamCatalog
import com.example.model.UserPreferences
import com.example.ui.CricPulseUiState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: CricPulseUiState,
    onSendTestNotification: () -> Unit,
    onUpdateGlobalInterval: (ReminderStage, Boolean) -> Unit,
    onToggleFavoriteTeam: (String) -> Unit,
    onSetAutoNotifyFavorites: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onSaveApiConfig: (String, String) -> Unit,
    onSyncFixtures: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "settings_header") {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. Test Notification Card
            item(key = "test_notification_card") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.settings_test_alert_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.settings_test_alert_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onSendTestNotification,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("test_notification_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.settings_test_alert_button))
                        }
                    }
                }
            }

            // 2. Global Reminder Intervals (12h / 2h / 5m)
            item(key = "global_intervals_card") {
                SettingsSectionCard(
                    icon = Icons.Rounded.Alarm,
                    title = stringResource(R.string.settings_intervals_title)
                ) {
                    GlobalIntervalSwitchRow(
                        title = stringResource(R.string.reminder_stage_12h_title),
                        subtitle = "\"Match Today: [Team A] vs [Team B] ([Format]) starts in 12 hours at [Local Time].\"",
                        checked = uiState.preferences.globalNotify12h,
                        onCheckedChange = { onUpdateGlobalInterval(ReminderStage.HOURS_12, it) },
                        testTag = "global_interval_12h_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    GlobalIntervalSwitchRow(
                        title = stringResource(R.string.reminder_stage_2h_title),
                        subtitle = "\"Get Ready: [Team A] vs [Team B] toss coming up soon! Starts in 2 hours.\"",
                        checked = uiState.preferences.globalNotify2h,
                        onCheckedChange = { onUpdateGlobalInterval(ReminderStage.HOURS_2, it) },
                        testTag = "global_interval_2h_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    GlobalIntervalSwitchRow(
                        title = stringResource(R.string.reminder_stage_5m_title),
                        subtitle = "\"Starting Now: [Team A] vs [Team B] begins in 5 minutes at [Venue]!\"",
                        checked = uiState.preferences.globalNotify5m,
                        onCheckedChange = { onUpdateGlobalInterval(ReminderStage.MINUTES_5, it) },
                        testTag = "global_interval_5m_switch"
                    )
                }
            }

            // 3. Favorite Teams & Auto-Notify
            item(key = "favorite_teams_card") {
                SettingsSectionCard(
                    icon = Icons.Rounded.Star,
                    title = stringResource(R.string.settings_favorites_title)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_auto_notify_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_auto_notify_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.preferences.autoNotifyFavorites,
                            onCheckedChange = onSetAutoNotifyFavorites,
                            modifier = Modifier.testTag("auto_notify_favorites_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TeamCatalog.featuredTeams.forEach { team ->
                            val selected = uiState.preferences.favoriteTeams.any {
                                it.equals(team.name, ignoreCase = true)
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleFavoriteTeam(team.name) },
                                leadingIcon = { Text(team.flagEmoji, fontSize = 15.sp) },
                                label = { Text(team.name) },
                                modifier = Modifier.testTag("fav_team_chip_${team.shortCode.lowercase()}")
                            )
                        }
                    }
                }
            }

            // 4. Permissions & Reboot Reliability
            item(key = "permissions_card") {
                SettingsSectionCard(
                    icon = Icons.Rounded.Security,
                    title = "Permissions & Alarm Reliability"
                ) {
                    PermissionRow(
                        label = "Push Notifications (Android 13+)",
                        granted = uiState.permissions.hasNotificationPermission,
                        actionText = stringResource(R.string.action_allow),
                        onAction = onRequestNotificationPermission,
                        testTag = "perm_notifications_action"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    PermissionRow(
                        label = "Exact Alarms (Android 12+)",
                        granted = uiState.permissions.canScheduleExactAlarms,
                        actionText = stringResource(R.string.action_open_settings),
                        onAction = onOpenExactAlarmSettings,
                        testTag = "perm_exact_alarm_action"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reboot & Timezone Recovery",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "WorkManager automatically restores scheduled alarms after reboot or timezone change.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 5. Cricket API Configuration
            item(key = "api_config_card") {
                SettingsSectionCard(
                    icon = Icons.Rounded.CloudSync,
                    title = stringResource(R.string.settings_api_title)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (uiState.isLiveApiConfigured) {
                                    "Mode: Live CricketData.org / CricAPI"
                                } else {
                                    "Mode: Built-in International Schedule"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.preferences.apiBaseUrl,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showApiDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("configure_api_key_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Key,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_api_configure))
                        }
                        Button(
                            onClick = onSyncFixtures,
                            enabled = !uiState.isSyncing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_sync_button")
                        ) {
                            Text(if (uiState.isSyncing) "Syncing…" else stringResource(R.string.settings_sync_now))
                        }
                    }
                }
            }

            // 6. Appearance (Theme Mode & Material You Dynamic Color)
            item(key = "appearance_card") {
                SettingsSectionCard(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.settings_appearance_title)
                ) {
                    val modes = listOf(
                        ThemeMode.SYSTEM to "System",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = uiState.preferences.themeMode == mode,
                                onClick = { onSetThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                                modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                            ) {
                                Text(label)
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_dynamic_color_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = stringResource(R.string.settings_dynamic_color_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.preferences.dynamicColorEnabled,
                                onCheckedChange = onSetDynamicColor,
                                modifier = Modifier.testTag("dynamic_color_switch")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showApiDialog) {
        ApiKeyConfigDialog(
            initialKey = uiState.preferences.customApiKey,
            initialBaseUrl = uiState.preferences.apiBaseUrl,
            onDismiss = { showApiDialog = false },
            onSave = { key, url ->
                showApiDialog = false
                onSaveApiConfig(key, url)
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun GlobalIntervalSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PermissionRow(
    label: String,
    granted: Boolean,
    actionText: String,
    onAction: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }
        }
        if (!granted) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.testTag(testTag)
            ) {
                Text(actionText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ApiKeyConfigDialog(
    initialKey: String,
    initialBaseUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var apiKey by remember { mutableStateOf(initialKey) }
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cricket API Configuration",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Queries /v1/series, /v1/series_info, and /v1/currentMatches using your CricAPI key (also configurable via CRIC_API_KEY in AI Studio Secrets). Leave blank to use the built-in schedule.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("CricAPI Key") },
                    placeholder = { Text("Enter CricketData.org API key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("API Base URL") },
                    placeholder = { Text(UserPreferences.DEFAULT_CRIC_API_BASE_URL) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_base_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKey, baseUrl) },
                modifier = Modifier.testTag("save_api_config_button")
            ) {
                Text("Save & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
