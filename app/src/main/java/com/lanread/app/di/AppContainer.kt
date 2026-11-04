package com.lanread.app.di

import android.content.Context
import com.lanread.app.data.BookImportService
import com.lanread.app.data.ReadingDailyStatsStore
import com.lanread.app.data.ReadingStatusService
import com.lanread.app.data.local.LanReadDatabase
import com.lanread.app.data.settings.AppSettingsStore
import com.lanread.app.util.AISummaryService

class AppContainer(private val context: Context) {
    val database: LanReadDatabase by lazy { LanReadDatabase.build(context) }
    val appSettings: AppSettingsStore by lazy { AppSettingsStore(context) }
    val bookImportService: BookImportService by lazy { BookImportService(context, database) }
    val readingStatusService: ReadingStatusService by lazy { ReadingStatusService(database) }
    val dailyStats: ReadingDailyStatsStore by lazy { ReadingDailyStatsStore(context) }
    val aiSummaryService: AISummaryService by lazy { AISummaryService(isAiAllowed = { true }) }
}
