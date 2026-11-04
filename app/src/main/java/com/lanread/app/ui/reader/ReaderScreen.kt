package com.lanread.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import com.lanread.app.R
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.data.local.entity.BookmarkEntity
import com.lanread.app.data.local.entity.HighlightEntity
import com.lanread.app.di.AppContainer
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.effectiveDarkMode
import com.lanread.app.ui.theme.AccentBlue
import com.lanread.app.util.EPubParser
import com.lanread.app.util.ParsedChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.UUID

/**
 * Reader screen: WebView with horizontal CSS-column pagination, tap/swipe page turns,
 * toolbar overlay, TOC sheet, reader settings sheet, bookmarks and highlights.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReaderScreen(
    book: BookEntity,
    initialChapterIndex: Int,
    appSettings: AppSettingsViewModel,
    container: AppContainer,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val darkMode = appSettings.effectiveDarkMode()
    val settings = appSettings.state

    var chapters by remember { mutableStateOf<List<ParsedChapter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var currentChapterIndex by remember { mutableStateOf(initialChapterIndex) }
    var totalPages by remember { mutableStateOf(1) }
    var currentPage by remember { mutableStateOf(0) }
    var showingToolbar by remember { mutableStateOf(false) }
    var showingToc by remember { mutableStateOf(false) }
    var showingReaderSettings by remember { mutableStateOf(false) }
    var hintMessage by remember { mutableStateOf<String?>(null) }
    var currentBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showingBookmarksList by remember { mutableStateOf(false) }
    var showingHighlightsList by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf<SelectionPayload?>(null) }
    var chapterHighlights by remember { mutableStateOf<List<HighlightEntity>>(emptyList()) }
    var highlightDetail by remember { mutableStateOf<HighlightEntity?>(null) }
    var showingNoteEditor by remember { mutableStateOf(false) }
    var noteDraft by remember { mutableStateOf("") }
    var noteTargetHighlightId by remember { mutableStateOf<String?>(null) }
    var showingAskComposer by remember { mutableStateOf(false) }
    var askQuestion by remember { mutableStateOf("") }
    var aiResponseTitle by remember { mutableStateOf("") }
    var aiResponseContent by remember { mutableStateOf("") }
    var aiLoading by remember { mutableStateOf(false) }
    var showingAiResponse by remember { mutableStateOf(false) }
    var pendingSelectionForAi by remember { mutableStateOf<SelectionPayload?>(null) }
    var pendingHighlightForAi by remember { mutableStateOf<HighlightEntity?>(null) }

    val context = LocalContext.current

    fun showHint(message: String) {
        hintMessage = message
        scope.launch {
            kotlinx.coroutines.delay(1600)
            if (hintMessage == message) hintMessage = null
        }
    }
    // AI 调用暂缓：仅提示（用户要求暂时不接 AI 功能）
    fun startAiAction(action: String) {
        showHint(context.getString(R.string.ai_error_endpoint))
    }

    // Load book chapters
    LaunchedEffect(book.id) {
        withContext(Dispatchers.IO) {
            try {
                val file = container.bookImportService.locateBookFile(book)
                if (file.exists()) {
                    val metadata = EPubParser.parse(file)
                    chapters = metadata.chapters
                } else {
                    loadError = context.getString(R.string.reader_error_file_missing)
                }
            } catch (e: Exception) {
                loadError = e.message ?: "load failed"
            } finally {
                isLoading = false
            }
        }
        // Restore saved position when opening without explicit location
        if (initialChapterIndex == -1) {
            val progress = container.database.libraryDao().findProgressByBookId(book.id)
            val position = progress?.currentPosition
            if (position != null) {
                try {
                    val obj = org.json.JSONObject(position)
                    currentChapterIndex = obj.optInt("chapterIndex", 0)
                    currentPage = obj.optInt("pageIndex", 0)
                } catch (_: Exception) {
                }
            }
        } else {
            currentChapterIndex = initialChapterIndex
        }
        // Status transition: want_to_read/paused -> reading
        scope.launch(Dispatchers.IO) {
            val item = container.database.libraryDao().findLibraryItemByBookId(book.id)
            if (item != null && (item.statusRaw == "want_to_read" || item.statusRaw == "paused")) {
                container.database.libraryDao().setStatusAndAccess(book.id, "reading", System.currentTimeMillis())
            } else {
                container.database.libraryDao().touchAccessed(book.id, System.currentTimeMillis())
            }
        }
    }

    // Save progress on chapter/page change and on dispose
    fun saveProgress() {
        scope.launch(Dispatchers.IO) {
            val dao = container.database.libraryDao()
            val existing = dao.findProgressByBookId(book.id) ?: return@launch
            val bookTotalPages = maxOf(book.totalPages, 1)
            val chapterTotal = maxOf(chapters.size, 1)
            val fallbackPerPage = maxOf(bookTotalPages / chapterTotal, 1)
            var globalPage = currentPage + 1
            for (i in 0 until currentChapterIndex) {
                globalPage += fallbackPerPage
            }
            val position = org.json.JSONObject()
                .put("chapterIndex", currentChapterIndex)
                .put("pageIndex", currentPage)
                .put("totalPages", totalPages)
                .put("globalPage", globalPage)
                .put("bookTotalPages", bookTotalPages)
                .toString()
            val percentage = (globalPage.toDouble() / bookTotalPages).coerceIn(0.0, 1.0)
            dao.updateProgress(
                existing.copy(
                    currentPage = currentChapterIndex,
                    currentPosition = position,
                    progressPercentage = percentage,
                    lastReadAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            // >= 99% => finished
            val item = dao.findLibraryItemByBookId(book.id)
            if (item != null && percentage >= 0.99 && item.statusRaw != "finished") {
                dao.setStatusByBookId(book.id, "finished")
            }
        }
    }

    DisposableEffect(book.id) {
        onDispose { saveProgress() }
    }

    // Load highlights for the current chapter
    LaunchedEffect(currentChapterIndex, chapters) {
        if (chapters.isEmpty()) return@LaunchedEffect
        kotlinx.coroutines.withContext(Dispatchers.IO) {
            chapterHighlights = container.database.highlightDao().byBook(book.id)
        }
    }

    // Bookmark state for current location
    LaunchedEffect(currentChapterIndex, currentPage, chapters) {
        if (chapters.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            currentBookmark = container.database.bookmarkDao()
                .findAtLocation(book.id, currentChapterIndex, currentPage)
        }
    }

    val readerBg = if (darkMode) Color(0xFF0D0D12) else Color(0xFFFAFAFA)

    Box(modifier = Modifier.fillMaxSize().background(readerBg)) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.reader_loading_book),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            loadError != null -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(50.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.reader_load_failed), fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(loadError ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.common_back),
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .background(AccentBlue, RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                    )
                }
            }
            chapters.isNotEmpty() -> {
                val chapter = chapters[currentChapterIndex.coerceIn(0, chapters.size - 1)]
                ReaderPageView(
                    book = book,
                    chapter = chapter,
                    chapterIndex = currentChapterIndex,
                    highlights = chapterHighlights,
                    appSettings = appSettings,
                    container = container,
                    darkMode = darkMode,
                    showingToolbar = showingToolbar,
                    currentPage = currentPage,
                    totalPages = totalPages,
                    onWebViewReady = { webView = it },
                    onPageSelected = { page ->
                        currentPage = page
                        saveProgress()
                    },
                    onTotalPages = { total ->
                        totalPages = total
                        saveProgress()
                    },
                    onToolbarToggle = { showingToolbar = !showingToolbar },
                    onSelection = { payload -> selectedText = payload },
                    onHighlightTap = { id, _ ->
                        val target = chapterHighlights.firstOrNull { it.id == id }
                        if (target != null) highlightDetail = target
                    },
                    onTurnPage = { forward ->
                        if (showingToolbar) showingToolbar = false
                        // Ask the WebView for the authoritative current page (JS tracks it),
                        // then turn relative to it. Avoids stale Compose state.
                        webView?.evaluateJavascript("window.getCurrentPage ? getCurrentPage() : 0") { result ->
                            val jsPage = result?.trim()?.removePrefix("\"")?.removeSuffix("\"")?.toIntOrNull() ?: currentPage
                            val target = if (forward) jsPage + 1 else jsPage - 1
                            if (target in 0 until totalPages) {
                                webView?.evaluateJavascript("scrollToPage($target, true)", null)
                                currentPage = target
                            } else if (forward && currentChapterIndex < chapters.size - 1) {
                                currentChapterIndex += 1
                                currentPage = 0
                            } else if (!forward && currentChapterIndex > 0) {
                                currentChapterIndex -= 1
                                currentPage = 0
                            } else {
                                showHint(
                                    context.getString(
                                        if (forward) R.string.reader_page_turn_reached_end
                                        else R.string.reader_page_turn_reached_start
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 36.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                )

                // Inline chapter title
                if (!showingToolbar) {
                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                        Text(
                            chapter.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(
                                horizontal = maxOf(20f, settings.pageMargins.toFloat()).dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }

                // Page indicator
                if (totalPages > 1 && !showingToolbar) {
                    Text(
                        "${currentPage + 1} / $totalPages",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 8.dp)
                    )
                }

                // Top toolbar
                androidx.compose.animation.AnimatedVisibility(
                    visible = showingToolbar,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ReaderTopToolbar(
                        bookTitle = book.title,
                        chapterTitle = chapter.title,
                        onBack = {
                            saveProgress()
                            onDismiss()
                        },
                        onToc = { showingToc = true },
                        onSettings = { showingReaderSettings = true }
                    )
                }

                // Bottom toolbar
                androidx.compose.animation.AnimatedVisibility(
                    visible = showingToolbar,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ReaderBottomToolbar(
                        chapterIndex = currentChapterIndex,
                        chapterCount = chapters.size,
                        isBookmarked = currentBookmark != null,
                        onToggleBookmark = {
                            scope.launch(Dispatchers.IO) {
                                val existing = container.database.bookmarkDao()
                                    .findAtLocation(book.id, currentChapterIndex, currentPage)
                                if (existing != null) {
                                    container.database.bookmarkDao().delete(existing.id)
                                } else {
                                    container.database.bookmarkDao().insert(
                                        BookmarkEntity(
                                            id = UUID.randomUUID().toString(),
                                            bookId = book.id,
                                            chapterIndex = currentChapterIndex,
                                            pageIndex = currentPage,
                                            chapterTitle = chapter.title,
                                            createdAt = System.currentTimeMillis(),
                                            colorHex = "FFFFFF"
                                        )
                                    )
                                }
                            }
                        },
                        onHighlights = { showingHighlightsList = true },
                        onBookmarks = { showingBookmarksList = true }
                    )
                }
            }
        }

        // Floating selection toolbar
        selectedText?.let { sel ->
            Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
                SelectionToolbar(
                    selection = sel,
                    onHighlight = {
                        val payload = sel
                        selectedText = null
                        webView?.evaluateJavascript("if (window.getSelection) window.getSelection().removeAllRanges(); if (window.clampVisualViewport) clampVisualViewport();", null)
                        scope.launch(Dispatchers.IO) {
                            val startAnchor = ReaderHtmlBuilder.buildAnchor("", payload.start, currentChapterIndex, payload.pageIndex)
                            val endAnchor = ReaderHtmlBuilder.buildAnchor("", payload.end, currentChapterIndex, payload.pageIndex)
                            container.database.highlightDao().insert(
                                HighlightEntity(
                                    id = java.util.UUID.randomUUID().toString(),
                                    bookId = book.id,
                                    selectedText = payload.text,
                                    startPosition = startAnchor,
                                    endPosition = endAnchor,
                                    chapter = chapters.getOrNull(currentChapterIndex)?.title,
                                    pageNumber = payload.pageIndex,
                                    colorHex = DEFAULT_HIGHLIGHT_HEX,
                                    note = null,
                                    createdAt = System.currentTimeMillis(),
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            chapterHighlights = container.database.highlightDao().byBook(book.id)
                        }
                        scope.launch {
                            webView?.evaluateJavascript(
                                "applyHighlightForSelection('#' + '$DEFAULT_HIGHLIGHT_HEX'); if (window.getSelection) window.getSelection().removeAllRanges(); if (window.clampVisualViewport) clampVisualViewport();", null
                            )
                        }
                    },
                    onNote = {
                        noteDraft = ""
                        noteTargetHighlightId = null
                        showingNoteEditor = true
                    },
                    onExplain = { startAiAction("explain") },
                    onTranslate = { startAiAction("translate") },
                    onCopy = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("selection", sel.text)
                        )
                        showHint(context.getString(R.string.reader_copy_success))
                        selectedText = null
                        webView?.evaluateJavascript("if (window.getSelection) window.getSelection().removeAllRanges(); if (window.clampVisualViewport) clampVisualViewport();", null)
                    },
                    onAsk = {
                        pendingSelectionForAi = sel
                        askQuestion = ""
                        showingAskComposer = true
                    },
                    onDismiss = {
                        selectedText = null
                        webView?.evaluateJavascript("if (window.getSelection) window.getSelection().removeAllRanges(); if (window.clampVisualViewport) clampVisualViewport();", null)
                    }
                )
            }
        }

        if (showingBookmarksList) {
            com.lanread.app.ui.library.BookmarkListSheet(
                book = book,
                container = container,
                onSelect = { chapterIdx, pageIdx ->
                    showingBookmarksList = false
                    currentChapterIndex = chapterIdx
                    currentPage = pageIdx
                },
                onDismiss = { showingBookmarksList = false }
            )
        }

        if (showingHighlightsList) {
            com.lanread.app.ui.library.HighlightListSheet(
                book = book,
                container = container,
                onSelect = { chapterIdx, pageIdx, _ ->
                    showingHighlightsList = false
                    currentChapterIndex = chapterIdx
                    currentPage = pageIdx
                },
                onDismiss = { showingHighlightsList = false }
            )
        }

        // Note editor (new note from selection, or edit existing highlight note)
        if (showingNoteEditor) {
            val quoted = noteTargetHighlightId?.let { id ->
                chapterHighlights.firstOrNull { it.id == id }?.selectedText
            } ?: selectedText?.text ?: ""
            val initial = noteTargetHighlightId?.let { id ->
                chapterHighlights.firstOrNull { it.id == id }?.note
            } ?: ""
            NoteEditorDialog(
                quotedText = quoted,
                initialNote = initial,
                onSave = { note ->
                    showingNoteEditor = false
                    val text = note
                    scope.launch(Dispatchers.IO) {
                        if (noteTargetHighlightId != null) {
                            container.database.highlightDao().updateNote(
                                noteTargetHighlightId!!, text.ifEmpty { null }, System.currentTimeMillis()
                            )
                        } else {
                            val payload = selectedText
                            if (payload != null) {
                                val id = java.util.UUID.randomUUID().toString()
                                container.database.highlightDao().insert(
                                    HighlightEntity(
                                        id = id,
                                        bookId = book.id,
                                        selectedText = payload.text,
                                        startPosition = ReaderHtmlBuilder.buildAnchor("", payload.start, currentChapterIndex, payload.pageIndex),
                                        endPosition = ReaderHtmlBuilder.buildAnchor("", payload.end, currentChapterIndex, payload.pageIndex),
                                        chapter = chapters.getOrNull(currentChapterIndex)?.title,
                                        pageNumber = payload.pageIndex,
                                        colorHex = DEFAULT_HIGHLIGHT_HEX,
                                        note = text.ifEmpty { null },
                                        createdAt = System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    webView?.evaluateJavascript("applyHighlightForSelection('#$DEFAULT_HIGHLIGHT_HEX')", null)
                                    showHint(context.getString(R.string.reader_highlight_saved_with_note))
                                }
                            }
                        }
                        chapterHighlights = container.database.highlightDao().byBook(book.id)
                        selectedText = null
                        noteTargetHighlightId = null
                    }
                },
                onDismiss = { showingNoteEditor = false }
            )
        }

        // Highlight detail sheet
        highlightDetail?.let { hl ->
            HighlightDetailSheet(
                highlight = hl,
                onExplain = { highlightDetail = null; startAiAction("explain") },
                onTranslate = { highlightDetail = null; startAiAction("translate") },
                onAsk = { pendingHighlightForAi = hl; highlightDetail = null; askQuestion = ""; showingAskComposer = true },
                onCopy = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("highlight", hl.selectedText))
                    showHint(context.getString(R.string.reader_copy_success))
                    highlightDetail = null
                },
                onEditNote = {
                    noteDraft = hl.note ?: ""
                    noteTargetHighlightId = hl.id
                    highlightDetail = null
                    showingNoteEditor = true
                },
                onDelete = {
                    val id = hl.id
                    highlightDetail = null
                    webView?.evaluateJavascript("clearExistingHighlights()", null)
                    scope.launch(Dispatchers.IO) {
                        container.database.highlightDao().delete(id)
                        chapterHighlights = container.database.highlightDao().byBook(book.id)
                    }
                },
                onDismiss = { highlightDetail = null }
            )
        }

        // Ask composer (selection or highlight) - AI 调用暂缓，仅可输入后关闭
        if (showingAskComposer) {
            val quoted = pendingHighlightForAi?.selectedText ?: pendingSelectionForAi?.text ?: ""
            AskComposerDialog(
                quotedText = quoted,
                question = askQuestion,
                onQuestionChange = { askQuestion = it },
                onSend = {
                    showingAskComposer = false
                    startAiAction("ask")
                },
                onDismiss = { showingAskComposer = false; pendingSelectionForAi = null; pendingHighlightForAi = null }
            )
        }

        // AI response
        if (showingAiResponse) {
            AiResponseDialog(
                title = aiResponseTitle,
                content = aiResponseContent,
                loading = aiLoading,
                onDismiss = { showingAiResponse = false }
            )
        }

        // Hint toast
        hintMessage?.let { message ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }

    if (showingToc) {
        TocSheet(
            chapters = chapters,
            currentIndex = currentChapterIndex,
            onSelect = { index ->
                showingToc = false
                currentChapterIndex = index
                currentPage = 0
            },
            onDismiss = { showingToc = false }
        )
    }

    if (showingReaderSettings) {
        ReaderSettingsSheet(
            appSettings = appSettings,
            onDismiss = { showingReaderSettings = false }
        )
    }
}

@Composable
fun ReaderTopToolbar(
    bookTitle: String,
    chapterTitle: String,
    onBack: () -> Unit,
    onToc: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable(onClick = onBack)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(bookTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(chapterTitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable(onClick = onToc)
            ) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.reader_toc_title), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable(onClick = onSettings)
            ) {
                Icon(Icons.Filled.FormatSize, contentDescription = stringResource(R.string.reading_settings_title), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ReaderBottomToolbar(
    chapterIndex: Int,
    chapterCount: Int,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    onHighlights: () -> Unit,
    onBookmarks: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 16.dp)) {
            if (chapterCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${chapterIndex + 1}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
                    Box(modifier = Modifier.weight(1f).height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((chapterIndex + 1).toFloat() / chapterCount)
                                .height(4.dp)
                                .background(AccentBlue, RoundedCornerShape(2.dp))
                        )
                    }
                    Text("$chapterCount", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            if (isBookmarked) AccentBlue.copy(alpha = 0.12f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onToggleBookmark)
                ) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = stringResource(R.string.bookmark_list_title),
                        tint = if (isBookmarked) AccentBlue else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(onClick = onHighlights)
                ) {
                    Icon(Icons.Filled.Highlight, contentDescription = stringResource(R.string.highlight_list_title), modifier = Modifier.size(20.dp))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clickable(onClick = onBookmarks)
                ) {
                    Icon(Icons.Filled.CollectionsBookmark, contentDescription = stringResource(R.string.bookmark_list_title), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun TocSheet(
    chapters: List<ParsedChapter>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.reader_toc_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
            }
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.height(480.dp)) {
                itemsIndexed(chapters) { index, chapter ->
                    val isCurrent = index == currentIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isCurrent) AccentBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                        ) {
                            Text(
                                "${index + 1}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                chapter.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCurrent) {
                                Text(
                                    stringResource(R.string.reader_current_chapter),
                                    fontSize = 12.sp,
                                    color = AccentBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderSettingsSheet(
    appSettings: AppSettingsViewModel,
    onDismiss: () -> Unit
) {
    val state = appSettings.state
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.reading_settings_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
            }

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.settings_font_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.lanread.app.data.settings.ReadingFontSizeOption.entries.forEach { option ->
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
                                    com.lanread.app.data.settings.ReadingFontSizeOption.SMALL -> R.string.settings_font_size_small
                                    com.lanread.app.data.settings.ReadingFontSizeOption.MEDIUM -> R.string.settings_font_size_medium
                                    com.lanread.app.data.settings.ReadingFontSizeOption.LARGE -> R.string.settings_font_size_large
                                    com.lanread.app.data.settings.ReadingFontSizeOption.EXTRA_LARGE -> R.string.settings_font_size_extra_large
                                }
                            ),
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
            Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.lanread.app.data.settings.AppThemeMode.entries.forEach { mode ->
                    val selected = state.theme == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(
                                if (selected) AccentBlue else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { appSettings.setTheme(mode) }
                    ) {
                        Text(
                            stringResource(
                                when (mode) {
                                    com.lanread.app.data.settings.AppThemeMode.SYSTEM -> R.string.settings_theme_mode_system
                                    com.lanread.app.data.settings.AppThemeMode.LIGHT -> R.string.settings_theme_mode_always_light
                                    com.lanread.app.data.settings.AppThemeMode.DARK -> R.string.settings_theme_mode_always_dark
                                }
                            ),
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
