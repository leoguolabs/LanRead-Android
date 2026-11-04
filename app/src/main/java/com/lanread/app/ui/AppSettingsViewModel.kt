package com.lanread.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.runtime.Composable
import com.lanread.app.data.settings.AppSettingsState
import com.lanread.app.data.settings.AppSettingsStore
import com.lanread.app.data.settings.AppLanguageOption
import com.lanread.app.di.AppContainer

/**
 * Holds app-wide settings state. Persists every change through AppSettingsStore,
 * mirroring the iOS AppSettings ObservableObject.
 */
class AppSettingsViewModel(
    private val store: AppSettingsStore,
    initial: AppSettingsState
) : ViewModel() {

    var state by mutableStateOf(initial)
        private set

    fun reload() {
        state = store.load()
    }

    fun setLanguage(value: AppLanguageOption) {
        state = state.copy(language = value)
        store.saveLanguage(value)
    }

    fun setTranslationLanguage(value: AppLanguageOption) {
        state = state.copy(translationLanguage = value)
        store.saveTranslationLanguage(value)
    }

    fun setTheme(value: com.lanread.app.data.settings.AppThemeMode) {
        state = state.copy(theme = value)
        store.saveTheme(value)
    }

    fun setReadingFontSize(value: com.lanread.app.data.settings.ReadingFontSizeOption) {
        state = state.copy(readingFontSize = value)
        store.saveReadingFontSize(value)
    }

    fun setReadingFont(value: com.lanread.app.data.settings.ReadingFontOption) {
        state = state.copy(readingFont = value)
        store.saveReadingFont(value)
    }

    fun setLineSpacing(value: Double) {
        state = state.copy(lineSpacing = value)
        store.saveLineSpacing(value)
    }

    fun setPageMargins(value: Double) {
        state = state.copy(pageMargins = value)
        store.savePageMargins(value)
    }

    fun setHighlightSortMode(value: com.lanread.app.data.settings.HighlightSortModeOption) {
        state = state.copy(highlightSortMode = value)
        store.saveHighlightSortMode(value)
    }

    fun setProfileDisplayName(value: String) {
        val clamped = value.take(24)
        state = state.copy(profileDisplayName = clamped)
        store.saveProfileDisplayName(clamped)
    }

    fun setProfileAvatar(data: ByteArray?) {
        state = state.copy(profileAvatarData = data)
        store.saveProfileAvatar(data)
    }

    fun setReadingReminderEnabled(value: Boolean) {
        state = state.copy(isReadingReminderEnabled = value)
        store.saveReadingReminderEnabled(value)
    }

    fun setDailyReadingGoal(value: Int) {
        state = state.copy(dailyReadingGoal = value)
        store.saveDailyReadingGoal(value)
    }

    fun setReadingReminderMinutes(value: Int) {
        state = state.copy(readingReminderMinutesSinceMidnight = value)
        store.saveReadingReminderMinutes(value)
    }

    fun recordAiConsent(granted: Boolean, suppress: Boolean) {
        state = state.copy(aiConsentGranted = granted, aiConsentSuppressPrompt = suppress)
        store.saveAiConsent(granted, suppress)
    }

    fun resolvedProfileDisplayName(fallback: String): String =
        state.profileDisplayName.trim().ifEmpty { fallback }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppSettingsViewModel(container.appSettings, container.appSettings.load()) as T
            }
    }
}

@Composable
fun rememberAppSettingsViewModel(): AppSettingsViewModel {
    val owner = LocalViewModelStoreOwner.current
        ?: error("No ViewModelStoreOwner")
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.lanread.app.LanReadApp).container
    return androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = owner,
        factory = AppSettingsViewModel.factory(container)
    )
}

/** Resolves the effective dark mode from settings theme + system. */
@Composable
fun AppSettingsViewModel.effectiveDarkMode(): Boolean {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    return when (state.theme) {
        com.lanread.app.data.settings.AppThemeMode.SYSTEM -> systemDark
        com.lanread.app.data.settings.AppThemeMode.LIGHT -> false
        com.lanread.app.data.settings.AppThemeMode.DARK -> true
    }
}
