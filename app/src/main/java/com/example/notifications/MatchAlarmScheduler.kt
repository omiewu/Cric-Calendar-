package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.MatchEntity
import com.example.data.local.MatchReminderEntity
import com.example.model.MatchFormat
import com.example.model.MatchStatus
import com.example.model.ReminderStage
import com.example.model.UserPreferences
import java.util.concurrent.TimeUnit

class MatchAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun syncAlarmsForMatch(
        match: MatchEntity,
        reminder: MatchReminderEntity,
        prefs: UserPreferences,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val isUpcoming = MatchStatus.fromCode(match.statusCode) == MatchStatus.UPCOMING &&
            match.startTimeEpochMs > nowMs

        if (!reminder.enabled || !isUpcoming) {
            cancelAllStagesForMatch(match.id)
            return
        }

        val stageStates = mapOf(
            ReminderStage.HOURS_12 to (reminder.notify12h && prefs.globalNotify12h),
            ReminderStage.HOURS_2 to (reminder.notify2h && prefs.globalNotify2h),
            ReminderStage.MINUTES_5 to (reminder.notify5m && prefs.globalNotify5m)
        )

        stageStates.forEach { (stage, shouldSchedule) ->
            val triggerAtMs = match.startTimeEpochMs - stage.offsetMillis
            if (shouldSchedule && triggerAtMs > nowMs + 5_000L) {
                scheduleSingleStage(match, stage, triggerAtMs)
            } else {
                cancelSingleStage(match.id, stage)
            }
        }
    }

    fun cancelAllStagesForMatch(matchId: String) {
        ReminderStage.entries.forEach { stage ->
            cancelSingleStage(matchId, stage)
        }
    }

    private fun scheduleSingleStage(
        match: MatchEntity,
        stage: ReminderStage,
        triggerAtMs: Long
    ) {
        val pendingIntent = buildAlarmPendingIntent(
            matchId = match.id,
            stage = stage,
            teamA = match.teamA,
            teamB = match.teamB,
            formatBadge = MatchFormat.fromCode(match.formatCode).badgeText,
            venue = match.venue,
            startTimeEpochMs = match.startTimeEpochMs
        )

        val manager = alarmManager
        val canExact = NotificationHelper.canScheduleExactAlarms(context)
        try {
            if (canExact) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }
    }

    private fun cancelSingleStage(matchId: String, stage: ReminderStage) {
        val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
            action = ACTION_MATCH_REMINDER
        }
        val requestCode = requestCodeFor(matchId, stage)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildAlarmPendingIntent(
        matchId: String,
        stage: ReminderStage,
        teamA: String,
        teamB: String,
        formatBadge: String,
        venue: String,
        startTimeEpochMs: Long
    ): PendingIntent {
        val intent = Intent(context, MatchAlarmReceiver::class.java).apply {
            action = ACTION_MATCH_REMINDER
            putExtra(NotificationHelper.EXTRA_MATCH_ID, matchId)
            putExtra(NotificationHelper.EXTRA_STAGE_CODE, stage.code)
            putExtra(NotificationHelper.EXTRA_TEAM_A, teamA)
            putExtra(NotificationHelper.EXTRA_TEAM_B, teamB)
            putExtra(NotificationHelper.EXTRA_FORMAT, formatBadge)
            putExtra(NotificationHelper.EXTRA_VENUE, venue)
            putExtra(NotificationHelper.EXTRA_START_EPOCH_MS, startTimeEpochMs)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(matchId, stage),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_MATCH_REMINDER = "com.example.cricpulse.ACTION_MATCH_REMINDER"
        private const val UNIQUE_RESCHEDULE_WORK = "cricpulse_reschedule_alarms_once"
        private const val PERIODIC_SAFEGUARD_WORK = "cricpulse_reschedule_alarms_periodic"

        fun requestCodeFor(matchId: String, stage: ReminderStage): Int {
            val base = (matchId.hashCode() and 0x07FFFFFF)
            return base * 10 + stage.code
        }

        fun enqueueImmediateReschedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<RescheduleAlarmsWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_RESCHEDULE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun ensurePeriodicSafeguard(context: Context) {
            val periodicRequest = PeriodicWorkRequestBuilder<RescheduleAlarmsWorker>(
                12,
                TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_SAFEGUARD_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
