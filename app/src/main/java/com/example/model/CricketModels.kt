package com.example.model

import com.example.ui.theme.ThemeMode

enum class MatchFormat(val label: String, val badgeText: String) {
    ALL("All", "ALL"),
    TEST("Test", "TEST"),
    ODI("ODI", "ODI"),
    T20I("T20I", "T20I");

    companion object {
        fun fromCode(raw: String?): MatchFormat = when (raw?.trim()?.uppercase()) {
            "TEST", "TESTS" -> TEST
            "ODI", "ODIS" -> ODI
            "T20", "T20I", "T20IS" -> T20I
            else -> ODI
        }
    }
}

enum class MatchStatus(val label: String) {
    UPCOMING("Upcoming"),
    LIVE("Live"),
    COMPLETED("Completed");

    companion object {
        fun fromCode(raw: String?): MatchStatus = when (raw?.trim()?.uppercase()) {
            "LIVE" -> LIVE
            "COMPLETED", "FINISHED", "RESULT" -> COMPLETED
            else -> UPCOMING
        }
    }
}

enum class ReminderStage(
    val code: Int,
    val offsetMillis: Long,
    val shortLabel: String,
    val fullLabel: String
) {
    HOURS_12(
        code = 1,
        offsetMillis = 12L * 60L * 60L * 1000L,
        shortLabel = "12h",
        fullLabel = "12 Hours Before"
    ),
    HOURS_2(
        code = 2,
        offsetMillis = 2L * 60L * 60L * 1000L,
        shortLabel = "2h",
        fullLabel = "2 Hours Before"
    ),
    MINUTES_5(
        code = 3,
        offsetMillis = 5L * 60L * 1000L,
        shortLabel = "5m",
        fullLabel = "5 Minutes Before"
    );

    fun buildNotificationTitle(teamA: String, teamB: String): String = when (this) {
        HOURS_12 -> "Match Today • $teamA vs $teamB"
        HOURS_2 -> "Get Ready • Toss Coming Up"
        MINUTES_5 -> "Starting Now • First Ball in 5m"
    }

    fun buildNotificationBody(
        teamA: String,
        teamB: String,
        formatBadge: String,
        localTime: String,
        venue: String
    ): String = when (this) {
        HOURS_12 -> "Match Today: $teamA vs $teamB ($formatBadge) starts in 12 hours at $localTime."
        HOURS_2 -> "Get Ready: $teamA vs $teamB toss coming up soon! Starts in 2 hours."
        MINUTES_5 -> "Starting Now: $teamA vs $teamB begins in 5 minutes at $venue!"
    }
}

data class CricketTeamInfo(
    val name: String,
    val shortCode: String,
    val flagEmoji: String
)

object TeamCatalog {
    val featuredTeams: List<CricketTeamInfo> = listOf(
        CricketTeamInfo("Bangladesh", "BAN", "\uD83C\uDDE7\uD83C\uDDE9"),
        CricketTeamInfo("India", "IND", "\uD83C\uDDEE\uD83C\uDDF3"),
        CricketTeamInfo("Australia", "AUS", "\uD83C\uDDE6\uD83C\uDDFA"),
        CricketTeamInfo("England", "ENG", "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F"),
        CricketTeamInfo("Pakistan", "PAK", "\uD83C\uDDF5\uD83C\uDDF0"),
        CricketTeamInfo("South Africa", "SA", "\uD83C\uDDFF\uD83C\uDDE6"),
        CricketTeamInfo("New Zealand", "NZ", "\uD83C\uDDF3\uD83C\uDDFF"),
        CricketTeamInfo("Sri Lanka", "SL", "\uD83C\uDDF1\uD83C\uDDF0"),
        CricketTeamInfo("West Indies", "WI", "\uD83C\uDF34"),
        CricketTeamInfo("Afghanistan", "AFG", "\uD83C\uDDE6\uD83C\uDDEB"),
        CricketTeamInfo("Ireland", "IRE", "\uD83C\uDDEE\uD83C\uDDEA"),
        CricketTeamInfo("Zimbabwe", "ZIM", "\uD83C\uDDFF\uD83C\uDDFC")
    )

