package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CricketMatch
import com.example.model.MatchFormat
import com.example.model.MatchStatus
import com.example.model.UserPreferences
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCricketColors
import com.example.util.MatchTimeFormatter

@Composable
fun MatchCard(
    match: CricketMatch,
    preferences: UserPreferences,
    nowEpochMs: Long,
    onCardClick: () -> Unit,
    onToggleNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cricketColors = LocalCricketColors.current
    val isUpcoming = match.status == MatchStatus.UPCOMING
    val isLive = match.status == MatchStatus.LIVE
    val countdown = if (isUpcoming) MatchTimeFormatter.formatCountdown(match.startTimeEpochMs, nowEpochMs) else null
    val activeAlertCount = match.reminder.activeCount(preferences, match.startTimeEpochMs, nowEpochMs)
    val isAlertActive = match.reminder.enabled && isUpcoming

    val isFavoriteTeamA = preferences.favoriteTeams.any { it.equals(match.teamA, ignoreCase = true) }
    val isFavoriteTeamB = preferences.favoriteTeams.any { it.equals(match.teamB, ignoreCase = true) }

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isLive -> cricketColors.liveDot.copy(alpha = 0.45f)
            isAlertActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        label = "cardBorderColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onCardClick)
            .testTag("match_card_${match.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isLive) 3.dp else 1.dp,
        border = BorderStroke(if (isLive || isAlertActive) 1.25.dp else 1.dp, cardBorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Series Name + Format Pill + Live/Countdown Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatBadge(format = match.format)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${match.seriesName} • ${match.matchNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    isLive -> LivePulseBadge()
                    countdown != null -> CountdownPill(countdown = countdown)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Center Matchup Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TeamMatchRow(
                        flag = match.teamAFlag,
                        teamName = match.teamA,
                        teamCode = match.teamACode,
                        score = match.teamAScore,
                        isFavorite = isFavoriteTeamA
                    )
                    TeamMatchRow(
                        flag = match.teamBFlag,
                        teamName = match.teamB,
                        teamCode = match.teamBCode,
                        score = match.teamBScore,
                        isFavorite = isFavoriteTeamB
                    )
                }

                if (isUpcoming) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val bellBg by animateColorAsState(
                            targetValue = if (isAlertActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            label = "bellBg"
                        )
                        val bellFg by animateColorAsState(
                            targetValue = if (isAlertActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            label = "bellFg"
                        )

                        IconButton(
                            onClick = onToggleNotification,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = bellBg,
                                contentColor = bellFg
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("notify_bell_${match.id}")
                        ) {
                            Icon(
                                imageVector = if (isAlertActive) {
                                    Icons.Rounded.NotificationsActive
                                } else {
                                    Icons.Outlined.NotificationsNone
                                },
                                contentDescription = stringResource(R.string.cd_toggle_notification),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isAlertActive) {
                                if (activeAlertCount > 0) "$activeAlertCount alert${if (activeAlertCount == 1) "" else "s"}" else "On"
                            } else {
                                stringResource(R.string.notify_me)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAlertActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isAlertActive) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Live status or completed match result
            if (!match.resultSummary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLive) {
                        cricketColors.liveDot.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    Text(
                        text = match.resultSummary,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isLive) cricketColors.liveDot else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Automatically converted Local Date/Time + Venue
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = MatchTimeFormatter.formatLocalDateAndTime(match.startTimeEpochMs),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.fullVenue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamMatchRow(
    flag: String,
    teamName: String,
    teamCode: String,
    score: String?,
    isFavorite: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = flag,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isFavorite) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Favorite team",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (!score.isNullOrBlank()) {
            Text(
                text = score,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        } else {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = teamCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun FormatBadge(
    format: MatchFormat,
    modifier: Modifier = Modifier
) {
    val colors = LocalCricketColors.current
    val (bg, fg) = when (format) {
        MatchFormat.TEST -> colors.testBg to colors.testFg
        MatchFormat.ODI -> colors.odiBg to colors.odiFg
        MatchFormat.T20I -> colors.t20Bg to colors.t20Fg
        MatchFormat.ALL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = format.badgeText,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun LivePulseBadge(modifier: Modifier = Modifier) {
    val cricketColors = LocalCricketColors.current
    val transition = rememberInfiniteTransition(label = "livePulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = cricketColors.liveDot.copy(alpha = 0.16f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(cricketColors.liveDot)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = stringResource(R.string.live_badge),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = cricketColors.liveDot
            )
        }
    }
}

@Composable
fun CountdownPill(
    countdown: String,
    modifier: Modifier = Modifier
) {
    val cricketColors = LocalCricketColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = cricketColors.countdownBg
    ) {
        Text(
            text = "In $countdown",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = cricketColors.countdownFg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}
