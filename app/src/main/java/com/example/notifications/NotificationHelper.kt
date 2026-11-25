package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.ReminderStage
import com.example.util.MatchTimeFormatter

object NotificationHelper {

    const val CHANNEL_ID_MATCH_ALERTS = "cricpulse_match_alerts"
    const val EXTRA_MATCH_ID = "extra_match_id"
    const val EXTRA_STAGE_CODE = "extra_stage_code"
    const val EXTRA_TEAM_A = "extra_team_a"
    const val EXTRA_TEAM_B = "extra_team_b"
    const val EXTRA_FORMAT = "extra_format"
    const val EXTRA_VENUE = "extra_venue"
    const val EXTRA_START_EPOCH_MS = "extra_start_epoch_ms"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID_MATCH_ALERTS,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun postMatchAlert(
        context: Context,
        matchId: String,
        stage: ReminderStage,
        teamA: String,
        teamB: String,
        formatBadge: String,
        venue: String,
        startTimeEpochMs: Long
    ): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        val localTime = MatchTimeFormatter.formatLocalTimeOnly(startTimeEpochMs)
        val title = stage.buildNotificationTitle(teamA, teamB)
        val body = stage.buildNotificationBody(
            teamA = teamA,
            teamB = teamB,
            formatBadge = formatBadge,
            localTime = localTime,
            venue = venue
        )

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_MATCH_ID, matchId)
        }

        val notificationId = ((matchId.hashCode() and 0x0FFFFFFF) * 10) + stage.code
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MATCH_ALERTS)
            .setSmallIcon(R.drawable.ic_notification_cricket)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun postTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        ensureChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            999001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sampleTime = MatchTimeFormatter.formatLocalTimeOnly(System.currentTimeMillis() + 5 * 60 * 1000L)
        val sampleBody = "Starting Now: Bangladesh vs India (T20I) begins in 5 minutes at Sher-e-Bangla National Cricket Stadium! ($sampleTime)"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MATCH_ALERTS)
            .setSmallIcon(R.drawable.ic_notification_cricket)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(sampleBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(sampleBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(999001, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
