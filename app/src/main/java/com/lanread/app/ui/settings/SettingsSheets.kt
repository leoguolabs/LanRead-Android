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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.data.settings.AppThemeMode
import com.lanread.app.data.settings.HighlightSortModeOption
import com.lanread.app.data.settings.ReadingFontOption
import com.lanread.app.data.settings.ReadingFontSizeOption
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.theme.AccentBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF4A71D6), RoundedCornerShape(20.dp))
            ) {
                Text("澜", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.app_version) + " 1.0",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.app_tagline),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_copyright), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.app_about_subtitle), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReadingSettingsSheet(appSettings: AppSettingsViewModel, onDismiss: () -> Unit) {
    val state = appSettings.state
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.settings_reading_preferences), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.settings_font_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ReadingFontSizeOption.entries.forEach { option ->
                    val selected = state.readingFontSize == option
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                if (selected) AccentBlue else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { appSettings.setReadingFontSize(option) }
                    ) {
                        Text(
                            stringResource(
                                when (option) {
                                    ReadingFontSizeOption.SMALL -> R.string.settings_font_size_small
                                    ReadingFontSizeOption.MEDIUM -> R.string.settings_font_size_medium
                                    ReadingFontSizeOption.LARGE -> R.string.settings_font_size_large
                                    ReadingFontSizeOption.EXTRA_LARGE -> R.string.settings_font_size_extra_large
                                }
                            ),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.settings_font_type), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ReadingFontOption.entries.forEach { option ->
                    val selected = state.readingFont == option
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                if (selected) AccentBlue else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { appSettings.setReadingFont(option) }
                    ) {
                        Text(
                            stringResource(
                                when (option) {
                                    ReadingFontOption.SYSTEM -> R.string.settings_font_family_system
                                    ReadingFontOption.SERIF -> R.string.settings_font_family_serif
                                    ReadingFontOption.MONOSPACE -> R.string.settings_font_family_monospace
                                }
                            ),
                            fontSize = 12.sp,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row {
                Text(stringResource(R.string.settings_typography_line_spacing))
                Spacer(Modifier.weight(1f))
                Text(String.format("%.1f", state.lineSpacing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(
                value = state.lineSpacing.toFloat(),
                onValueChange = { appSettings.setLineSpacing(it.toDouble()) },
                valueRange = 0.6f..2.4f,
                steps = 17
            )

            Row {
                Text(stringResource(R.string.settings_typography_page_margins))
                Spacer(Modifier.weight(1f))
                Text("${state.pageMargins.toInt()}pt", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(
                value = state.pageMargins.toFloat(),
                onValueChange = { appSettings.setPageMargins(it.toDouble()) },
                valueRange = 0f..50f,
                steps = 9
            )

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.settings_preview), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_sample_text_content),
                fontSize = state.readingFontSize.px.sp,
                lineHeight = (state.readingFontSize.px * 1.6).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = (state.pageMargins / 2).dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
fun ThemeSettingsSheet(appSettings: AppSettingsViewModel, onDismiss: () -> Unit) {
    val state = appSettings.state
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.settings_theme_appearance), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            AppThemeMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { appSettings.setTheme(mode) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(
                                when (mode) {
                                    AppThemeMode.SYSTEM -> R.string.settings_theme_mode_system
                                    AppThemeMode.LIGHT -> R.string.settings_theme_mode_always_light
                                    AppThemeMode.DARK -> R.string.settings_theme_mode_always_dark
                                }
                            )
                        )
                        Text(
                            stringResource(
                                when (mode) {
                                    AppThemeMode.SYSTEM -> R.string.settings_theme_mode_follow_system
                                    AppThemeMode.LIGHT -> R.string.settings_theme_mode_always_light
                                    AppThemeMode.DARK -> R.string.settings_theme_mode_always_dark
                                }
                            ),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.theme == mode) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = AccentBlue)
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightSortSheet(appSettings: AppSettingsViewModel, onDismiss: () -> Unit) {
    val state = appSettings.state
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.settings_highlight_sort_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            HighlightSortModeOption.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { appSettings.setHighlightSortMode(mode) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(
                            when (mode) {
                                HighlightSortModeOption.CHAPTER -> R.string.settings_highlight_sort_by_chapter
                                HighlightSortModeOption.MODIFIED_TIME -> R.string.settings_highlight_sort_by_modified_time
                            }
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (state.highlightSortMode == mode) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = AccentBlue)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_highlight_sort_sync_note),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileSheet(appSettings: AppSettingsViewModel, onDismiss: () -> Unit) {
    val state = appSettings.state
    var nameDraft by remember(state.profileDisplayName) { mutableStateOf(state.profileDisplayName) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.lanread.app.LanReadApp).container

    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            }
            container.appSettings.setProfileAvatarFromBitmap(bitmap)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF6992EF), Color(0xFF4A71D6))),
                            CircleShape
                        )
                        .clickable { photoPicker.launch("image/*") }
                ) {
                    val avatar = state.profileAvatarData?.let { container.appSettings.decodeAvatar(it) }
                    if (avatar != null) {
                        Image(bitmap = avatar.asImageBitmap(), contentDescription = null, modifier = Modifier.size(72.dp))
                    } else {
                        Text(
                            appSettings.resolvedProfileDisplayName(stringResource(R.string.settings_profile_default_display_name)).take(1),
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_profile_avatar_edit),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                appSettings.resolvedProfileDisplayName(stringResource(R.string.settings_profile_default_display_name)),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.settings_profile_preview_hint),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = nameDraft,
                onValueChange = { appSettings.setProfileDisplayName(it) },
                placeholder = { Text(stringResource(R.string.settings_profile_display_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    stringResource(R.string.settings_profile_default_name_hint),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${state.profileDisplayName.length}/24",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.profileAvatarData != null) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { appSettings.setProfileAvatar(null) }) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_profile_avatar_remove), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun DataManagementSheet(appSettings: AppSettingsViewModel, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.lanread.app.LanReadApp).container
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf<String?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dir = context.cacheDir
            var total = 0L
            dir.walkTopDown().filter { it.isFile }.forEach { total += it.length() }
            val booksDir = container.bookImportService.booksDirectory
            cacheSize = if (total > 1024 * 1024) "${total / 1024 / 1024} MB" else "${total / 1024} KB"
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp)) {
            Text(stringResource(R.string.settings_data_management), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Export
            TextButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        val file = exportAllData(container)
                        withContext(Dispatchers.Main) {
                            resultMessage = file.absolutePath
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) { resultMessage = "Export failed: ${e.message}" }
                    }
                }
            }) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_export_all_data_button), color = AccentBlue)
            }
            Text(
                stringResource(R.string.export_all_data_description),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            // Storage
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_cache_size))
                Spacer(Modifier.weight(1f))
                Text(cacheSize ?: stringResource(R.string.cache_status_calculating), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    context.cacheDir.deleteRecursively()
                    container.database.libraryDao().clearAiCaches()
                    withContext(Dispatchers.Main) { cacheSize = "0 KB" }
                }
            }) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_cache_clear), color = Color(0xFFFF9500))
            }

            Spacer(Modifier.height(12.dp))
            // Danger zone
            TextButton(onClick = { clearConfirm = true }) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.settings_data_clear_all), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (clearConfirm) {
        AlertDialog(
            onDismissRequest = { clearConfirm = false },
            title = { Text(stringResource(R.string.settings_data_clear_all)) },
            text = { Text(stringResource(R.string.settings_data_clear_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    clearConfirm = false
                    scope.launch(Dispatchers.IO) {
                        val dao = container.database.libraryDao()
                        val count = dao.bookCount()
                        dao.deleteAllBooks()
                        dao.deleteAllLibraryItems()
                        dao.deleteAllProgress()
                        dao.deleteAllBookmarks()
                        dao.deleteAllHighlights()
                        dao.deleteAllAnnotations()
                        container.bookImportService.booksDirectory.deleteRecursively()
                        withContext(Dispatchers.Main) {
                            resultMessage = context.getString(R.string.data_clear_success_message, count, "0 KB")
                        }
                    }
                }) {
                    Text(stringResource(R.string.settings_data_clear_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text(stringResource(R.string.library_import_result_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { resultMessage = null }) { Text(stringResource(R.string.common_confirm)) }
            }
        )
    }
}

private suspend fun exportAllData(container: com.lanread.app.di.AppContainer): java.io.File =
    withContext(Dispatchers.IO) {
        val dao = container.database.libraryDao()
        val books = dao.allBooks()
        val booksJson = org.json.JSONArray()
        books.forEach { book ->
            val progress = dao.findProgressByBookId(book.id)
            val item = dao.findLibraryItemByBookId(book.id)
            val bookJson = org.json.JSONObject()
                .put("checksum", book.checksum)
                .put("title", book.title)
                .put("author", book.author)
            progress?.let {
                bookJson.put(
                    "readingProgress",
                    org.json.JSONObject()
                        .put("currentPage", it.currentPage)
                        .put("currentPosition", it.currentPosition ?: org.json.JSONObject.NULL)
                        .put("progressPercentage", it.progressPercentage)
                        .put("lastReadAt", it.lastReadAt)
                        .put("totalReadingTime", it.totalReadingTime)
                        .put("detailedReadingTime", it.detailedReadingTime)
                        .put("skimmingReadingTime", it.skimmingReadingTime)
                )
            }
            item?.let {
                bookJson.put(
                    "libraryItem",
                    org.json.JSONObject()
                        .put("statusRaw", it.statusRaw)
                        .put("isFavorite", it.isFavorite)
                        .put("rating", it.rating)
                )
            }
            booksJson.put(bookJson)
        }
        val payload = org.json.JSONObject()
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("readingData", org.json.JSONObject().put("books", booksJson))
        val formatter = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.US)
        val file = java.io.File(
            container.bookImportService.appContext.cacheDir,
            "LanRead-AllData-${formatter.format(java.util.Date())}.json"
        )
        file.writeText(payload.toString(), Charsets.UTF_8)
        file
    }
