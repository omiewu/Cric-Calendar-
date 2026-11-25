package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.CricPulseApplication
import com.example.data.repository.SyncOutcome
import com.example.model.CricketMatch
import com.example.model.MatchFormat
import com.example.model.MatchStatus
import com.example.model.ReminderStage
import com.example.model.UserPreferences
import com.example.notifications.NotificationHelper
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    FIXTURES, ALERTS, SETTINGS
}

data class FilterState(
    val format: MatchFormat = MatchFormat.ALL,
    val status: MatchStatus = MatchStatus.UPCOMING,
    val teamFilter: String? = null,
    val searchQuery: String = ""
)

data class PermissionState(
    val hasNotificationPermission: Boolean = true,
    val canScheduleExactAlarms: Boolean = true
)

data class CricPulseUiState(
    val allMatches: List<CricketMatch> = emptyList(),
    val filteredMatches: List<CricketMatch> = emptyList(),
    val alertedMatches: List<CricketMatch> = emptyList(),
    val selectedMatch: CricketMatch? = null,
    val filters: FilterState = FilterState(),
    val preferences: UserPreferences = UserPreferences(),
    val permissions: PermissionState = PermissionState(),
    val isSyncing: Boolean = false,
    val isLiveApiConfigured: Boolean = false,
    val upcomingCount: Int = 0,
    val liveCount: Int = 0,
    val completedCount: Int = 0,
    val nowEpochMs: Long = System.currentTimeMillis()
)

class CricPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CricPulseApplication
    private val repository = app.matchRepository
    private val preferencesRepo = app.preferencesRepository

    private val filtersFlow = MutableStateFlow(FilterState())
    private val selectedMatchIdFlow = MutableStateFlow<String?>(null)
    private val isSyncingFlow = MutableStateFlow(false)
    private val permissionsFlow = MutableStateFlow(readPermissions())

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _requestNotificationPermissionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 2)
    val requestNotificationPermissionEvent: SharedFlow<Unit> = _requestNotificationPermissionEvent.asSharedFlow()

    private val clockTickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000L)
        }
    }

    private val baseDataFlow = combine(
        repository.matchesFlow,
        repository.preferencesFlow,
        filtersFlow,
        selectedMatchIdFlow
    ) { matches, prefs, filters, selectedId ->
        QuadData(matches, prefs, filters, selectedId)
    }

    val uiState: StateFlow<CricPulseUiState> = combine(
        baseDataFlow,
        isSyncingFlow,
        permissionsFlow,
        clockTickerFlow
    ) { base, syncing, perms, nowMs ->
        val matches = base.matches
        val filters = base.filters
        val prefs = base.prefs

        val formatAndTeamMatches = matches.filter { match ->
            val formatOk = filters.format == MatchFormat.ALL || match.format == filters.format
            val teamOk = filters.teamFilter.isNullOrBlank() || match.involvesTeam(filters.teamFilter)
            val searchOk = match.matchesSearch(filters.searchQuery)
            formatOk && teamOk && searchOk
        }

        val upcomingCount = formatAndTeamMatches.count { it.status == MatchStatus.UPCOMING }
        val liveCount = formatAndTeamMatches.count { it.status == MatchStatus.LIVE }
        val completedCount = formatAndTeamMatches.count { it.status == MatchStatus.COMPLETED }

        val filtered = formatAndTeamMatches
            .filter { it.status == filters.status }
            .let { list ->
                when (filters.status) {
                    MatchStatus.UPCOMING -> list.sortedBy { it.startTimeEpochMs }
                    MatchStatus.LIVE -> list.sortedByDescending { it.startTimeEpochMs }
                    MatchStatus.COMPLETED -> list.sortedByDescending { it.startTimeEpochMs }
                }
            }

        val alerted = matches
            .filter { it.reminder.enabled && it.status == MatchStatus.UPCOMING }
            .sortedBy { it.startTimeEpochMs }

        val selectedMatch = base.selectedId?.let { id -> matches.firstOrNull { it.id == id } }

        CricPulseUiState(
            allMatches = matches,
            filteredMatches = filtered,
            alertedMatches = alerted,
            selectedMatch = selectedMatch,
            filters = filters,
            preferences = prefs,
            permissions = perms,
            isSyncing = syncing,
            isLiveApiConfigured = repository.resolveEffectiveApiKey(prefs).isNotEmpty(),
            upcomingCount = upcomingCount,
            liveCount = liveCount,
            completedCount = completedCount,
            nowEpochMs = nowMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CricPulseUiState(permissions = readPermissions())
    )

    init {
        syncFixtures(forceRemote = false, showToast = false)
    }

    private fun readPermissions(): PermissionState = PermissionState(
        hasNotificationPermission = NotificationHelper.hasNotificationPermission(app),
        canScheduleExactAlarms = NotificationHelper.canScheduleExactAlarms(app)
    )

    fun refreshPermissions() {
        permissionsFlow.value = readPermissions()
        viewModelScope.launch {
            repository.rescheduleAllEnabledAlarms()
        }
    }

    fun selectFormat(format: MatchFormat) {
        filtersFlow.update { it.copy(format = format) }
    }

    fun selectStatus(status: MatchStatus) {
        filtersFlow.update { it.copy(status = status) }
    }

    fun selectTeamFilter(teamName: String?) {
        filtersFlow.update { current ->
            val next = if (current.teamFilter == teamName) null else teamName
            current.copy(teamFilter = next)
        }
    }

    fun updateSearchQuery(query: String) {
        filtersFlow.update { it.copy(searchQuery = query) }
    }

    fun clearFilters() {
        filtersFlow.update {
            it.copy(
                format = MatchFormat.ALL,
                teamFilter = null,
                searchQuery = ""
            )
        }
    }

    fun openMatchDetail(matchId: String) {
        selectedMatchIdFlow.value = matchId
    }

    fun closeMatchDetail() {
        selectedMatchIdFlow.value = null
    }

    fun toggleMatchReminder(match: CricketMatch) {
        if (match.status != MatchStatus.UPCOMING) {
            _snackbarEvents.tryEmit("Pre-match alerts are only available for upcoming fixtures.")
            return
        }
        viewModelScope.launch {
            val wasEnabled = match.reminder.enabled
            if (!wasEnabled && !NotificationHelper.hasNotificationPermission(app)) {
                _requestNotificationPermissionEvent.tryEmit(Unit)
            }
            val nowEnabled = repository.toggleMatchReminder(match.id)
            if (nowEnabled) {
                val exact = NotificationHelper.canScheduleExactAlarms(app)
                val suffix = if (exact) "" else " (enable Exact Alarms in Settings for pinpoint timing)"
                _snackbarEvents.tryEmit("Alerts scheduled for ${match.teamACode} vs ${match.teamBCode}$suffix")
            } else {
                _snackbarEvents.tryEmit("Alerts muted for ${match.teamACode} vs ${match.teamBCode}")
            }
        }
    }

    fun updateMatchReminderStage(matchId: String, stage: ReminderStage, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !NotificationHelper.hasNotificationPermission(app)) {
                _requestNotificationPermissionEvent.tryEmit(Unit)
            }
            repository.updateMatchReminderStage(matchId, stage, enabled)
        }
    }

    fun toggleFavoriteTeam(teamName: String) {
        viewModelScope.launch {
            repository.toggleFavoriteTeam(teamName)
        }
    }

    fun setAutoNotifyFavorites(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !NotificationHelper.hasNotificationPermission(app)) {
                _requestNotificationPermissionEvent.tryEmit(Unit)
            }
            repository.setAutoNotifyFavorites(enabled)
            if (enabled) {
                _snackbarEvents.tryEmit("Auto-notify enabled for upcoming favorite team fixtures.")
            }
        }
    }

    fun updateGlobalInterval(stage: ReminderStage, enabled: Boolean) {
        viewModelScope.launch {
            val prefs = uiState.value.preferences
            val next12h = if (stage == ReminderStage.HOURS_12) enabled else prefs.globalNotify12h
            val next2h = if (stage == ReminderStage.HOURS_2) enabled else prefs.globalNotify2h
            val next5m = if (stage == ReminderStage.MINUTES_5) enabled else prefs.globalNotify5m
            repository.updateGlobalIntervals(next12h, next2h, next5m)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepo.setThemeMode(mode)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setDynamicColorEnabled(enabled)
        }
    }

    fun saveApiConfig(apiKey: String, baseUrl: String) {
        viewModelScope.launch {
            preferencesRepo.saveApiConfig(apiKey, baseUrl)
            syncFixtures(forceRemote = true, showToast = true)
        }
    }

    fun syncFixtures(forceRemote: Boolean = true, showToast: Boolean = true) {
        if (isSyncingFlow.value) return
        viewModelScope.launch {
            isSyncingFlow.value = true
            try {
                val outcome = repository.ensureSeededAndSync(forceRemote = forceRemote)
                if (showToast) {
                    val msg = when (outcome) {
                        is SyncOutcome.LiveApiSuccess -> "Synced ${outcome.matchCount} live fixtures from Cricket API."
                        is SyncOutcome.BuiltInScheduleLoaded -> outcome.note
                    }
                    _snackbarEvents.tryEmit(msg)
                }
            } finally {
                isSyncingFlow.value = false
            }
        }
    }

    fun sendTestNotification() {
        if (!NotificationHelper.hasNotificationPermission(app)) {
            _requestNotificationPermissionEvent.tryEmit(Unit)
            _snackbarEvents.tryEmit("Please grant notification permission first.")
            return
        }
        val posted = NotificationHelper.postTestNotification(app)
        if (posted) {
            _snackbarEvents.tryEmit("Test match alert sent! Check your notification shade.")
        } else {
            _snackbarEvents.tryEmit("Could not post notification. Check system notification settings.")
        }
    }

    private data class QuadData(
        val matches: List<CricketMatch>,
        val prefs: UserPreferences,
        val filters: FilterState,
        val selectedId: String?
    )
}
