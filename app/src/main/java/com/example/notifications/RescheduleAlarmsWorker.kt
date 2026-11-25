package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.CricPulseDatabase
import com.example.data.preferences.UserPreferencesRepository

class RescheduleAlarmsWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = CricPulseDatabase.getInstance(applicationContext)
            val prefsRepo = UserPreferencesRepository(applicationContext)
            val scheduler = MatchAlarmScheduler(applicationContext)

            val prefs = prefsRepo.getPreferencesOnce()
            val matchesById = database.matchDao().getAllMatchesOnce().associateBy { it.id }
            val enabledReminders = database.matchReminderDao().getEnabledRemindersOnce()
            val nowMs = System.currentTimeMillis()

            enabledReminders.forEach { reminder ->
                val match = matchesById[reminder.matchId]
                if (match != null) {
                    scheduler.syncAlarmsForMatch(
                        match = match,
                        reminder = reminder,
                        prefs = prefs,
                        nowMs = nowMs
                    )
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
