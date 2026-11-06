package com.lanread.app.ui.library

import com.lanread.app.ads.AdMobManager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import coil.compose.AsyncImage
import com.lanread.app.R
import com.lanread.app.data.BookImportService
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.di.AppContainer
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.summary.AiSummaryRoute
import com.lanread.app.ui.theme.AccentBlue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow

class LibraryViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val dao = container.database.libraryDao()

    val uiState = MutableStateFlow(LibraryUiState())

    init {
        // Observe library entries from Room
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.observeLibrary().collect { entries ->
                val cards = entries.map { LibraryCardData(it.item, it.book, it.progress) }
                uiState.value = uiState.value.copy(books = cards)
            }
        }
    }

    fun setSearchText(value: String) {
        uiState.value = uiState.value.copy(searchText = value)
    }

    fun setFilter(filter: LibraryFilterType) {
        uiState.value = uiState.value.copy(selectedFilter = filter)
    }

    suspend fun toggleFavorite(card: LibraryCardData) {
        dao.setFavorite(card.item.id, !card.item.isFavorite)
    }

    suspend fun removeBook(card: LibraryCardData) {
        dao.removeBookCompletely(card.book.id)
        container.bookImportService.deleteBookFile(card.book)
    }

    suspend fun markAccessed(bookId: String) {
        dao.touchAccessed(bookId, System.currentTimeMillis())
    }

    suspend fun readingStatusOnOpen(bookId: String, progress: Double): String {
        val item = dao.findLibraryItemByBookId(bookId) ?: return "want_to_read"
        val newStatus = when {
            progress >= 0.99 -> "finished"
            item.statusRaw == "want_to_read" || item.statusRaw == "paused" -> "reading"
            else -> item.statusRaw
        }
        val now = System.currentTimeMillis()
        if (newStatus != item.statusRaw) {
            dao.setStatusAndAccess(bookId, newStatus, now)
        } else {
            dao.touchAccessed(bookId, now)
        }
        return newStatus
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LibraryViewModel(container) as T
    }
}

