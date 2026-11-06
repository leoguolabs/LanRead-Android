package com.lanread.app.ui.summary

import com.lanread.app.ads.AdMobBannerView

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.di.AppContainer
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.library.LibraryCardData
import com.lanread.app.ui.theme.AccentBlue
import com.lanread.app.util.AISummaryService
import com.lanread.app.util.ParsedChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * AI 导读摘要 full-screen page. Mirrors iOS AISummaryView:
 * header (title + 开始阅读), overview layout loaded immediately, streaming
 * partial summary, key points when ready, bottom 略读模式 entry.
 */
@Composable
fun AiSummaryRoute(
    card: LibraryCardData,
    appSettings: AppSettingsViewModel,
    onClose: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenSkimming: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.lanread.app.LanReadApp).container
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onClose)

    val cached = remember(card.book.id) {
        container.aiSummaryService.cachedSummary(card.book)
    }
    var summaryState by remember(card.book.id) {
        mutableStateOf<AISummaryService.SummaryState>(
            cached ?: AISummaryService.SummaryState.Generating(0.05, "")
        )
    }

    LaunchedEffect(card.book.id) {
        if (cached == null) {
            generateSummary(container, card.book, appSettings) { newState ->
                summaryState = newState
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header bar with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton2(onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
            }

            // Immediately loaded Book Overview Screen content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    // Summary header (AI 导读摘要 + 开始阅读)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Psychology,
                                    contentDescription = null,
                                    tint = AccentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    stringResource(R.string.ai_summary_title),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                stringResource(R.string.ai_summary_subtitle),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenReader,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                        ) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.ai_summary_start_reading),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // AdMob Banner Ad
                    AdMobBannerView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Scrollable summary area
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (val state = summaryState) {
                            is AISummaryService.SummaryState.Ready -> {
                                // 内容摘要
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Description, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.ai_summary_content_summary), fontWeight = FontWeight.Medium)
                                }
                                MarkdownText(state.summary)

                                // 关键要点
                                if (state.keyPoints.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.FormatListBulleted, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.ai_summary_key_points), fontWeight = FontWeight.Medium)
                                    }
                                    state.keyPoints.forEach { point ->
                                        Row {
                                            Text("·", color = Color(0xFFFF9500), fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(8.dp))
                                            Text(point, lineHeight = 22.sp)
                                        }
                                    }
                                }

                                // Footer
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_disclaimer), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        stringResource(R.string.ai_summary_generated_at) + " " + formatTimestamp(state.generatedAt),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is AISummaryService.SummaryState.Generating -> {
                                // Progress bar
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    LinearProgressIndicator(
                                        progress = { state.progress.toFloat() },
                                        modifier = Modifier.weight(1f),
                                        color = AccentBlue
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${(state.progress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 内容摘要
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Description, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.ai_summary_content_summary), fontWeight = FontWeight.Medium)
                                }

                                if (state.partialText.isNotEmpty()) {
                                    MarkdownText(state.partialText)
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp,
                                            color = AccentBlue
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            stringResource(R.string.ai_summary_analyzing),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Footer
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_disclaimer), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            is AISummaryService.SummaryState.Failed -> {
                                ErrorView(state.message) {
                                    scope.launch {
                                        summaryState = AISummaryService.SummaryState.Generating(0.05, "")
                                        generateSummary(container, card.book, appSettings) { summaryState = it }
                                    }
                                }
                            }

                            is AISummaryService.SummaryState.Idle -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = AccentBlue
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        stringResource(R.string.common_loading),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 略读模式 entry
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFF9500).copy(alpha = 0.82f))),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onOpenSkimming)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.skimming_mode_badge),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text("›", color = Color.White, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

private suspend fun generateSummary(
    container: AppContainer,
    book: BookEntity,
    appSettings: AppSettingsViewModel,
    setState: (AISummaryService.SummaryState) -> Unit
) {
    val service = container.aiSummaryService
    withContext(Dispatchers.IO) {
        try {
            val chapters = parseStoredChapters(book)
            val result = service.generateBookSummary(book, chapters) { interim ->
                withContext(Dispatchers.Main) {
                    setState(interim)
                }
            }
            // Persist cache
            val dao = container.database.libraryDao()
            dao.findBookById(book.id)?.let { fresh ->
                val keyPointsJson = JSONArray().apply { result.keyPoints.forEach { put(it) } }.toString()
                dao.updateBook(
                    fresh.copy(
                        aiSummary = result.summary,
                        aiKeyPoints = keyPointsJson,
                        aiSummaryGeneratedAt = System.currentTimeMillis()
                    )
                )
            }
            withContext(Dispatchers.Main) {
                setState(AISummaryService.SummaryState.Ready(result.summary, result.keyPoints, System.currentTimeMillis()))
            }
        } catch (e: Exception) {
            android.util.Log.e("AiSummary", "generateSummary error", e)
            withContext(Dispatchers.Main) {
                setState(AISummaryService.SummaryState.Failed(e.message ?: "error"))
            }
        }
    }
}

fun parseStoredChapters(book: BookEntity): List<ParsedChapter> {
    val chapters = mutableListOf<ParsedChapter>()
    val raw = book.metadata ?: return chapters
    try {
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            chapters.add(
                ParsedChapter(
                    title = obj.optString("title", "Chapter ${i + 1}"),
                    content = obj.optString("content", ""),
                    htmlContent = "",
                    order = obj.optString("order", "$i").toIntOrNull() ?: i,
                    sourceHref = null
                )
            )
        }
    } catch (_: Exception) {
    }
    return chapters
}

@Composable
private fun IconButton2(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.ai_summary_error_title), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.ai_summary_error_retry))
        }
    }
}

@Composable
fun MarkdownText(markdown: String, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column {
        markdown.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                return@forEachIndexed
            }
            when {
                line.startsWith("### ") -> Text(
                    line.removePrefix("### "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor
                )
                line.startsWith("## ") -> Text(
                    line.removePrefix("## "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                line.startsWith("# ") -> Text(
                    line.removePrefix("# "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textColor
                )
                line.startsWith("• ") || line.startsWith("- ") || line.startsWith("* ") || line.startsWith("· ") || line.startsWith("·") ||
                    (line.length > 2 && line[0].isDigit() && (line[1] == '.' || line[1] == '、')) -> {
                    val cleanText = if (line.length > 2 && line[0].isDigit() && (line[1] == '.' || line[1] == '、')) {
                        line.substring(2).trim()
                    } else {
                        line.trimStart('•', '-', '*', '·', ' ').trim()
                    }
                    Row {
                        Text("·", color = Color(0xFFFF9500), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stripBoldMarkers(cleanText),
                            color = textColor,
                            lineHeight = 22.sp
                        )
                    }
                }
                else -> Text(stripBoldMarkers(line), color = textColor, lineHeight = 22.sp)
            }
        }
    }
}

private fun stripBoldMarkers(text: String): String = text.replace("**", "")

private fun formatTimestamp(timestamp: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
    return formatter.format(java.util.Date(timestamp))
}
