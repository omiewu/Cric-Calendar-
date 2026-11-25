package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.model.ReminderStage

class MatchAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MatchAlarmScheduler.ACTION_MATCH_REMINDER) return

        val matchId = intent.getStringExtra(NotificationHelper.EXTRA_MATCH_ID) ?: return
        val stageCode = intent.getIntExtra(NotificationHelper.EXTRA_STAGE_CODE, ReminderStage.MINUTES_5.code)
        val stage = ReminderStage.entries.firstOrNull { it.code == stageCode } ?: ReminderStage.MINUTES_5
        val teamA = intent.getStringExtra(NotificationHelper.EXTRA_TEAM_A) ?: "Team A"
        val teamB = intent.getStringExtra(NotificationHelper.EXTRA_TEAM_B) ?: "Team B"
        val formatBadge = intent.getStringExtra(NotificationHelper.EXTRA_FORMAT) ?: "INTL"
        val venue = intent.getStringExtra(NotificationHelper.EXTRA_VENUE) ?: "International Venue"
        val startEpochMs = intent.getLongExtra(
            NotificationHelper.EXTRA_START_EPOCH_MS,
            System.currentTimeMillis() + stage.offsetMillis
        )

        NotificationHelper.postMatchAlert(
            context = context,
            matchId = matchId,
            stage = stage,
            teamA = teamA,
            teamB = teamB,
            formatBadge = formatBadge,
            venue = venue,
            startTimeEpochMs = startEpochMs
        )
    }
}
