package com.lanread.app.data

import android.content.Context
import androidx.core.content.edit
import java.time.LocalDate
import java.time.ZoneId

/** Tracks today's accumulated reading seconds; resets at local midnight. */
class ReadingDailyStatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("reading_daily_stats", Context.MODE_PRIVATE)

    fun todayReadingSeconds(): Long {
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val storedStart = prefs.getLong("today_start_timestamp", 0L)
        if (storedStart != todayStart) {
            prefs.edit { putLong("today_start_timestamp", todayStart); putLong("today_reading_seconds", 0L) }
            return 0L
        }
        return prefs.getLong("today_reading_seconds", 0L)
    }

    fun addSeconds(seconds: Long) {
        if (seconds <= 0) return
        val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val storedStart = prefs.getLong("today_start_timestamp", 0L)
        if (storedStart != todayStart) {
            prefs.edit { putLong("today_start_timestamp", todayStart); putLong("today_reading_seconds", seconds) }
        } else {
            prefs.edit { putLong("today_reading_seconds", prefs.getLong("today_reading_seconds", 0L) + seconds) }
        }
    }

    fun clear() = prefs.edit { clear() }
}
