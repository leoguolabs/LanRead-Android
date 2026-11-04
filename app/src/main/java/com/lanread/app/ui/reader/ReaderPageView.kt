package com.lanread.app.ui.reader

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.di.AppContainer
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.util.ParsedChapter
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Single-chapter WebView with tap-zone page turns and swipe-to-page.
 * Mirrors iOS ReaderWebView interaction: <24% tap = prev, >76% = next, center = toolbar.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderPageView(
    book: BookEntity,
    chapter: ParsedChapter,
    chapterIndex: Int,
    highlights: List<com.lanread.app.data.local.entity.HighlightEntity>,
    appSettings: AppSettingsViewModel,
    container: AppContainer,
    darkMode: Boolean,
    showingToolbar: Boolean,
    currentPage: Int,
    totalPages: Int,
    onWebViewReady: (WebView) -> Unit,
    onPageSelected: (Int) -> Unit,
    onTotalPages: (Int) -> Unit,
    onToolbarToggle: () -> Unit,
    onTurnPage: (Boolean) -> Unit,
    onSelection: (SelectionPayload?) -> Unit = {},
    onHighlightTap: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val settings = appSettings.state
    val context = androidx.compose.ui.platform.LocalContext.current

    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var lastSwipeTime by remember { mutableStateOf(0L) }

    val html = remember(chapter.order, settings.readingFontSize, settings.lineSpacing, settings.pageMargins, darkMode, settings.readingFont) {
        ReaderHtmlBuilder.buildFullHtml(
            htmlContent = chapter.htmlContent,
            fontSizePx = settings.readingFontSize.px,
            lineSpacing = settings.lineSpacing,
            isDarkMode = darkMode,
            pageMarginsPx = settings.pageMargins.toInt(),
            fontOption = settings.readingFont
        )
    }

    // Keep latest callbacks so the bridge (remembered once) never uses stale state
    val latestToolbarToggle by androidx.compose.runtime.rememberUpdatedState(onToolbarToggle)
    val latestOnPageSelected by androidx.compose.runtime.rememberUpdatedState(onPageSelected)
    val latestOnTotalPages by androidx.compose.runtime.rememberUpdatedState(onTotalPages)
    val latestOnTurnPage by androidx.compose.runtime.rememberUpdatedState(onTurnPage)
    val latestOnSelection by androidx.compose.runtime.rememberUpdatedState(onSelection)
    val latestOnHighlightTap by androidx.compose.runtime.rememberUpdatedState(onHighlightTap)
    val latestRestorePage by androidx.compose.runtime.rememberUpdatedState(currentPage)
    var loadedHtml by remember { mutableStateOf<String?>(null) }
    val bridge = remember(book.id) {
        ReaderBridge(
            onToolbarToggle = { latestToolbarToggle() },
            onSelection = { payload -> latestOnSelection(payload) },
            onHighlightTap = { id, text -> latestOnHighlightTap(id, text) },
            onLinkTap = { _, _, _ -> },
            onPageMetrics = { type, value ->
                if (type == "pageCount") latestOnTotalPages(value)
                if (type == "currentPage" && !isDragging) latestOnPageSelected(value)
            },
            onPageTap = { zone ->
                when (zone) {
                    "prev" -> latestOnTurnPage(false)
                    "next" -> latestOnTurnPage(true)
                    else -> latestToolbarToggle()
                }
            }
        )
    }

    DisposableEffect(chapter.order) {
        onDispose { }
    }

    android.webkit.WebView.setWebContentsDebuggingEnabled(true)

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                getSettings().javaScriptEnabled = true
                getSettings().domStorageEnabled = true
                getSettings().useWideViewPort = false
                getSettings().loadWithOverviewMode = false
                getSettings().allowFileAccess = false
                getSettings().cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setBackgroundColor(if (darkMode) 0xFF0D0D12.toInt() else 0xFFFAFAFA.toInt())
                getSettings().mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                addJavascriptInterface(bridge, "LanReadBridge")
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        // Restore page position after layout (fresh state, not captured)
                        if (latestRestorePage > 0) {
                            view.evaluateJavascript("scrollToPage($latestRestorePage, false)", null)
                        }
                    }
                }
                loadedHtml = html
                loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                webView = this
                onWebViewReady(this)
            }
        },
        update = { view ->
            // Chapter switch / reader-settings change produce new html: reload it.
            // Without this the WebView kept the first chapter forever.
            if (loadedHtml != html) {
                loadedHtml = html
                dragOffset = 0f
                isDragging = false
                view.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
            }
        },
        modifier = modifier
            .offset { IntOffset(dragOffset.roundToInt(), 0) }
            .pointerInput(chapter.order) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragOffset = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        val width = size.width.toFloat()
                        val shouldTurn = abs(dragOffset) > width * 0.12f
                        val turnForward = dragOffset < 0f
                        dragOffset = 0f
                        val now = System.currentTimeMillis()
                        if (shouldTurn && now - lastSwipeTime > 360) {
                            lastSwipeTime = now
                            latestOnTurnPage(turnForward)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = 0f
                    }
                ) { change, dragAmount ->
                    val width = size.width.toFloat()
                    val candidate = dragOffset + dragAmount.x * 0.65f
                    dragOffset = candidate.coerceIn(-width * 0.4f, width * 0.4f)
                }
            }
    )

    // Apply persisted highlights for this chapter once the WebView has loaded and
    // re-apply whenever the highlight list changes.
    LaunchedEffect(chapter.order, highlights, totalPages) {
        if (totalPages > 0) {
            // Wait for initial pagination/layout
            kotlinx.coroutines.delay(600)
            if (highlights.isEmpty()) {
                webView?.evaluateJavascript("clearExistingHighlights()", null)
            } else {
                val anchors = highlights.mapNotNull { hl ->
                    val anchor = ReaderHtmlBuilder.parseAnchor(hl.startPosition) ?: return@mapNotNull null
                    val endAnchor = ReaderHtmlBuilder.parseAnchor(hl.endPosition)
                    val start = anchor.third
                    val end = endAnchor?.third ?: (start + hl.selectedText.length)
                    if (end <= start) null else hl.id to (start to end)
                }.toMap()
                val js = ReaderHtmlBuilder.highlightsToJs(highlights, anchors)
                webView?.evaluateJavascript("applyNativeHighlights($js)", null)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
}