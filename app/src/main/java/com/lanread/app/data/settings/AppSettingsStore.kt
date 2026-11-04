package com.lanread.app.data.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.edit
import java.io.ByteArrayOutputStream

enum class AppThemeMode(val raw: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw } ?: SYSTEM
    }
}

enum class ReadingFontSizeOption(val raw: String, val px: Int) {
    SMALL("small", 16), MEDIUM("medium", 19), LARGE("large", 22), EXTRA_LARGE("extra_large", 26);

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw } ?: MEDIUM
    }
}

enum class ReadingFontOption(val raw: String) {
    SYSTEM("system"), SERIF("serif"), MONOSPACE("monospace");

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw } ?: SYSTEM
    }
}

enum class AppLanguageOption(val raw: String, val displayName: String, val aiName: String) {
    SYSTEM("system", "", ""), EN("en", "English", "English"),
    ZH_HANS("zh-Hans", "中文", "Simplified Chinese"), JA("ja", "日本語", "Japanese"),
    KO("ko", "한국어", "Korean");

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw } ?: SYSTEM
    }
}

enum class HighlightSortModeOption(val raw: String) {
    CHAPTER("chapter"), MODIFIED_TIME("modified_time");

    companion object {
        fun from(raw: String?) = entries.firstOrNull { it.raw == raw } ?: MODIFIED_TIME
    }
}

data class AppSettingsState(
    val language: AppLanguageOption = AppLanguageOption.SYSTEM,
    val translationLanguage: AppLanguageOption = AppLanguageOption.SYSTEM,
    val theme: AppThemeMode = AppThemeMode.SYSTEM,
    val readingFontSize: ReadingFontSizeOption = ReadingFontSizeOption.MEDIUM,
    val readingFont: ReadingFontOption = ReadingFontOption.SYSTEM,
    val lineSpacing: Double = 1.0,
    val pageMargins: Double = 35.0,
    val highlightSortMode: HighlightSortModeOption = HighlightSortModeOption.MODIFIED_TIME,
    val profileDisplayName: String = "",
    val profileAvatarData: ByteArray? = null,
    val isReadingReminderEnabled: Boolean = false,
    val dailyReadingGoal: Int = 20,
    val readingReminderMinutesSinceMidnight: Int = 20 * 60,
    val aiConsentGranted: Boolean = false,
    val aiConsentSuppressPrompt: Boolean = false,
    val areAdsEnabled: Boolean = true,
    val aiAdvanceAdNoticeEnabled: Boolean = true
)

class AppSettingsStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("lanread_settings", Context.MODE_PRIVATE)

    val LINE_SPACING_RANGE = 0.6..2.4
    val LINE_SPACING_STEP = 0.1
    val PAGE_MARGINS_RANGE = 0.0..50.0
    val PAGE_MARGINS_STEP = 5.0

    fun load(): AppSettingsState {
        val storedLanguage = AppLanguageOption.from(prefs.getString("app_language", null))
        val storedTranslation = AppLanguageOption.from(prefs.getString("translation_language", null))
        return AppSettingsState(
            language = storedLanguage,
            translationLanguage = if (storedTranslation == AppLanguageOption.SYSTEM) storedLanguage else storedTranslation,
            theme = AppThemeMode.from(prefs.getString("app_theme", null)),
            readingFontSize = ReadingFontSizeOption.from(prefs.getString("reading_font_size", null)),
            readingFont = ReadingFontOption.from(prefs.getString("reading_font", null)),
            lineSpacing = clamp(
                prefs.getFloat("line_spacing", 1.0f).toDouble(),
                LINE_SPACING_RANGE
            ),
            pageMargins = clamp(
                prefs.getFloat("page_margins", 35.0f).toDouble(),
                PAGE_MARGINS_RANGE
            ),
            highlightSortMode = HighlightSortModeOption.from(prefs.getString("highlightSortMode", null)),
            profileDisplayName = prefs.getString("profileDisplayName", "") ?: "",
            profileAvatarData = prefs.getString("profileAvatarData", null)?.let { hexToBytes(it) },
            isReadingReminderEnabled = prefs.getBoolean("readingReminderEnabled", false),
            dailyReadingGoal = prefs.getInt("readingGoalMinutes", 20),
            readingReminderMinutesSinceMidnight = normalizeMinutes(
                prefs.getInt("readingReminderTimeMinutes", 20 * 60)
            ),
            aiConsentGranted = prefs.getBoolean("aiPrivacyPermissionGranted", false),
            aiConsentSuppressPrompt = prefs.getBoolean("aiPrivacySuppressLaunchPrompt", false),
            areAdsEnabled = !prefs.getBoolean("adRemovalUnlocked", false),
            aiAdvanceAdNoticeEnabled = prefs.getBoolean("aiAdvanceAdNoticeEnabled", true)
        )
    }

    fun saveLanguage(value: AppLanguageOption) = prefs.edit { putString("app_language", value.raw) }
    fun saveTranslationLanguage(value: AppLanguageOption) = prefs.edit { putString("translation_language", value.raw) }
    fun saveTheme(value: AppThemeMode) = prefs.edit { putString("app_theme", value.raw) }
    fun saveReadingFontSize(value: ReadingFontSizeOption) = prefs.edit { putString("reading_font_size", value.raw) }
    fun saveReadingFont(value: ReadingFontOption) = prefs.edit { putString("reading_font", value.raw) }
    fun saveLineSpacing(value: Double) = prefs.edit { putFloat("line_spacing", value.toFloat()) }
    fun savePageMargins(value: Double) = prefs.edit { putFloat("page_margins", value.toFloat()) }
    fun saveHighlightSortMode(value: HighlightSortModeOption) = prefs.edit { putString("highlightSortMode", value.raw) }
    fun saveProfileDisplayName(value: String) = prefs.edit { putString("profileDisplayName", value) }

    fun saveProfileAvatar(data: ByteArray?) {
        prefs.edit {
            if (data != null) putString("profileAvatarData", bytesToHex(data)) else remove("profileAvatarData")
        }
    }

    fun saveReadingReminderEnabled(value: Boolean) = prefs.edit { putBoolean("readingReminderEnabled", value) }
    fun saveDailyReadingGoal(value: Int) = prefs.edit { putInt("readingGoalMinutes", value) }
    fun saveReadingReminderMinutes(value: Int) =
        prefs.edit { putInt("readingReminderTimeMinutes", normalizeMinutes(value)) }

    fun areAdsEnabled(): Boolean = !prefs.getBoolean("adRemovalUnlocked", false)
    fun isAiAdvanceAdNoticeEnabled(): Boolean = prefs.getBoolean("aiAdvanceAdNoticeEnabled", true)
    fun saveAiAdvanceAdNoticeEnabled(value: Boolean) = prefs.edit { putBoolean("aiAdvanceAdNoticeEnabled", value) }
    fun saveAdRemovalUnlocked(value: Boolean) = prefs.edit { putBoolean("adRemovalUnlocked", value) }

    fun saveAiConsent(granted: Boolean, suppress: Boolean) = prefs.edit {
        putBoolean("aiPrivacyPermissionGranted", granted)
        putBoolean("aiPrivacySuppressLaunchPrompt", suppress)
        putInt("aiPrivacyConsentVersion", 2)
    }

    fun isSampleBookSeeded(): Boolean = prefs.getBoolean("bundled_sample_book_seeded_v1", false)
    fun markSampleBookSeeded() = prefs.edit { putBoolean("bundled_sample_book_seeded_v1", true) }

    fun resolvedProfileDisplayName(fallback: String): String {
        val trimmed = load().profileDisplayName.trim()
        return trimmed.ifEmpty { fallback }
    }

    fun setProfileAvatarFromBitmap(source: Bitmap?) {
        if (source == null) {
            saveProfileAvatar(null)
            return
        }
        val maxPixel = 512
        val maxDimension = maxOf(source.width, source.height)
        val resized = if (maxDimension > maxPixel) {
            val scale = maxPixel.toFloat() / maxDimension
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else source
        val square = centerCropSquare(resized)
        val out = ByteArrayOutputStream()
        square.compress(Bitmap.CompressFormat.JPEG, 85, out)
        saveProfileAvatar(out.toByteArray())
    }

    fun decodeAvatar(data: ByteArray?): Bitmap? =
        data?.takeIf { it.isNotEmpty() }?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }

    private fun centerCropSquare(bitmap: Bitmap): Bitmap {
        val side = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - side) / 2
        val top = (bitmap.height - side) / 2
        return Bitmap.createBitmap(bitmap, left, top, side, side)
    }

    private fun clamp(value: Double, range: ClosedRange<Double>): Double =
        value.coerceIn(range.start, range.endInclusive)
    private fun normalizeMinutes(minutes: Int): Int {
        val normalized = minutes % (24 * 60)
        return if (normalized >= 0) normalized else normalized + 24 * 60
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
}
