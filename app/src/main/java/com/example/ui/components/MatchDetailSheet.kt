package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CricketMatch
import com.example.model.MatchStatus
import com.example.model.ReminderStage
import com.example.model.UserPreferences
import com.example.ui.PermissionState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.util.MatchTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailSheet(
    match: CricketMatch,
    preferences: UserPreferences,
    permissions: PermissionState,
    nowEpochMs: Long,
    onDismiss: () -> Unit,
    onToggleMasterReminder: () -> Unit,
    onToggleStage: (ReminderStage, Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isUpcoming = match.status == MatchStatus.UPCOMING
    val countdown = if (isUpcoming) MatchTimeFormatter.formatCountdown(match.startTimeEpochMs, nowEpochMs) else null
    val localShortTime = MatchTimeFormatter.formatLocalTimeOnly(match.startTimeEpochMs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .testTag("match_detail_sheet")
        ) {
            // Header
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
                    Text(
                        text = match.matchNumber,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (countdown != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CountdownPill(countdown = countdown)
                    } else if (match.status == MatchStatus.LIVE) {
                        Spacer(modifier = Modifier.width(8.dp))
                        LivePulseBadge()
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("close_match_detail_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close_sheet)
                    )
                }
            }

            Text(
                text = match.seriesName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scoreboard / Matchup Card
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailTeamRow(
                        flag = match.teamAFlag,
                        name = match.teamA,
                        code = match.teamACode,
                        score = match.teamAScore
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    DetailTeamRow(
                        flag = match.teamBFlag,
                        name = match.teamB,
                        code = match.teamBCode,
                        score = match.teamBScore
                    )

                    if (!match.resultSummary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Text(
                                text = match.resultSummary,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Local Time, UTC Time & Venue Card
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoMetadataRow(
                        icon = Icons.Rounded.Schedule,
                        label = stringResource(R.string.match_local_time_label),
                        value = MatchTimeFormatter.formatLocalFullDateTime(match.startTimeEpochMs),
                        monoValue = true
                    )
                    InfoMetadataRow(
                        icon = Icons.Rounded.Public,
                        label = stringResource(R.string.match_utc_time_label),
                        value = MatchTimeFormatter.formatUtcFullDateTime(match.startTimeEpochMs),
                        monoValue = true
                    )
                    InfoMetadataRow(
                        icon = Icons.Rounded.LocationOn,
                        label = stringResource(R.string.match_venue_label),
                        value = match.fullVenue,
                        monoValue = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Multi-Stage Notification Schedule Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.match_reminders_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isUpcoming) {
                    Switch(
                        checked = match.reminder.enabled,
                        onCheckedChange = { onToggleMasterReminder() },
                        modifier = Modifier.testTag("detail_master_reminder_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isUpcoming) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.match_already_started_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                // Permission callouts if missing
                if (!permissions.hasNotificationPermission) {
                    PermissionInlineAlert(
                        text = "Notifications are currently disabled. Grant permission to receive match alerts.",
                        actionLabel = stringResource(R.string.action_allow),
                        onAction = onRequestNotificationPermission
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (!permissions.canScheduleExactAlarms) {
                    PermissionInlineAlert(
                        text = "Exact Alarms permission is recommended so 5-minute alerts fire right on time.",
                        actionLabel = stringResource(R.string.action_open_settings),
                        onAction = onOpenExactAlarmSettings
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3 Stage Cards: 12h, 2h, 5m
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReminderStageRow(
                        stage = ReminderStage.HOURS_12,
                        isMasterEnabled = match.reminder.enabled,
                        isStageEnabled = match.reminder.notify12h,
                        isGlobalEnabled = preferences.globalNotify12h,
                        triggerEpochMs = match.startTimeEpochMs - ReminderStage.HOURS_12.offsetMillis,
                        nowEpochMs = nowEpochMs,
                        previewMessage = ReminderStage.HOURS_12.buildNotificationBody(
                            teamA = match.teamA,
                            teamB = match.teamB,
                            formatBadge = match.format.badgeText,
                            localTime = localShortTime,
                            venue = match.venue
                        ),
                        onToggle = { enabled -> onToggleStage(ReminderStage.HOURS_12, enabled) }
                    )
                    ReminderStageRow(
                        stage = ReminderStage.HOURS_2,
                        isMasterEnabled = match.reminder.enabled,
                        isStageEnabled = match.reminder.notify2h,
                        isGlobalEnabled = preferences.globalNotify2h,
                        triggerEpochMs = match.startTimeEpochMs - ReminderStage.HOURS_2.offsetMillis,
                        nowEpochMs = nowEpochMs,
                        previewMessage = ReminderStage.HOURS_2.buildNotificationBody(
                            teamA = match.teamA,
                            teamB = match.teamB,
                            formatBadge = match.format.badgeText,
                            localTime = localShortTime,
                            venue = match.venue
                        ),
                        onToggle = { enabled -> onToggleStage(ReminderStage.HOURS_2, enabled) }
                    )
                    ReminderStageRow(
                        stage = ReminderStage.MINUTES_5,
                        isMasterEnabled = match.reminder.enabled,
                        isStageEnabled = match.reminder.notify5m,
                        isGlobalEnabled = preferences.globalNotify5m,
                        triggerEpochMs = match.startTimeEpochMs - ReminderStage.MINUTES_5.offsetMillis,
                        nowEpochMs = nowEpochMs,
                        previewMessage = ReminderStage.MINUTES_5.buildNotificationBody(
                            teamA = match.teamA,
                            teamB = match.teamB,
                            formatBadge = match.format.badgeText,
                            localTime = localShortTime,
                            venue = match.venue
                        ),
                        onToggle = { enabled -> onToggleStage(ReminderStage.MINUTES_5, enabled) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTeamRow(
    flag: String,
    name: String,
    code: String,
    score: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(text = flag, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!score.isNullOrBlank()) {
            Text(
                text = score,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InfoMetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    monoValue: Boolean
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = if (monoValue) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReminderStageRow(
    stage: ReminderStage,
    isMasterEnabled: Boolean,
    isStageEnabled: Boolean,
    isGlobalEnabled: Boolean,
    triggerEpochMs: Long,
    nowEpochMs: Long,
    previewMessage: String,
    onToggle: (Boolean) -> Unit
) {
    val isPassed = triggerEpochMs <= nowEpochMs
    val effectiveOn = isMasterEnabled && isStageEnabled && isGlobalEnabled && !isPassed

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (effectiveOn) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            1.dp,
            if (effectiveOn) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = if (effectiveOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stage.fullLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val timeSubtitle = when {
                            isPassed -> stringResource(R.string.reminder_passed)
                            !isGlobalEnabled -> stringResource(R.string.reminder_muted_globally)
                            else -> MatchTimeFormatter.formatReminderTimestamp(triggerEpochMs, nowEpochMs)
                        }
                        Text(
                            text = timeSubtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = when {
                                isPassed -> MaterialTheme.colorScheme.onSurfaceVariant
                                !isGlobalEnabled -> MaterialTheme.colorScheme.error
                                effectiveOn -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Switch(
                    checked = isMasterEnabled && isStageEnabled && isGlobalEnabled,
                    enabled = !isPassed && isGlobalEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("stage_switch_${stage.shortLabel}")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "\"$previewMessage\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PermissionInlineAlert(
    text: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
