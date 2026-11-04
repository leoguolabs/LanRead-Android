package com.lanread.app.di

import com.lanread.app.util.AISummaryService

/** Lazily-created shared AI summary service holder. */
class AISummaryServiceHolder {
    val service: AISummaryService by lazy { AISummaryService(isAiAllowed = { true }) }
}
