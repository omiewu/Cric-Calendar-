package com.example.data.mock

import com.example.data.local.MatchEntity
import com.example.model.MatchFormat
import com.example.model.MatchStatus
import com.example.model.TeamCatalog
import com.example.util.MatchTimeFormatter
import java.util.Calendar
import java.util.TimeZone

/**
 * Rich built-in international cricket fixture schedule spanning the full current calendar year
 * across Test, ODI, and T20I formats. Live matches and near-term upcoming fixtures are anchored
 * cleanly relative to the current clock so live scores, 24h countdowns, and 12h/2h/5m alerts
 * work immediately out of the box without requiring a live API key.
 */
object MockCricketDataset {

    private const val MINUTE_MS = 60L * 1000L
    private const val HOUR_MS = 60L * MINUTE_MS
    private const val DAY_MS = 24L * HOUR_MS

    fun generateCurrentYearFixtures(nowMs: Long = System.currentTimeMillis()): List<MatchEntity> {
        val year = MatchTimeFormatter.currentCalendarYear()

        fun utcDate(month1Based: Int, day: Int, hourUtc: Int, minuteUtc: Int): Long {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month1Based - 1)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hourUtc)
                set(Calendar.MINUTE, minuteUtc)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun relative(offsetMs: Long): Long = nowMs + offsetMs

        fun match(
            id: String,
            series: String,
            matchNumber: String,
            format: MatchFormat,
            status: MatchStatus,
            teamA: String,
            teamB: String,
            venue: String,
            city: String,
            epochMs: Long,
            teamAScore: String? = null,
            teamBScore: String? = null,
            resultSummary: String? = null
        ): MatchEntity = MatchEntity(
            id = id,
            seriesName = series,
            matchNumber = matchNumber,
            formatCode = format.name,
            statusCode = status.name,
            teamA = teamA,
            teamB = teamB,
            teamACode = TeamCatalog.shortCodeFor(teamA),
            teamBCode = TeamCatalog.shortCodeFor(teamB),
            teamAScore = teamAScore,
            teamBScore = teamBScore,
            resultSummary = resultSummary,
            venue = venue,
            city = city,
            startTimeUtcIso = MatchTimeFormatter.formatEpochMsToUtcIso(epochMs),
            startTimeEpochMs = epochMs,
            lastUpdatedMs = nowMs
        )

