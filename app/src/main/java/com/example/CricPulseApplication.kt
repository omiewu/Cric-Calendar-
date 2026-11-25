package com.example

import android.app.Application
import com.example.data.local.CricPulseDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.MatchRepository
import com.example.notifications.MatchAlarmScheduler
import com.example.notifications.NotificationHelper

class CricPulseApplication : Application() {

    val database: CricPulseDatabase by lazy {
        CricPulseDatabase.getInstance(this)
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val alarmScheduler: MatchAlarmScheduler by lazy {
        MatchAlarmScheduler(this)
    }

    val matchRepository: MatchRepository by lazy {
        MatchRepository(
            matchDao = database.matchDao(),
            reminderDao = database.matchReminderDao(),
            preferencesRepository = preferencesRepository,
            alarmScheduler = alarmScheduler
        )
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        MatchAlarmScheduler.ensurePeriodicSafeguard(this)
    }
}
