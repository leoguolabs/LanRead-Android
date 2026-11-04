package com.lanread.app.ui.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lanread.app.MainActivity
import com.lanread.app.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val goalMinutes = intent.getIntExtra("goal_minutes", 20)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "reading_reminder",
            context.getString(R.string.reading_reminder_daily),
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("isla-reader://read/last")
        }
        val tapPending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "reading_reminder")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.reading_reminder_title))
            .setContentText(context.getString(R.string.reading_reminder_body, goalMinutes))
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()
        manager.notify(ReadingReminderService.NOTIFICATION_ID, notification)

        // Reschedule for tomorrow
        val prefs = context.getSharedPreferences("lanread_settings", Context.MODE_PRIVATE)
        val minutes = prefs.getInt("readingReminderTimeMinutes", 20 * 60)
        ReadingReminderService.schedule(context, minutes / 60, minutes % 60, goalMinutes)
    }
}
