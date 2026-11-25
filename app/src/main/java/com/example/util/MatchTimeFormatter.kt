package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object MatchTimeFormatter {

    private val utcZone: TimeZone = TimeZone.getTimeZone("UTC")

    fun parseUtcIsoToEpochMs(isoString: String): Long? {
        val cleaned = isoString.trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = utcZone
                    isLenient = false
                }
                val parsed = sdf.parse(cleaned)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // Try next format
            }
        }
        return null
    }

    fun formatEpochMsToUtcIso(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = utcZone
        }
        return sdf.format(Date(epochMs))
    }

    fun formatLocalDateAndTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d • h:mm a z", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date(epochMs))
    }

    fun formatLocalTimeOnly(epochMs: Long): String {
        val sdf = SimpleDateFormat("h:mm a z", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date(epochMs))
    }

    fun formatLocalFullDateTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy • h:mm a z", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
        return sdf.format(Date(epochMs))
    }

    fun formatUtcFullDateTime(epochMs: Long): String {
        val sdf = SimpleDateFormat("MMM d, yyyy • HH:mm 'UTC'", Locale.US).apply {
            timeZone = utcZone
        }
        return sdf.format(Date(epochMs))
    }

    fun formatReminderTimestamp(triggerEpochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        val targetCal = Calendar.getInstance().apply { timeInMillis = triggerEpochMs }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val tomorrowCal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val timeStr = SimpleDateFormat("h:mm a z", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(triggerEpochMs))

        return when {
            isSameDay(targetCal, nowCal) -> "Today, $timeStr"
            isSameDay(targetCal, tomorrowCal) -> "Tomorrow, $timeStr"
            else -> {
                val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply {
                    timeZone = TimeZone.getDefault()
                }.format(Date(triggerEpochMs))
                "$dateStr • $timeStr"
            }
        }
    }

    fun formatCountdown(targetEpochMs: Long, nowMs: Long): String? {
        val diff = targetEpochMs - nowMs
        if (diff <= 0L || diff > 24L * 60L * 60L * 1000L) return null
        val totalSeconds = diff / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02dm %02ds", minutes, seconds)
        }
    }

    fun currentTimeZoneBadge(): String {
        val tz = TimeZone.getDefault()
        val shortName = tz.getDisplayName(tz.inDaylightTime(Date()), TimeZone.SHORT, Locale.getDefault())
        val offsetMs = tz.getOffset(System.currentTimeMillis())
        val sign = if (offsetMs >= 0) "+" else "-"
        val absMinutes = abs(offsetMs) / (60 * 1000)
        val hrs = absMinutes / 60
        val mins = absMinutes % 60
        val offsetFormatted = if (mins == 0) "UTC$sign$hrs" else String.format(Locale.US, "UTC%s%d:%02d", sign, hrs, mins)
        return "$shortName ($offsetFormatted)"
    }

    fun currentCalendarYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    private fun isSameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