        return listOf(
            // =========================================================================
            // LIVE INTERNATIONAL MATCHES (3 across Test, ODI, T20I)
            // =========================================================================
            match(
                id = "live_ind_aus_test_1",
                series = "Border-Gavaskar Trophy $year",
                matchNumber = "1st Test • Day 3",
                format = MatchFormat.TEST,
                status = MatchStatus.LIVE,
                teamA = "Australia",
                teamB = "India",
                venue = "Optus Stadium",
                city = "Perth",
                epochMs = relative(-3 * HOUR_MS),
                teamAScore = "312 & 148/4 (44.2 ov)",
                teamBScore = "289",
                resultSummary = "Day 3: Session 2 • Australia lead by 171 runs"
            ),
            match(
                id = "live_ban_nz_odi_2",
                series = "Bangladesh Tour of New Zealand $year",
                matchNumber = "2nd ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.LIVE,
                teamA = "New Zealand",
                teamB = "Bangladesh",
                venue = "Hagley Oval",
                city = "Christchurch",
                epochMs = relative(-2 * HOUR_MS - 15 * MINUTE_MS),
                teamAScore = "274/8 (50.0 ov)",
                teamBScore = "198/4 (36.4 ov)",
                resultSummary = "Bangladesh need 77 runs from 80 balls"
            ),
            match(
                id = "live_eng_wi_t20_3",
                series = "England Tour of West Indies $year",
                matchNumber = "3rd T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.LIVE,
                teamA = "West Indies",
                teamB = "England",
                venue = "Kensington Oval",
                city = "Bridgetown",
                epochMs = relative(-55 * MINUTE_MS),
                teamAScore = "186/6 (20.0 ov)",
                teamBScore = "104/2 (11.3 ov)",
                resultSummary = "England need 83 runs in 51 balls • RR 9.04"
            ),

            // =========================================================================
            // UPCOMING WITHIN 24 HOURS (Live countdowns + 12h / 2h / 5m alerts!)
            // =========================================================================
            match(
                id = "up_pak_sa_t20_1",
                series = "South Africa Tour of Pakistan $year",
                matchNumber = "1st T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.UPCOMING,
                teamA = "Pakistan",
                teamB = "South Africa",
                venue = "Gaddafi Stadium",
                city = "Lahore",
                epochMs = relative(1 * HOUR_MS + 42 * MINUTE_MS)
            ),
            match(
                id = "up_sl_afg_odi_1",
                series = "Afghanistan Tour of Sri Lanka $year",
                matchNumber = "1st ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.UPCOMING,
                teamA = "Sri Lanka",
                teamB = "Afghanistan",
                venue = "R. Premadasa Stadium",
                city = "Colombo",
                epochMs = relative(4 * HOUR_MS + 20 * MINUTE_MS)
            ),
            match(
                id = "up_ban_ind_t20_1",
                series = "India Tour of Bangladesh $year",
                matchNumber = "1st T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.UPCOMING,
                teamA = "Bangladesh",
                teamB = "India",
                venue = "Sher-e-Bangla National Cricket Stadium",
                city = "Dhaka",
                epochMs = relative(13 * HOUR_MS + 30 * MINUTE_MS)
            ),
            match(
                id = "up_sa_eng_test_1",
                series = "Basil D'Oliveira Trophy $year",
                matchNumber = "1st Test",
                format = MatchFormat.TEST,
                status = MatchStatus.UPCOMING,
                teamA = "South Africa",
                teamB = "England",
                venue = "Newlands Cricket Ground",
                city = "Cape Town",
                epochMs = relative(18 * HOUR_MS + 15 * MINUTE_MS)
            ),
            match(
                id = "up_aus_nz_odi_1",
                series = "Chappell-Hadlee Trophy $year",
                matchNumber = "1st ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.UPCOMING,
                teamA = "Australia",
                teamB = "New Zealand",
                venue = "Melbourne Cricket Ground",
                city = "Melbourne",
                epochMs = relative(22 * HOUR_MS + 45 * MINUTE_MS)
            ),

            // =========================================================================
            // UPCOMING SCHEDULE (Rest of Month & Calendar Year across Test/ODI/T20I)
            // =========================================================================
            match(
                id = "up_ban_ind_t20_2",
                series = "India Tour of Bangladesh $year",
                matchNumber = "2nd T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.UPCOMING,
                teamA = "Bangladesh",
                teamB = "India",
                venue = "Zahur Ahmed Chowdhury Stadium",
                city = "Chattogram",
                epochMs = relative(2 * DAY_MS + 6 * HOUR_MS)
            ),
            match(
                id = "up_pak_sa_t20_2",
                series = "South Africa Tour of Pakistan $year",
                matchNumber = "2nd T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.UPCOMING,
                teamA = "Pakistan",
                teamB = "South Africa",
                venue = "Rawalpindi Cricket Stadium",
                city = "Rawalpindi",
                epochMs = relative(3 * DAY_MS + 4 * HOUR_MS)
            ),
            match(
                id = "up_ind_aus_test_2",
                series = "Border-Gavaskar Trophy $year",
                matchNumber = "2nd Test (Day-Night)",
                format = MatchFormat.TEST,
                status = MatchStatus.UPCOMING,
                teamA = "Australia",
                teamB = "India",
                venue = "Adelaide Oval",
                city = "Adelaide",
                epochMs = relative(5 * DAY_MS + 1 * HOUR_MS)
            ),
            match(
                id = "up_sl_afg_odi_2",
                series = "Afghanistan Tour of Sri Lanka $year",
                matchNumber = "2nd ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.UPCOMING,
                teamA = "Sri Lanka",
                teamB = "Afghanistan",
                venue = "Pallekele International Cricket Stadium",
                city = "Kandy",
                epochMs = relative(6 * DAY_MS + 3 * HOUR_MS)
            ),
            match(
                id = "up_ban_ind_odi_1",
                series = "India Tour of Bangladesh $year",
                matchNumber = "1st ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.UPCOMING,
                teamA = "Bangladesh",
                teamB = "India",
                venue = "Sher-e-Bangla National Cricket Stadium",
                city = "Dhaka",
                epochMs = relative(8 * DAY_MS + 2 * HOUR_MS)
            ),
            match(
                id = "up_wi_pak_test_1",
                series = "Pakistan Tour of West Indies $year",
                matchNumber = "1st Test",
                format = MatchFormat.TEST,
                status = MatchStatus.UPCOMING,
                teamA = "West Indies",
                teamB = "Pakistan",
                venue = "Sabina Park",
                city = "Kingston",
                epochMs = relative(10 * DAY_MS + 8 * HOUR_MS)
            ),
            match(
                id = "up_nz_eng_t20_1",
                series = "England Tour of New Zealand $year",
                matchNumber = "1st T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.UPCOMING,
                teamA = "New Zealand",
                teamB = "England",
                venue = "Eden Park",
                city = "Auckland",
                epochMs = relative(12 * DAY_MS + 5 * HOUR_MS)
            ),
            match(
                id = "up_afg_ire_odi_1",
                series = "Afghanistan vs Ireland Series $year",
                matchNumber = "1st ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.UPCOMING,
                teamA = "Afghanistan",
                teamB = "Ireland",
                venue = "Sharjah Cricket Stadium",
                city = "Sharjah",
                epochMs = relative(14 * DAY_MS + 4 * HOUR_MS)
            ),
            match(
                id = "up_sa_sl_test_1",
                series = "ICC World Test Championship $year",
                matchNumber = "1st Test",
                format = MatchFormat.TEST,
                status = MatchStatus.UPCOMING,
                teamA = "South Africa",
                teamB = "Sri Lanka",
                venue = "SuperSport Park",
                city = "Centurion",
                epochMs = relative(17 * DAY_MS + 1 * HOUR_MS)
            ),
            match(
                id = "up_ind_aus_test_3",
                series = "Border-Gavaskar Trophy $year",
                matchNumber = "3rd Test",
                format = MatchFormat.TEST,
                status = MatchStatus.UPCOMING,
                teamA = "Australia",
                teamB = "India",
                venue = "The Gabba",
                city = "Brisbane",
                epochMs = relative(21 * DAY_MS)
            ),
            match(
                id = "up_ban_zim_test_1",
                series = "Zimbabwe Tour of Bangladesh $year",
                matchNumber = "1st Test",
                format = MatchFormat.TEST,
                status = MatchStatus.UPCOMING,
                teamA = "Bangladesh",
                teamB = "Zimbabwe",
                venue = "Sylhet International Cricket Stadium",
                city = "Sylhet",
                epochMs = relative(24 * DAY_MS + 2 * HOUR_MS)
            ),
            match(
                id = "up_aus_ind_boxing_day",
                series = "Border-Gavaskar Trophy $year",
                matchNumber = "4th Test (Boxing Day)",
                format = MatchFormat.TEST,
                status = MatchStatus.UPCOMING,
                teamA = "Australia",
                teamB = "India",
                venue = "Melbourne Cricket Ground",
                city = "Melbourne",
                epochMs = maxOf(utcDate(12, 25, 23, 30), relative(26 * DAY_MS))
            ),
            match(
                id = "up_sa_pak_newyear_odi",
                series = "Pakistan Tour of South Africa $year",
                matchNumber = "3rd ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.UPCOMING,
                teamA = "South Africa",
                teamB = "Pakistan",
                venue = "Wanderers Stadium",
                city = "Johannesburg",
                epochMs = maxOf(utcDate(12, 29, 8, 0), relative(30 * DAY_MS))
            ),
            match(
                id = "up_eng_wi_t20_finale",
                series = "New Year T20I Tri-Series $year",
                matchNumber = "Final T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.UPCOMING,
                teamA = "England",
                teamB = "Australia",
                venue = "Lord's Cricket Ground",
                city = "London",
                epochMs = maxOf(utcDate(12, 30, 17, 30), relative(33 * DAY_MS))
            ),

            // =========================================================================
            // COMPLETED INTERNATIONAL FIXTURES FROM EARLIER IN THE CURRENT YEAR
            // =========================================================================
            match(
                id = "comp_ban_nz_odi_1",
                series = "Bangladesh Tour of New Zealand $year",
                matchNumber = "1st ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.COMPLETED,
                teamA = "New Zealand",
                teamB = "Bangladesh",
                venue = "Basin Reserve",
                city = "Wellington",
                epochMs = relative(-3 * DAY_MS),
                teamAScore = "259/9 (50.0 ov)",
                teamBScore = "262/6 (48.3 ov)",
                resultSummary = "Bangladesh won by 4 wickets"
            ),
            match(
                id = "comp_ind_eng_t20_5",
                series = "England Tour of India $year",
                matchNumber = "5th T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.COMPLETED,
                teamA = "India",
                teamB = "England",
                venue = "Wankhede Stadium",
                city = "Mumbai",
                epochMs = relative(-7 * DAY_MS),
                teamAScore = "214/5 (20.0 ov)",
                teamBScore = "197/9 (20.0 ov)",
                resultSummary = "India won by 17 runs (Series 3-2)"
            ),
            match(
                id = "comp_pak_aus_odi_3",
                series = "Pakistan Tour of Australia $year",
                matchNumber = "3rd ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.COMPLETED,
                teamA = "Australia",
                teamB = "Pakistan",
                venue = "Sydney Cricket Ground",
                city = "Sydney",
                epochMs = relative(-14 * DAY_MS),
                teamAScore = "242 (47.4 ov)",
                teamBScore = "245/4 (43.2 ov)",
                resultSummary = "Pakistan won by 6 wickets (Series 2-1)"
            ),
            match(
                id = "comp_sl_ban_test_2",
                series = "ICC World Test Championship $year",
                matchNumber = "2nd Test",
                format = MatchFormat.TEST,
                status = MatchStatus.COMPLETED,
                teamA = "Sri Lanka",
                teamB = "Bangladesh",
                venue = "Galle International Stadium",
                city = "Galle",
                epochMs = relative(-24 * DAY_MS),
                teamAScore = "368 & 211",
                teamBScore = "304 & 278/7",
                resultSummary = "Bangladesh won by 3 wickets"
            ),
            match(
                id = "comp_afg_sa_odi_2",
                series = "South Africa vs Afghanistan ODI Series $year",
                matchNumber = "2nd ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.COMPLETED,
                teamA = "Afghanistan",
                teamB = "South Africa",
                venue = "Sharjah Cricket Stadium",
                city = "Sharjah",
                epochMs = relative(-35 * DAY_MS),
                teamAScore = "289/6 (50.0 ov)",
                teamBScore = "244 (46.5 ov)",
                resultSummary = "Afghanistan won by 45 runs"
            ),
            match(
                id = "comp_eng_aus_ashes_test",
                series = "The Ashes Test Series $year",
                matchNumber = "5th Test",
                format = MatchFormat.TEST,
                status = MatchStatus.COMPLETED,
                teamA = "England",
                teamB = "Australia",
                venue = "The Oval",
                city = "London",
                epochMs = utcDate(8, 12, 10, 0),
                teamAScore = "342 & 298",
                teamBScore = "315 & 276",
                resultSummary = "England won by 49 runs"
            ),
            match(
                id = "comp_ind_pak_asia_t20",
                series = "Asia Cup T20I $year",
                matchNumber = "Final",
                format = MatchFormat.T20I,
                status = MatchStatus.COMPLETED,
                teamA = "India",
                teamB = "Pakistan",
                venue = "Dubai International Cricket Stadium",
                city = "Dubai",
                epochMs = utcDate(9, 18, 14, 0),
                teamAScore = "178/6 (20.0 ov)",
                teamBScore = "172/8 (20.0 ov)",
                resultSummary = "India won by 6 runs"
            ),
            match(
                id = "comp_wi_sa_t20_2",
                series = "South Africa Tour of West Indies $year",
                matchNumber = "2nd T20I",
                format = MatchFormat.T20I,
                status = MatchStatus.COMPLETED,
                teamA = "West Indies",
                teamB = "South Africa",
                venue = "Brian Lara Cricket Academy",
                city = "Tarouba",
                epochMs = utcDate(6, 22, 19, 0),
                teamAScore = "205/4 (20.0 ov)",
                teamBScore = "188/7 (20.0 ov)",
                resultSummary = "West Indies won by 17 runs"
            ),
            match(
                id = "comp_nz_ind_test_1",
                series = "New Zealand Tour of India $year",
                matchNumber = "1st Test",
                format = MatchFormat.TEST,
                status = MatchStatus.COMPLETED,
                teamA = "India",
                teamB = "New Zealand",
                venue = "Eden Gardens",
                city = "Kolkata",
                epochMs = utcDate(3, 14, 4, 0),
                teamAScore = "412 & 220/4d",
                teamBScore = "325 & 218",
                resultSummary = "India won by 89 runs"
            ),
            match(
                id = "comp_ban_pak_odi_3",
                series = "Bangladesh Tour of Pakistan $year",
                matchNumber = "3rd ODI",
                format = MatchFormat.ODI,
                status = MatchStatus.COMPLETED,
                teamA = "Pakistan",
                teamB = "Bangladesh",
                venue = "National Bank Cricket Arena",
                city = "Karachi",
                epochMs = utcDate(2, 19, 9, 30),
                teamAScore = "284/7 (50.0 ov)",
                teamBScore = "271/9 (50.0 ov)",
                resultSummary = "Pakistan won by 13 runs"
            )
        )
    }
}
