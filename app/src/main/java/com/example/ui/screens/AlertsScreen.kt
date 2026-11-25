package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOn
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CricketMatch
import com.example.model.ReminderStage
import com.example.model.UserPreferences
import com.example.ui.CricPulseUiState
import com.example.ui.components.CountdownPill
import com.example.ui.components.FormatBadge
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.MatchTimeFormatter

@Composable
fun AlertsScreen(
    uiState: CricPulseUiState,
    onMatchClick: (CricketMatch) -> Unit,
    onDisableAlert: (CricketMatch) -> Unit,
    onGoToFixtures: () -> Unit,
    onGoToSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .testTag("alerts_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "alerts_header") {
                Column {
                    Text(
                        text = "Scheduled Match Alerts",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.alertedMatches.size} upcoming fixture${if (uiState.alertedMatches.size == 1) "" else "s"} armed • ${MatchTimeFormatter.currentTimeZoneBadge()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Permission Health Banner
            item(key = "permission_health") {
                PermissionHealthCard(
                    hasNotifications = uiState.permissions.hasNotificationPermission,
                    hasExactAlarms = uiState.permissions.canScheduleExactAlarms,
                    onGrantNotifications = onRequestNotificationPermission,
                    onGrantExactAlarms = onOpenExactAlarmSettings
                )
            }

            if (uiState.alertedMatches.isEmpty()) {
                item(key = "empty_alerts") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.empty_alerts_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.empty_alerts_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onGoToFixtures,
                                    modifier = Modifier.testTag("browse_fixtures_cta")
                                ) {
                                    Text("Browse Fixtures")
                                }
                                OutlinedButton(
                                    onClick = onGoToSettings,
                                    modifier = Modifier.testTag("favorite_teams_cta")
                                ) {
                                    Text("Favorite Teams")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.alertedMatches,
                    key = { "alert_${it.id}" }
                ) { match ->
                    ScheduledAlertCard(
                        match = match,
                        preferences = uiState.preferences,
                        nowEpochMs = uiState.nowEpochMs,
                        onOpenDetail = { onMatchClick(match) },
                        onMute = { onDisableAlert(match) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionHealthCard(
    hasNotifications: Boolean,
    hasExactAlarms: Boolean,
    onGrantNotifications: () -> Unit,
    onGrantExactAlarms: () -> Unit
) {
    val allGood = hasNotifications && hasExactAlarms
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (allGood) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        },
        border = BorderStroke(
            1.dp,
            if (allGood) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (allGood) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = if (allGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (allGood) {
                        "Exact Alarm Engine Ready"
                    } else {
                        "Action Needed for Exact Pre-Match Alerts"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (allGood) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (allGood) {
                    "Notifications, Exact Alarms, and Reboot Recovery via WorkManager are active."
                } else {
                    "Grant both Notifications and Exact Alarms so 12h, 2h, and 5m reminders fire accurately even in Doze mode."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (allGood) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
            )

            if (!allGood) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hasNotifications) {
                        TextButton(onClick = onGrantNotifications) {
                            Text("Enable Notifications", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!hasExactAlarms) {
                        TextButton(onClick = onGrantExactAlarms) {
                            Text("Allow Exact Alarms", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduledAlertCard(
    match: CricketMatch,
    preferences: UserPreferences,
    nowEpochMs: Long,
    onOpenDetail: () -> Unit,
    onMute: () -> Unit
) {
    val countdown = MatchTimeFormatter.formatCountdown(match.startTimeEpochMs, nowEpochMs)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpenDetail)
            .testTag("scheduled_alert_card_${match.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    FormatBadge(format = match.format)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (countdown != null) {
                        CountdownPill(countdown = countdown)
                    } else {
                        Text(
                            text = MatchTimeFormatter.formatLocalDateAndTime(match.startTimeEpochMs),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onMute,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("mute_alert_${match.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsOff,
                        contentDescription = "Mute alerts for ${match.matchupTitle}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${match.teamAFlag} ${match.teamA}  vs  ${match.teamBFlag} ${match.teamB}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${match.seriesName} • ${match.fullVenue}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            val stages = listOf(
                Triple(ReminderStage.HOURS_12, match.reminder.notify12h && preferences.globalNotify12h, match.startTimeEpochMs - ReminderStage.HOURS_12.offsetMillis),
                Triple(ReminderStage.HOURS_2, match.reminder.notify2h && preferences.globalNotify2h, match.startTimeEpochMs - ReminderStage.HOURS_2.offsetMillis),
                Triple(ReminderStage.MINUTES_5, match.reminder.notify5m && preferences.globalNotify5m, match.startTimeEpochMs - ReminderStage.MINUTES_5.offsetMillis)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stages.forEach { (stage, enabled, triggerMs) ->
                    val upcoming = enabled && triggerMs > nowEpochMs
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (upcoming) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AlarmOn,
                                contentDescription = null,
                                tint = if (upcoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (upcoming) {
                                    "${stage.shortLabel}: ${MatchTimeFormatter.formatReminderTimestamp(triggerMs, nowEpochMs)}"
                                } else {
                                    "${stage.shortLabel}: ${if (!enabled) "Off" else "Passed"}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp
                                ),
                                color = if (upcoming) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
