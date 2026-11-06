package com.lanread.app.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.data.settings.AppLanguageOption
import com.lanread.app.data.settings.AppThemeMode
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.theme.AccentBlue
import kotlinx.coroutines.launch

/** Root settings screen: sections mirroring iOS SettingsView. */
@Composable
fun SettingsScreen(
    appSettings: AppSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state = appSettings.state
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showingAbout by remember { mutableStateOf(false) }
    var showingDataManagement by remember { mutableStateOf(false) }
    var showingReadingSettings by remember { mutableStateOf(false) }
    var showingThemeSettings by remember { mutableStateOf(false) }
    var showingHighlightSort by remember { mutableStateOf(false) }
    var showingProfile by remember { mutableStateOf(false) }
    var showingAiConsent by remember { mutableStateOf(false) }
    var showingTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text(
            stringResource(R.string.tab_settings),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 个人资料
        SettingsSection(title = stringResource(R.string.settings_profile_section)) {
            SettingsRow(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.settings_profile_title),
                subtitle = appSettings.resolvedProfileDisplayName(
                    stringResource(R.string.settings_profile_default_display_name)
                ),
                leading = { ProfileAvatarSmall(appSettings) },
                onClick = { showingProfile = true }
            )
        }

        // 语言
        SettingsSection(title = stringResource(R.string.settings_language)) {
            MenuRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.settings_language),
                options = AppLanguageOption.entries.map { it.displayName.ifEmpty { stringResource(R.string.settings_language_follow_system) } },
                selectedIndex = AppLanguageOption.entries.indexOf(state.language),
                onSelect = { appSettings.setLanguage(AppLanguageOption.entries[it]) }
            )
            MenuRow(
                icon = Icons.Filled.Description,
                title = stringResource(R.string.settings_translation_target_language),
                options = AppLanguageOption.entries.map { it.displayName.ifEmpty { stringResource(R.string.settings_language_follow_system) } },
                selectedIndex = AppLanguageOption.entries.indexOf(state.translationLanguage),
                onSelect = { appSettings.setTranslationLanguage(AppLanguageOption.entries[it]) }
            )
        }

        // 阅读设置
        SettingsSection(title = stringResource(R.string.reading_settings_title)) {
            SettingsRow(
                icon = Icons.Filled.TextFields,
                title = stringResource(R.string.settings_reading_preferences),
                onClick = { showingReadingSettings = true }
            )
            SettingsRow(
                icon = Icons.Filled.Brush,
                title = stringResource(R.string.settings_theme_appearance),
                onClick = { showingThemeSettings = true }
            )
            SettingsRow(
                icon = Icons.Filled.SwapVert,
                title = stringResource(R.string.settings_highlight_sort_title),
                subtitle = stringResource(R.string.settings_highlight_sort_subtitle_format, highlightSortName(state.highlightSortMode)),
                onClick = { showingHighlightSort = true }
            )
        }

        // 数据与同步
        SettingsSection(title = stringResource(R.string.settings_data_and_sync)) {
            SettingsRow(
                icon = Icons.Filled.CloudSync,
                title = stringResource(R.string.notion_connect),
                subtitle = stringResource(R.string.notion_connection_disconnected),
                onClick = { /* Notion auth */ }
            )
            SettingsRow(
                icon = Icons.Filled.Build,
                title = stringResource(R.string.settings_data_management),
                onClick = { showingDataManagement = true }
            )
        }

        // AI 数据与隐私
        SettingsSection(title = stringResource(R.string.settings_ai_privacy_section)) {
            SettingsRow(
                icon = Icons.Filled.Shield,
                title = stringResource(R.string.settings_ai_privacy_manage_title),
                subtitle = if (state.aiConsentGranted) stringResource(R.string.settings_ai_privacy_status_allowed)
                else stringResource(R.string.settings_ai_privacy_status_not_allowed),
                onClick = { showingAiConsent = true }
            )
            LinkRow(
                icon = Icons.Filled.Handshake,
                title = stringResource(R.string.app_privacy_policy),
                url = "https://isla-reader.top/privacy"
            )
        }

        // 通知提醒
        SettingsSection(title = stringResource(R.string.settings_notifications)) {
            ToggleRow(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.reading_reminder_daily),
                checked = state.isReadingReminderEnabled,
                onChecked = { enabled ->
                    appSettings.setReadingReminderEnabled(enabled)
                    if (enabled) {
                        val minutes = state.readingReminderMinutesSinceMidnight
                        ReadingReminderService.schedule(context, minutes / 60, minutes % 60, state.dailyReadingGoal)
                    } else {
                        ReadingReminderService.cancel(context)
                    }
                }
            )
            if (state.isReadingReminderEnabled) {
                SettingsRow(
                    icon = Icons.Filled.Schedule,
                    title = stringResource(R.string.reading_reminder_time),
                    value = String.format(
                        "%02d:%02d",
                        state.readingReminderMinutesSinceMidnight / 60,
                        state.readingReminderMinutesSinceMidnight % 60
                    ),
                    onClick = { showingTimePicker = true }
                )
            }
            StepperRow(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.settings_daily_goal),
                value = state.dailyReadingGoal,
                range = 10..180,
                step = 5,
                suffix = stringResource(R.string.common_minutes),
                onChange = {
                    appSettings.setDailyReadingGoal(it)
                    if (state.isReadingReminderEnabled) {
                        val minutes = state.readingReminderMinutesSinceMidnight
                        ReadingReminderService.schedule(context, minutes / 60, minutes % 60, it)
                    }
                }
            )
        }

        // 关于
        SettingsSection(title = stringResource(R.string.settings_about)) {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = stringResource(R.string.app_about_title),
                onClick = { showingAbout = true }
            )
            SettingsRow(
                icon = Icons.Filled.Numbers,
                title = stringResource(R.string.app_version),
                value = "1.0"
            )
        }

        // 支持
        SettingsSection(title = stringResource(R.string.settings_support)) {
            LinkRow(
                icon = Icons.Filled.Email,
                title = stringResource(R.string.app_contact_us),
                url = "mailto:support@isla-reader.top"
            )
            LinkRow(
                icon = Icons.Filled.Handshake,
                title = stringResource(R.string.app_privacy_policy),
                url = "https://isla-reader.top/privacy"
            )
        }
    }

    if (showingAbout) AboutSheet { showingAbout = false }
    if (showingDataManagement) DataManagementSheet(appSettings) { showingDataManagement = false }
    if (showingReadingSettings) ReadingSettingsSheet(appSettings) { showingReadingSettings = false }
    if (showingThemeSettings) ThemeSettingsSheet(appSettings) { showingThemeSettings = false }
    if (showingHighlightSort) HighlightSortSheet(appSettings) { showingHighlightSort = false }
    if (showingProfile) ProfileSheet(appSettings) { showingProfile = false }
    if (showingAiConsent) AiConsentSheet(appSettings, isLaunchPrompt = false) { showingAiConsent = false }

    if (showingTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = state.readingReminderMinutesSinceMidnight / 60,
            initialMinute = state.readingReminderMinutesSinceMidnight % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showingTimePicker = false },
            title = { Text(stringResource(R.string.reading_reminder_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = timeState.hour * 60 + timeState.minute
                    appSettings.setReadingReminderMinutes(minutes)
                    ReadingReminderService.schedule(context, timeState.hour, timeState.minute, state.dailyReadingGoal)
                    showingTimePicker = false
                }) { Text(stringResource(R.string.common_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showingTimePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
fun highlightSortName(mode: com.lanread.app.data.settings.HighlightSortModeOption): String =
    stringResource(
        when (mode) {
            com.lanread.app.data.settings.HighlightSortModeOption.CHAPTER -> R.string.settings_highlight_sort_by_chapter
            com.lanread.app.data.settings.HighlightSortModeOption.MODIFIED_TIME -> R.string.settings_highlight_sort_by_modified_time
        }
    )

@Composable
fun ProfileAvatarSmall(appSettings: AppSettingsViewModel) {
    val container = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.lanread.app.LanReadApp).container
    val bitmap = appSettings.state.profileAvatarData?.let { container.appSettings.decodeAvatar(it) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .background(
                if (bitmap == null) {
                    Brush.linearGradient(listOf(Color(0xFF6992EF), Color(0xFF4A71D6)))
                } else {
                    Brush.linearGradient(listOf(Color.Gray, Color.Gray))
                },
                CircleShape
            )
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        } else {
            Text(
                appSettings.resolvedProfileDisplayName("读者").take(1),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
        )
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (value != null) {
            Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(
            options.getOrNull(selectedIndex) ?: "",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
        }
    }
}

@Composable
fun ToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
fun StepperRow(
    icon: ImageVector,
    title: String,
    value: Int,
    range: IntRange,
    step: Int,
    suffix: String,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = { onChange((value - step).coerceIn(range.first, range.last)) }) { Text("−") }
        Text("$value $suffix", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { onChange((value + step).coerceIn(range.first, range.last)) }) { Text("+") }
    }
}

@Composable
fun LinkRow(icon: ImageVector, title: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                } catch (_: Exception) {
                }
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, color = AccentBlue, modifier = Modifier.weight(1f))
    }
}