    private val byNormalizedName = featuredTeams.associateBy { it.name.lowercase() }

    fun flagFor(teamName: String): String {
        val lower = teamName.trim().lowercase()
        byNormalizedName[lower]?.let { return it.flagEmoji }
        featuredTeams.firstOrNull { lower.contains(it.name.lowercase()) }?.let { return it.flagEmoji }
        return "\uD83C\uDFCF"
    }

    fun shortCodeFor(teamName: String): String {
        val lower = teamName.trim().lowercase()
        byNormalizedName[lower]?.let { return it.shortCode }
        featuredTeams.firstOrNull { lower.contains(it.name.lowercase()) }?.let { return it.shortCode }
        return teamName.take(3).uppercase()
    }
}

data class MatchReminder(
    val matchId: String,
    val enabled: Boolean = false,
    val notify12h: Boolean = true,
    val notify2h: Boolean = true,
    val notify5m: Boolean = true
) {
    fun activeCount(prefs: UserPreferences, startTimeEpochMs: Long, nowMs: Long): Int {
        if (!enabled) return 0
        var count = 0
        if (notify12h && prefs.globalNotify12h && startTimeEpochMs - ReminderStage.HOURS_12.offsetMillis > nowMs) count++
        if (notify2h && prefs.globalNotify2h && startTimeEpochMs - ReminderStage.HOURS_2.offsetMillis > nowMs) count++
        if (notify5m && prefs.globalNotify5m && startTimeEpochMs - ReminderStage.MINUTES_5.offsetMillis > nowMs) count++
        return count
    }
}

data class CricketMatch(
    val id: String,
    val seriesName: String,
    val matchNumber: String,
    val format: MatchFormat,
    val status: MatchStatus,
    val teamA: String,
    val teamB: String,
    val teamACode: String,
    val teamBCode: String,
    val teamAScore: String?,
    val teamBScore: String?,
    val resultSummary: String?,
    val venue: String,
    val city: String,
    val startTimeUtcIso: String,
    val startTimeEpochMs: Long,
    val reminder: MatchReminder
) {
    val teamAFlag: String get() = TeamCatalog.flagFor(teamA)
    val teamBFlag: String get() = TeamCatalog.flagFor(teamB)
    val fullVenue: String get() = if (city.isNotBlank() && !venue.contains(city, ignoreCase = true)) "$venue, $city" else venue
    val matchupTitle: String get() = "$teamA vs $teamB"

    fun involvesTeam(teamQuery: String): Boolean {
        val q = teamQuery.trim()
        if (q.isEmpty()) return true
        return teamA.contains(q, ignoreCase = true) ||
            teamB.contains(q, ignoreCase = true) ||
            teamACode.equals(q, ignoreCase = true) ||
            teamBCode.equals(q, ignoreCase = true)
    }

    fun matchesSearch(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return teamA.contains(q, ignoreCase = true) ||
            teamB.contains(q, ignoreCase = true) ||
            seriesName.contains(q, ignoreCase = true) ||
            venue.contains(q, ignoreCase = true) ||
            city.contains(q, ignoreCase = true) ||
            format.label.contains(q, ignoreCase = true)
    }
}

data class UserPreferences(
    val favoriteTeams: Set<String> = emptySet(),
    val autoNotifyFavorites: Boolean = false,
    val globalNotify12h: Boolean = true,
    val globalNotify2h: Boolean = true,
    val globalNotify5m: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorEnabled: Boolean = false,
    val customApiKey: String = "",
    val apiBaseUrl: String = DEFAULT_CRIC_API_BASE_URL
) {
    companion object {
        const val DEFAULT_CRIC_API_BASE_URL = "https://api.cricapi.com/v1/"
    }
}