@Composable
fun rememberLibraryViewModel(container: AppContainer): LibraryViewModel {
    val owner = LocalViewModelStoreOwner.current ?: error("No ViewModelStoreOwner")
    return viewModel(viewModelStoreOwner = owner, factory = LibraryViewModel.Factory(container))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    appSettings: AppSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.lanread.app.LanReadApp).container
    val viewModel = rememberLibraryViewModel(container)
    val state by viewModel.uiState.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var showingImportSheet by remember { mutableStateOf(false) }
    var showingFilterSheet by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<LibraryCardData?>(null) }
    var removalError by remember { mutableStateOf<String?>(null) }
    var selectedBook by remember { mutableStateOf<LibraryCardData?>(null) }
    var readerTarget by remember { mutableStateOf<Pair<LibraryCardData, Int>?>(null) }
    var skimmingTarget by remember { mutableStateOf<LibraryCardData?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Large title + add book action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.clickable { showingImportSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.library_import_title)
                    )
                }
            }
        }

        // Search + filter bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchText,
                onValueChange = viewModel::setSearchText,
                placeholder = { Text(stringResource(R.string.library_search_placeholder), fontSize = 15.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.clickable { showingFilterSheet = true }) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = stringResource(R.string.library_filter_title),
                        tint = if (state.selectedFilter != LibraryFilterType.All) AccentBlue else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        val filtered = state.filteredBooks
        if (filtered.isEmpty()) {
            EmptyLibraryView(
                onImportTap = { showingImportSheet = true },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filtered, key = { it.item.id }) { card ->
                    BookCard(
                        card = card,
                        onClick = {
                            AdMobManager.preloadSummaryBannerIfNeeded(context, "book_card_tap")
                            selectedBook = card
                        },
                        onSkim = { skimmingTarget = card },
                        onContinueReading = { /* wired below */ },
                        onToggleFavorite = { scope.launch { viewModel.toggleFavorite(card) } },
                        onRemove = { pendingRemoval = card }
                    )
                }
            }
        }
    }



    if (showingImportSheet) {
        ImportBookSheet(
            importService = container.bookImportService,
            onDismiss = { showingImportSheet = false },
            onImported = { book ->
                showingImportSheet = false
                selectedBook = null
            }
        )
    }

    if (showingFilterSheet) {
        FilterSheet(
            selected = state.selectedFilter,
            onSelect = { viewModel.setFilter(it); showingFilterSheet = false },
            onDismiss = { showingFilterSheet = false }
        )
    }

    pendingRemoval?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text(stringResource(R.string.library_remove_confirm_title)) },
            text = {
                Text(
                    stringResource(R.string.library_remove_confirm_message, card.book.title) + "\n\n" +
                        stringResource(R.string.library_remove_confirm_notion_note)
                )
            },
            confirmButton = {
                TextButtonWrapper(
                    text = stringResource(R.string.library_remove_confirm_action),
                    color = MaterialTheme.colorScheme.error
                ) {
                    pendingRemoval = null
                    scope.launch { viewModel.removeBook(card) }
                }
            },
            dismissButton = {
                TextButtonWrapper(text = stringResource(R.string.common_cancel), color = MaterialTheme.colorScheme.primary) {
                    pendingRemoval = null
                }
            }
        )
    }

    removalError?.let { message ->
        AlertDialog(
            onDismissRequest = { removalError = null },
            title = { Text(stringResource(R.string.library_remove_failure_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButtonWrapper(text = stringResource(R.string.common_confirm), color = MaterialTheme.colorScheme.primary) {
                    removalError = null
                }
            }
        )
    }

    selectedBook?.let { card ->
        AiSummaryRoute(
            card = card,
            appSettings = appSettings,
            onClose = { selectedBook = null },
            onOpenReader = {
                selectedBook = null
                readerTarget = card to -1
            },
            onOpenSkimming = {
                selectedBook = null
                skimmingTarget = card
            }
        )
    }

    readerTarget?.let { (card, chapterIndex) ->
        ReaderOverlay(
            card = card,
            initialChapterIndex = chapterIndex,
            appSettings = appSettings,
            container = container,
            onDismiss = { readerTarget = null }
        )
    }

    skimmingTarget?.let { card ->
        SkimmingOverlay(
            card = card,
            appSettings = appSettings,
            container = container,
            onClose = { skimmingTarget = null },
            onStartChapterReading = { chapterIndex ->
                skimmingTarget = null
                readerTarget = card to chapterIndex
            },
            onResumeFullReading = {
                skimmingTarget = null
                readerTarget = card to -1
            }
        )
    }
}

@Composable
private fun ReaderOverlay(
    card: LibraryCardData,
    initialChapterIndex: Int,
    appSettings: AppSettingsViewModel,
    container: AppContainer,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.lanread.app.ui.reader.ReaderScreen(
            book = card.book,
            initialChapterIndex = initialChapterIndex,
            appSettings = appSettings,
            container = container,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SkimmingOverlay(
    card: LibraryCardData,
    appSettings: AppSettingsViewModel,
    container: AppContainer,
    onClose: () -> Unit,
    onStartChapterReading: (Int) -> Unit,
    onResumeFullReading: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        com.lanread.app.ui.skimming.SkimmingScreen(
            book = card.book,
            container = container,
            appSettings = appSettings,
            onClose = onClose,
            onStartChapterReading = onStartChapterReading,
            onResumeFullReading = onResumeFullReading
        )
    }
}

@Composable
private fun TextButtonWrapper(text: String, color: Color, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text, color = color)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    card: LibraryCardData,
    onClick: () -> Unit,
    onSkim: () -> Unit,
    onContinueReading: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.4f)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = onClick, onLongClick = { menuExpanded = true })
        ) {
            // Cover
            if (card.book.coverImageData != null) {
                AsyncImage(
                    model = card.book.coverImageData,
                    contentDescription = card.book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color.Blue.copy(alpha = 0.6f), Color(0xFF8A2BE2).copy(alpha = 0.8f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            card.book.title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // Overlays
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    if (card.progressPercentage > 0) {
                        ProgressBadge(card.progressPercentage)
                    }
                    Spacer(Modifier.weight(1f))
                    StatusBadge(card.status)
                }
                Spacer(Modifier.weight(1f))
                Row {
                    if (card.item.isFavorite) {
                        FavoriteBadge()
                    }
                    Spacer(Modifier.weight(1f))
                }
            }

            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.skimming_mode_title)) },
                    onClick = { menuExpanded = false; onSkim() },
                    leadingIcon = { Icon(Icons.Outlined.AutoGraph, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_book_info_title)) },
                    onClick = { menuExpanded = false },
                    leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (card.item.isFavorite) stringResource(R.string.library_favorite_remove) else stringResource(R.string.library_favorite_add)) },
                    onClick = { menuExpanded = false; onToggleFavorite() },
                    leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null) }
                )
                androidx.compose.material3.HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.library_remove_action), color = MaterialTheme.colorScheme.error) },
                    onClick = { menuExpanded = false; onRemove() },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: ReadingStatus) {
    val (labelRes, color) = when (status) {
        ReadingStatus.WANT_TO_READ -> R.string.reading_status_want_to_read to Color.Gray
        ReadingStatus.READING -> R.string.reading_status_reading to AccentBlue
        ReadingStatus.FINISHED -> R.string.reading_status_finished to Color(0xFF34C759)
        ReadingStatus.PAUSED -> R.string.reading_status_paused to Color(0xFFFF9500)
    }
    Text(
        text = stringResource(labelRes),
        color = Color.White,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ProgressBadge(progress: Double) {
    val color = when {
        progress < 0.3 -> Color(0xFFFF9500)
        progress < 0.7 -> AccentBlue
        progress < 1.0 -> Color(0xFF8A2BE2)
        else -> Color(0xFF34C759)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            Icons.Filled.BarChart,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            String.format("%.0f%%", progress * 100),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FavoriteBadge() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color(0xFFFF2D55).copy(alpha = 0.9f), CircleShape)
            .size(24.dp)
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun EmptyLibraryView(onImportTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.CollectionsBookmark,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.library_empty_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onImportTap,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.library_import_title), fontWeight = FontWeight.SemiBold)
        }
    }
}
