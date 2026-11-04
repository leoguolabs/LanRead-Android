package com.lanread.app.ui.skimming

import android.app.Activity
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.lanread.app.ads.AdMobManager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.di.AppContainer
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.summary.MarkdownText
import com.lanread.app.ui.summary.parseStoredChapters
import com.lanread.app.util.AISummaryService
import com.lanread.app.util.ParsedChapter
import com.lanread.app.util.SkimmingChapterSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Skimming mode: dark full-screen pager with per-chapter AI summaries
 * (mirrors iOS SkimmingModeView using smooth HorizontalPager).
 */
@Composable
fun SkimmingScreen(
    book: BookEntity,
    container: AppContainer,
    appSettings: AppSettingsViewModel,
    onClose: () -> Unit,
    onStartChapterReading: (Int) -> Unit,
    onResumeFullReading: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val service = container.aiSummaryService

    var chapters by remember { mutableStateOf<List<ParsedChapter>>(emptyList()) }
    var isLoadingChapters by remember { mutableStateOf(true) }
    var summaries by remember { mutableStateOf<Map<Int, SkimmingChapterSummary>>(emptyMap()) }
    var loadingIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var errors by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showTocSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var forwardSwipeCount by remember { mutableStateOf(0) }
    var interstitialPresentedCount by remember { mutableStateOf(0) }
    var pendingInterstitial by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(initialPage = 0) { chapters.size }
    var lastRecordedPage by remember { mutableStateOf(0) }

    val languageName = com.lanread.app.util.aiOutputLanguageName(
        appSettings.state.language,
        com.lanread.app.data.settings.AppLanguageOption.ZH_HANS
    )

    LaunchedEffect(book.id) {
        AdMobManager.loadInterstitial(context)
    }

    LaunchedEffect(book.id) {
        withContext(Dispatchers.IO) {
            chapters = parseStoredChapters(book)
            // Load cached summaries
            val cached = service.loadSkimmingSummaries(book)
            val restored = mutableMapOf<Int, SkimmingChapterSummary>()
            cached.forEach { (key, summary) ->
                val parts = key.split("-")
                if (parts.size >= 2) {
                    val order = parts[1].toIntOrNull()
                    if (order != null) restored[order] = summary
                }
            }
            summaries = restored
            isLoadingChapters = false
        }
    }

    // Track forward chapter swipe and prepare interstitial ads
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (settledPage > lastRecordedPage) {
                val advancedCount = settledPage - lastRecordedPage
                forwardSwipeCount += advancedCount
                val completedTriggerCount = forwardSwipeCount / 3
                if (appSettings.state.areAdsEnabled && completedTriggerCount > interstitialPresentedCount) {
                    AdMobManager.loadInterstitial(context)
                    pendingInterstitial = true
                    if (appSettings.state.aiAdvanceAdNoticeEnabled) {
                        val noticeRes = if (AdMobManager.isInterstitialReady()) {
                            R.string.skimming_ad_notice_third_ready
                        } else {
                            R.string.skimming_ad_notice_third_maybe
                        }
                        Toast.makeText(context, context.getString(noticeRes), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            lastRecordedPage = settledPage
        }
    }

    LaunchedEffect(pendingInterstitial, pagerState.settledPage) {
        if (pendingInterstitial) {
            pendingInterstitial = false
            kotlinx.coroutines.delay(260)
            if (AdMobManager.isInterstitialReady()) {
                val shown = AdMobManager.showInterstitial(context) {
                    interstitialPresentedCount++
                }
                if (!shown) {
                    Toast.makeText(context, context.getString(R.string.skimming_ad_notice_skipped_not_ready), Toast.LENGTH_SHORT).show()
                }
            } else if (AdMobManager.isRewardedInterstitialReady()) {
                val shown = AdMobManager.showRewardedInterstitial(context) {
                    interstitialPresentedCount++
                }
                if (!shown) {
                    Toast.makeText(context, context.getString(R.string.skimming_ad_notice_skipped_not_ready), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, context.getString(R.string.skimming_ad_notice_skipped_not_ready), Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Preload current + next 3
    LaunchedEffect(pagerState.currentPage, chapters) {
        if (chapters.isEmpty()) return@LaunchedEffect
        val currentIdx = pagerState.currentPage
        val toLoad = (0..3).map { currentIdx + it }.filter { it < chapters.size }
        toLoad.forEach { index ->
            if (!summaries.containsKey(index) && !loadingIndices.contains(index)) {
                loadingIndices = loadingIndices + index
                scope.launch(Dispatchers.IO) {
                    try {
                        val summary = service.generateSkimmingSummary(book, chapters[index], languageName)
                        withContext(Dispatchers.Main) {
                            summaries = summaries + (index to summary)
                            loadingIndices = loadingIndices - index
                        }
                        // persist
                        withContext(Dispatchers.IO) {
                            val dao = container.database.libraryDao()
                            dao.findBookById(book.id)?.let { fresh ->
                                val cache = service.loadSkimmingSummaries(fresh).toMutableMap()
                                cache[service.skimmingCacheKey(book.id, chapters[index].order, languageName)] = summary
                                dao.updateBook(fresh.copy(skimmingSummaries = service.storeSkimmingSummaries(book.id, cache)))
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            errors = errors + (index to (e.message ?: "error"))
                            loadingIndices = loadingIndices - index
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0D1A), Color(0xFF050508))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 8.dp)
        ) {
            // Top controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clickable(onClick = onClose)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Color.White.copy(alpha = 0.9f))
                }
                Spacer(Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clickable { showTocSheet = true }
                ) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.reader_toc_title), tint = Color.White.copy(alpha = 0.9f))
                }
            }

            when {
                isLoadingChapters -> {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.skimming_loading_chapters), color = Color.White)
                        }
                    }
                }
                chapters.isEmpty() -> {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.skimming_no_chapters), color = Color.White.copy(alpha = 0.8f))
                    }
                }
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        pageSpacing = 12.dp,
                        beyondViewportPageCount = 1,
                        key = { index -> chapters.getOrNull(index)?.order ?: index }
                    ) { page ->
                        val chapter = chapters[page]
                        val summary = summaries[page]
                        val isLoading = loadingIndices.contains(page)
                        val error = errors[page]

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 6.dp)
                                .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp)
                        ) {
                            Text(chapter.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

                            Spacer(Modifier.height(16.dp))

                            when {
                                isLoading -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        CircularProgressIndicator(color = Color.White)
                                        Spacer(Modifier.height(12.dp))
                                        Text(stringResource(R.string.skimming_generating), color = Color.White.copy(alpha = 0.8f))
                                        Spacer(Modifier.height(8.dp))
                                        Text(stringResource(R.string.ai_disclaimer), fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                                error != null -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text(error, color = Color.White)
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                errors = errors - page
                                                loadingIndices = loadingIndices + page
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.15f)
                                            )
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.skimming_retry), color = Color.White)
                                        }
                                    }
                                }
                                summary != null -> SkimmingSummarySections(summary, onStartChapterReading = { onStartChapterReading(page) })
                                else -> {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text(stringResource(R.string.skimming_placeholder), color = Color.White.copy(alpha = 0.7f))
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                // Request summary for current chapter (preloads current+3)
                                                loadingIndices = loadingIndices + page
                                                errors = errors - page
                                            },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = Color.White.copy(alpha = 0.15f)
                                            )
                                        ) {
                                            Text(stringResource(R.string.skimming_generate), color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom progress
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${pagerState.currentPage + 1}/${chapters.size}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f).height(4.dp).background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(2.dp))) {
                            val progressFraction = if (chapters.isNotEmpty()) {
                                ((pagerState.currentPage + pagerState.currentPageOffsetFraction + 1f) / chapters.size).coerceIn(0f, 1f)
                            } else 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .height(4.dp)
                                    .background(Color.White, RoundedCornerShape(2.dp))
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .clickable(onClick = onResumeFullReading)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.skimming_switch_to_full), fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // TOC Bottom Sheet
        if (showTocSheet && chapters.isNotEmpty()) {
            ModalBottomSheet(
                onDismissRequest = { showTocSheet = false },
                containerColor = Color(0xFF141724),
                contentColor = Color.White
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.reader_toc_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { showTocSheet = false }) {
                            Text(stringResource(R.string.common_done), color = Color(0xFF007AFF))
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                    ) {
                        itemsIndexed(chapters) { index, ch ->
                            val isCurrent = index == pagerState.currentPage
                            val hasSummary = summaries.containsKey(index)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showTocSheet = false
                                        scope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isCurrent) Color(0xFF007AFF) else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(16.dp)
                                        )
                                ) {
                                    Text(
                                        "${index + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        ch.title,
                                        fontSize = 15.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) Color(0xFF007AFF) else Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    summaries[index]?.let { sum ->
                                        if (sum.keywords.isNotEmpty()) {
                                            Text(
                                                sum.keywords.take(3).joinToString(" ? "),
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.6f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                if (hasSummary) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD60A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkimmingSummarySections(summary: SkimmingChapterSummary, onStartChapterReading: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        // Time badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color(0xFFFFD60A), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.skimming_time_estimate, summary.estimatedMinutes),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }

        // ????
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF007AFF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.skimming_goal), fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(summary.readingGoal, color = Color.White, lineHeight = 22.sp)
        }

        // ????
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Segment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.skimming_structure), fontWeight = FontWeight.Bold, color = Color.White)
        }
        summary.structure.forEach { point ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(point.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(point.insight, color = Color.White.copy(alpha = 0.85f))
            }
        }

        // ???
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FormatQuote, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.skimming_key_sentences), fontWeight = FontWeight.Bold, color = Color.White)
        }
        summary.keySentences.forEachIndexed { index, sentence ->
            Row {
                Text("${index + 1}.", color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(8.dp))
                Text(sentence, color = Color.White, lineHeight = 22.sp)
            }
        }

        // ???
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Spellcheck, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.skimming_keywords), fontWeight = FontWeight.Bold, color = Color.White)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            summary.keywords.forEach { keyword ->
                Text(
                    keyword,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // ????
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.QuestionAnswer, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.skimming_questions), fontWeight = FontWeight.Bold, color = Color.White)
        }
        summary.inspectionQuestions.forEach { question ->
            Text("? $question", color = Color.White)
        }

        // AI ????
        if (summary.aiNarrative.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.skimming_narrative), fontWeight = FontWeight.Bold, color = Color.White)
            }
            MarkdownText(summary.aiNarrative, textColor = Color.White)
        }

        // ????
        Button(
            onClick = onStartChapterReading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF007AFF).copy(alpha = 0.8f)
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.skimming_start_chapter_reading), fontWeight = FontWeight.SemiBold)
        }

        Text(stringResource(R.string.ai_disclaimer), fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
    }
}
