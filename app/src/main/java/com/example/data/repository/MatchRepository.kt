package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.MatchDao
import com.example.data.local.MatchEntity
import com.example.data.local.MatchReminderDao
import com.example.data.local.MatchReminderEntity
import com.example.data.mock.MockCricketDataset
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.CricApiClientFactory
import com.example.data.remote.CricApiMatchDto
import com.example.data.remote.CricApiSeriesDto
import com.example.model.CricketMatch
import com.example.model.MatchFormat
import com.example.model.MatchReminder
import com.example.model.MatchStatus
import com.example.model.ReminderStage
import com.example.model.TeamCatalog
import com.example.model.UserPreferences
import com.example.notifications.MatchAlarmScheduler
import com.example.util.MatchTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

sealed interface SyncOutcome {
    data class LiveApiSuccess(val matchCount: Int, val seriesCount: Int) : SyncOutcome
    data class BuiltInScheduleLoaded(val matchCount: Int, val note: String) : SyncOutcome
}

class MatchRepository(
    private val matchDao: MatchDao,
    private val reminderDao: MatchReminderDao,
    private val preferencesRepository: UserPreferencesRepository,
    private val alarmScheduler: MatchAlarmScheduler
) {

    val preferencesFlow: Flow<UserPreferences> = preferencesRepository.preferencesFlow

    val matchesFlow: Flow<List<CricketMatch>> = combine(
        matchDao.observeAllMatches(),
        reminderDao.observeAllReminders()
    ) { entities, reminders ->
        val reminderMap = reminders.associateBy { it.matchId }
        entities.map { entity ->
            val remEntity = reminderMap[entity.id]
            val reminder = if (remEntity != null) {
                MatchReminder(
                    matchId = entity.id,
                    enabled = remEntity.enabled,
                    notify12h = remEntity.notify12h,
                    notify2h = remEntity.notify2h,
                    notify5m = remEntity.notify5m
                )
            } else {
                MatchReminder(matchId = entity.id, enabled = false)
            }
            entity.toDomain(reminder)
        }
    }

    fun resolveEffectiveApiKey(prefs: UserPreferences): String {
        val custom = prefs.customApiKey.trim()
        if (custom.isNotEmpty()) return custom
        val buildConfigKey = BuildConfig.CRIC_API_KEY.trim()
        return if (buildConfigKey.isNotEmpty() && buildConfigKey != "YOUR_CRICAPI_KEY") {
            buildConfigKey
        } else {
            ""
        }
    }

    suspend fun ensureSeededAndSync(forceRemote: Boolean = false): SyncOutcome = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.getPreferencesOnce()
        val apiKey = resolveEffectiveApiKey(prefs)
        var remoteFailureReason: String? = null

        if (apiKey.isNotEmpty()) {
            val remoteFetch = fetchFromCricApi(apiKey, prefs.apiBaseUrl)
            remoteFailureReason = remoteFetch.failureReason
            if (remoteFetch.matches.isNotEmpty()) {
                matchDao.clearAllMatches()
                matchDao.upsertMatches(remoteFetch.matches)
                if (prefs.autoNotifyFavorites && prefs.favoriteTeams.isNotEmpty()) {
                    applyAutoNotifyFavoritesInternal(prefs)
                }
                rescheduleAllEnabledAlarms()
                return@withContext SyncOutcome.LiveApiSuccess(
                    matchCount = remoteFetch.matches.size,
                    seriesCount = remoteFetch.seriesCount
                )
            }
        }

        val existingCount = matchDao.countMatches()
        if (existingCount == 0 || forceRemote) {
            val builtIn = MockCricketDataset.generateCurrentYearFixtures()
            matchDao.upsertMatches(builtIn)
            if (prefs.autoNotifyFavorites && prefs.favoriteTeams.isNotEmpty()) {
                applyAutoNotifyFavoritesInternal(prefs)
            }
            rescheduleAllEnabledAlarms()
            val note = when {
                !remoteFailureReason.isNullOrBlank() ->
                    "CricAPI: $remoteFailureReason. Showing built-in ${MatchTimeFormatter.currentCalendarYear()} schedule."
                apiKey.isNotEmpty() ->
                    "Live API returned no international fixtures; showing built-in ${MatchTimeFormatter.currentCalendarYear()} schedule."
                else ->
                    "Loaded built-in ${MatchTimeFormatter.currentCalendarYear()} international schedule."
            }
            SyncOutcome.BuiltInScheduleLoaded(builtIn.size, note)
        } else {
            rescheduleAllEnabledAlarms()
            SyncOutcome.BuiltInScheduleLoaded(existingCount, "Showing cached international schedule.")
        }
    }

    private data class RemoteFetchResult(
        val matches: List<MatchEntity>,
        val seriesCount: Int,
        val failureReason: String? = null
    )

    private suspend fun fetchFromCricApi(apiKey: String, baseUrl: String): RemoteFetchResult {
        return try {
            val api = CricApiClientFactory.create(baseUrl)
            val nowMs = System.currentTimeMillis()

            // Primary call: GET /v1/series?apikey=...&offset=0
            val seriesResp = api.getSeries(apiKey = apiKey, offset = 0)
            if (seriesResp.status.equals("failure", ignoreCase = true)) {
                return RemoteFetchResult(
                    matches = emptyList(),
                    seriesCount = 0,
                    failureReason = seriesResp.reason ?: "API request rejected"
                )
            }

            val allSeries = seriesResp.data.orEmpty()
            val prioritizedSeries = prioritizeInternationalSeries(allSeries).take(8)

            // Fetch match lists for prioritized series + current live matches concurrently
            val (seriesMatches, currentMatches) = coroutineScope {
                val seriesJobs = prioritizedSeries.map { series ->
                    async {
                        val infoResp = runCatching {
                            api.getSeriesInfo(apiKey = apiKey, seriesId = series.id)
                        }.getOrNull()
                        val resolvedSeriesName = infoResp?.data?.info?.name?.takeIf { it.isNotBlank() }
                            ?: series.name?.takeIf { it.isNotBlank() }
                        infoResp?.data?.matchList.orEmpty().map { dto ->
                            dto to resolvedSeriesName
                        }
                    }
                }
                val currentJob = async {
                    runCatching { api.getCurrentMatches(apiKey = apiKey, offset = 0) }
                        .getOrNull()
                        ?.data
                        .orEmpty()
                }
                seriesJobs.awaitAll().flatten() to currentJob.await()
            }

            val seriesNameById = allSeries.associate { it.id to it.name }
            val byMatchId = LinkedHashMap<String, Pair<CricApiMatchDto, String?>>()

            // Add series fixtures first (with authoritative series name)
            seriesMatches.forEach { (dto, seriesName) ->
                byMatchId[dto.id] = dto to seriesName
            }
            // Merge live/current matches (preserving live scores and authoritative series names)
            currentMatches.forEach { dto ->
                val existingSeriesName = byMatchId[dto.id]?.second ?: dto.seriesId?.let { seriesNameById[it] }
                byMatchId[dto.id] = dto to existingSeriesName
            }

            // Fallback to /v1/matches if series_info returned no match rows
            if (byMatchId.isEmpty()) {
                val fallbackMatches = runCatching { api.getMatches(apiKey = apiKey, offset = 0) }
                    .getOrNull()
                    ?.data
                    .orEmpty()
                fallbackMatches.forEach { dto ->
                    val sName = dto.seriesId?.let { seriesNameById[it] }
                    byMatchId[dto.id] = dto to sName
                }
            }

            val entities = byMatchId.values.mapNotNull { (dto, seriesNameOverride) ->
                mapDtoToEntity(dto, nowMs, seriesNameOverride)
            }

            RemoteFetchResult(
                matches = entities,
                seriesCount = prioritizedSeries.size
            )
        } catch (e: Exception) {
            RemoteFetchResult(
                matches = emptyList(),
                seriesCount = 0,
                failureReason = e.localizedMessage
            )
        }
    }

    private fun prioritizeInternationalSeries(seriesList: List<CricApiSeriesDto>): List<CricApiSeriesDto> {
        val internationalKeywords = listOf(
            "tour of", "icc", "world cup", "champions trophy", "asia cup", "ashes",
            "border-gavaskar", "tri-series", "t20i", "odi", "test"
        )
        val majorTeamNames = TeamCatalog.featuredTeams.map { it.name.lowercase() }

        return seriesList
            .filter { (it.matches ?: 0) > 0 || (it.odi ?: 0) > 0 || (it.t20 ?: 0) > 0 || (it.test ?: 0) > 0 }
            .sortedByDescending { series ->
                val lowerName = series.name?.lowercase().orEmpty()
                var score = 0
                if ((series.test ?: 0) > 0) score += 40
                if ((series.odi ?: 0) > 0) score += 30
                if (internationalKeywords.any { lowerName.contains(it) }) score += 25
                if (majorTeamNames.any { lowerName.contains(it) }) score += 35
                score
            }
    }

    private fun mapDtoToEntity(
        dto: CricApiMatchDto,
        nowMs: Long,
        seriesNameOverride: String? = null
    ): MatchEntity? {
        val rawType = dto.matchType?.trim()?.lowercase() ?: return null
        val format = when (rawType) {
            "test" -> MatchFormat.TEST
            "odi" -> MatchFormat.ODI
            "t20", "t20i" -> MatchFormat.T20I
            else -> return null
        }

        val teams = dto.teams?.takeIf { it.size >= 2 } ?: return null
        val teamA = teams[0].trim()
        val teamB = teams[1].trim()

        val rawIso = dto.dateTimeGmt?.takeIf { it.isNotBlank() } ?: dto.date ?: return null
        val epochMs = MatchTimeFormatter.parseUtcIsoToEpochMs(rawIso) ?: return null

        val status = when {
            dto.matchEnded == true -> MatchStatus.COMPLETED
            dto.matchStarted == true -> MatchStatus.LIVE
            epochMs > nowMs -> MatchStatus.UPCOMING
            else -> MatchStatus.COMPLETED
        }

        val scores = dto.score.orEmpty()
        val scoreA = scores.firstOrNull { it.inning?.contains(teamA, ignoreCase = true) == true }
            ?.let { formatApiScore(it.runs, it.wickets, it.overs) }
            ?: scores.getOrNull(0)?.let { formatApiScore(it.runs, it.wickets, it.overs) }
        val scoreB = scores.firstOrNull { it.inning?.contains(teamB, ignoreCase = true) == true }
            ?.let { formatApiScore(it.runs, it.wickets, it.overs) }
            ?: scores.getOrNull(1)?.let { formatApiScore(it.runs, it.wickets, it.overs) }

        val fullName = dto.name?.trim().orEmpty()
        val parts = fullName.split(",").map { it.trim() }
        val seriesTitle = seriesNameOverride?.takeIf { it.isNotBlank() }
            ?: parts.getOrNull(1)?.ifBlank { null }
            ?: "International ${format.label} Series"
        val matchNum = parts.getOrNull(1)?.takeIf { !seriesNameOverride.isNullOrBlank() }
            ?: parts.getOrNull(0)?.ifBlank { null }
            ?: "${format.label} Match"

        val venueRaw = dto.venue?.trim().orEmpty().ifBlank { "International Stadium" }
        val venueParts = venueRaw.split(",").map { it.trim() }
        val venueName = venueParts.firstOrNull() ?: venueRaw
        val cityName = venueParts.lastOrNull()?.takeIf { venueParts.size > 1 } ?: ""

        return MatchEntity(
            id = "api_${dto.id}",
            seriesName = seriesTitle,
            matchNumber = matchNum,
            formatCode = format.name,
            statusCode = status.name,
            teamA = teamA,
            teamB = teamB,
            teamACode = TeamCatalog.shortCodeFor(teamA),
            teamBCode = TeamCatalog.shortCodeFor(teamB),
            teamAScore = scoreA,
            teamBScore = scoreB,
            resultSummary = dto.status?.takeIf { status != MatchStatus.UPCOMING },
            venue = venueName,
            city = cityName,
            startTimeUtcIso = MatchTimeFormatter.formatEpochMsToUtcIso(epochMs),
            startTimeEpochMs = epochMs,
            lastUpdatedMs = nowMs
        )
    }

    private fun formatApiScore(runs: Int?, wickets: Int?, overs: Double?): String? {
        if (runs == null) return null
        val wkt = wickets?.let { "/$it" } ?: ""
        val ov = overs?.let { " ($it ov)" } ?: ""
        return "$runs$wkt$ov"
    }

    suspend fun toggleMatchReminder(matchId: String): Boolean = withContext(Dispatchers.IO) {
        val match = matchDao.getMatchById(matchId) ?: return@withContext false
        val prefs = preferencesRepository.getPreferencesOnce()
        val existing = reminderDao.getReminderByMatchId(matchId)
        val newEnabled = !(existing?.enabled ?: false)

        val updated = MatchReminderEntity(
            matchId = matchId,
            enabled = newEnabled,
            notify12h = existing?.notify12h ?: prefs.globalNotify12h,
            notify2h = existing?.notify2h ?: prefs.globalNotify2h,
            notify5m = existing?.notify5m ?: prefs.globalNotify5m,
            updatedAtMs = System.currentTimeMillis()
        )
        reminderDao.upsertReminder(updated)
        alarmScheduler.syncAlarmsForMatch(match, updated, prefs)
        newEnabled
    }

    suspend fun updateMatchReminderStage(
        matchId: String,
        stage: ReminderStage,
        enabled: Boolean
    ) = withContext(Dispatchers.IO) {
        val match = matchDao.getMatchById(matchId) ?: return@withContext
        val prefs = preferencesRepository.getPreferencesOnce()
        val existing = reminderDao.getReminderByMatchId(matchId) ?: MatchReminderEntity(
            matchId = matchId,
            enabled = true,
            notify12h = prefs.globalNotify12h,
            notify2h = prefs.globalNotify2h,
            notify5m = prefs.globalNotify5m,
            updatedAtMs = System.currentTimeMillis()
        )

        val next12h = if (stage == ReminderStage.HOURS_12) enabled else existing.notify12h
        val next2h = if (stage == ReminderStage.HOURS_2) enabled else existing.notify2h
        val next5m = if (stage == ReminderStage.MINUTES_5) enabled else existing.notify5m
        val masterEnabled = if (enabled) true else (next12h || next2h || next5m)

        val updated = existing.copy(
            enabled = masterEnabled,
            notify12h = next12h,
            notify2h = next2h,
            notify5m = next5m,
            updatedAtMs = System.currentTimeMillis()
        )
        reminderDao.upsertReminder(updated)
        alarmScheduler.syncAlarmsForMatch(match, updated, prefs)
    }

    suspend fun toggleFavoriteTeam(teamName: String) = withContext(Dispatchers.IO) {
        preferencesRepository.toggleFavoriteTeam(teamName)
        val prefs = preferencesRepository.getPreferencesOnce()
        if (prefs.autoNotifyFavorites) {
            applyAutoNotifyFavoritesInternal(prefs)
        }
    }

    suspend fun setAutoNotifyFavorites(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferencesRepository.setAutoNotifyFavorites(enabled)
        if (enabled) {
            val prefs = preferencesRepository.getPreferencesOnce()
            applyAutoNotifyFavoritesInternal(prefs)
        }
    }

    suspend fun updateGlobalIntervals(notify12h: Boolean, notify2h: Boolean, notify5m: Boolean) =
        withContext(Dispatchers.IO) {
            preferencesRepository.setGlobalIntervals(notify12h, notify2h, notify5m)
            rescheduleAllEnabledAlarms()
        }

    suspend fun rescheduleAllEnabledAlarms() = withContext(Dispatchers.IO) {
        val prefs = preferencesRepository.getPreferencesOnce()
        val matchesById = matchDao.getAllMatchesOnce().associateBy { it.id }
        val enabledReminders = reminderDao.getEnabledRemindersOnce()
        val nowMs = System.currentTimeMillis()
        enabledReminders.forEach { reminder ->
            val match = matchesById[reminder.matchId]
            if (match != null) {
                alarmScheduler.syncAlarmsForMatch(match, reminder, prefs, nowMs)
            }
        }
    }

    private suspend fun applyAutoNotifyFavoritesInternal(prefs: UserPreferences) {
        if (prefs.favoriteTeams.isEmpty()) return
        val nowMs = System.currentTimeMillis()
        val upcomingMatches = matchDao.getAllMatchesOnce().filter { entity ->
            MatchStatus.fromCode(entity.statusCode) == MatchStatus.UPCOMING &&
                entity.startTimeEpochMs > nowMs &&
                prefs.favoriteTeams.any { fav ->
                    entity.teamA.equals(fav, ignoreCase = true) ||
                        entity.teamB.equals(fav, ignoreCase = true)
                }
        }

        val toUpsert = mutableListOf<MatchReminderEntity>()
        upcomingMatches.forEach { entity ->
            val existing = reminderDao.getReminderByMatchId(entity.id)
            if (existing == null || !existing.enabled) {
                val reminder = MatchReminderEntity(
                    matchId = entity.id,
                    enabled = true,
                    notify12h = existing?.notify12h ?: prefs.globalNotify12h,
                    notify2h = existing?.notify2h ?: prefs.globalNotify2h,
                    notify5m = existing?.notify5m ?: prefs.globalNotify5m,
                    updatedAtMs = nowMs
                )
                toUpsert.add(reminder)
                alarmScheduler.syncAlarmsForMatch(entity, reminder, prefs, nowMs)
            }
        }
        if (toUpsert.isNotEmpty()) {
            reminderDao.upsertReminders(toUpsert)
        }
    }

    private fun MatchEntity.toDomain(reminder: MatchReminder): CricketMatch = CricketMatch(
        id = id,
        seriesName = seriesName,
        matchNumber = matchNumber,
        format = MatchFormat.fromCode(formatCode),
        status = MatchStatus.fromCode(statusCode),
        teamA = teamA,
        teamB = teamB,
        teamACode = teamACode,
        teamBCode = teamBCode,
        teamAScore = teamAScore,
        teamBScore = teamBScore,
        resultSummary = resultSummary,
        venue = venue,
        city = city,
        startTimeUtcIso = startTimeUtcIso,
        startTimeEpochMs = startTimeEpochMs,
        reminder = reminder
    )
}
