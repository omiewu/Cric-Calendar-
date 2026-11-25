package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsCricket
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CricketMatch
import com.example.model.MatchFormat
import com.example.model.MatchStatus
import com.example.model.TeamCatalog
import com.example.ui.CricPulseUiState
import com.example.ui.components.MatchCard
import com.example.ui.theme.EmeraldPitchDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCricketColors
import com.example.util.MatchTimeFormatter

@Composable
fun FixturesScreen(
    uiState: CricPulseUiState,
    onSelectFormat: (MatchFormat) -> Unit,
    onSelectStatus: (MatchStatus) -> Unit,
    onSelectTeam: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onSyncClick: () -> Unit,
    onMatchClick: (CricketMatch) -> Unit,
    onToggleNotification: (CricketMatch) -> Unit,
    onRequestNotificationPermission: () -> Unit,
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
                .testTag("fixtures_list"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Stadium Banner Header
            item(key = "hero_header") {
                StadiumHeroHeader(
                    liveCount = uiState.liveCount,
                    isSyncing = uiState.isSyncing,
                    isLiveApi = uiState.isLiveApiConfigured,
                    onSyncClick = onSyncClick
                )
            }

            // Notification Permission Prompt Banner (if missing on Android 13+)
            if (!uiState.permissions.hasNotificationPermission) {
                item(key = "permission_banner") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.permission_notifications_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.permission_notifications_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = onRequestNotificationPermission,
                                modifier = Modifier.testTag("banner_allow_notifications_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.action_allow),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = uiState.filters.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(visible = uiState.filters.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("team_search_input")
                )
            }

            // Format Filter Chips (All, Test, ODI, T20I)
            item(key = "format_chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MatchFormat.entries.forEach { format ->
                        val selected = uiState.filters.format == format
                        val cricketColors = LocalCricketColors.current
                        val selectedBg = when (format) {
                            MatchFormat.TEST -> cricketColors.testBg
                            MatchFormat.ODI -> cricketColors.odiBg
                            MatchFormat.T20I -> cricketColors.t20Bg
                            MatchFormat.ALL -> MaterialTheme.colorScheme.primaryContainer
                        }
                        val selectedFg = when (format) {
                            MatchFormat.TEST -> cricketColors.testFg
                            MatchFormat.ODI -> cricketColors.odiFg
                            MatchFormat.T20I -> cricketColors.t20Fg
                            MatchFormat.ALL -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectFormat(format) },
                            label = {
                                Text(
                                    text = format.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = selectedBg,
                                selectedLabelColor = selectedFg
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("format_chip_${format.name.lowercase()}")
                        )
                    }
                }
            }

            // Quick-Filter Team Pills
            item(key = "team_pills") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("team_pills_row")
                ) {
                    item(key = "all_teams") {
                        val isAllSelected = uiState.filters.teamFilter == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { onSelectTeam(null) },
                            label = { Text(stringResource(R.string.filter_all_teams)) },
                            modifier = Modifier.testTag("team_pill_all")
                        )
                    }
                    items(
                        items = TeamCatalog.featuredTeams.take(10),
                        key = { it.shortCode }
                    ) { team ->
                        val selected = uiState.filters.teamFilter.equals(team.name, ignoreCase = true)
                        val isFav = uiState.preferences.favoriteTeams.any { it.equals(team.name, ignoreCase = true) }
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTeam(team.name) },
                            leadingIcon = {
                                Text(text = team.flagEmoji, fontSize = 15.sp)
                            },
                            trailingIcon = if (isFav) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = "Favorite team",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            label = { Text(team.name) },
                            modifier = Modifier.testTag("team_pill_${team.shortCode.lowercase()}")
                        )
                    }
                }
            }

            // Status Tabs (Upcoming / Live / Completed)
            item(key = "status_tabs") {
                val statuses = listOf(
                    Triple(MatchStatus.UPCOMING, stringResource(R.string.tab_upcoming), uiState.upcomingCount),
                    Triple(MatchStatus.LIVE, stringResource(R.string.tab_live), uiState.liveCount),
                    Triple(MatchStatus.COMPLETED, stringResource(R.string.tab_completed), uiState.completedCount)
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    statuses.forEachIndexed { index, (status, label, count) ->
                        val selected = uiState.filters.status == status
                        SegmentedButton(
                            selected = selected,
                            onClick = { onSelectStatus(status) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("status_tab_${status.name.lowercase()}")
                        ) {
                            Text(
                                text = "$label ($count)",
                                maxLines = 1,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Match List or Empty State
            if (uiState.filteredMatches.isEmpty()) {
                item(key = "empty_fixtures") {
                    EmptyFixturesCard(
                        onResetFilters = onClearFilters,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(
                    items = uiState.filteredMatches,
                    key = { it.id }
                ) { match ->
                    MatchCard(
                        match = match,
                        preferences = uiState.preferences,
                        nowEpochMs = uiState.nowEpochMs,
                        onCardClick = { onMatchClick(match) },
                        onToggleNotification = { onToggleNotification(match) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StadiumHeroHeader(
    liveCount: Int,
    isSyncing: Boolean,
    isLiveApi: Boolean,
    onSyncClick: () -> Unit
) {
    val year = MatchTimeFormatter.currentCalendarYear()
    val tzBadge = MatchTimeFormatter.currentTimeZoneBadge()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_stadium_hero),
            contentDescription = stringResource(R.string.cd_stadium_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Rich dark emerald-slate atmosphere overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            EmeraldPitchDark.copy(alpha = 0.65f),
                            Color(0xFF091417).copy(alpha = 0.78f),
                            Color(0xFF071114).copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.45f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SportsCricket,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "$year International Season",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }

                IconButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("sync_fixtures_button")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF34D399)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_fixtures),
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom badges row: Local Timezone + Live count / API status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tzBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = Color.White
                        )
                    }
                }

                if (liveCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEF4444).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = "● $liveCount LIVE NOW",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFECACA),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isLiveApi) "Live API" else "Built-in Feed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD1FAE5),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFixturesCard(
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsCricket,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.empty_fixtures_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.empty_fixtures_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onResetFilters,
                modifier = Modifier.testTag("reset_filters_button")
            ) {
                Text(stringResource(R.string.action_clear_filters))
            }
        }
    }
}
