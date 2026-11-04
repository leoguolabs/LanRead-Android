package com.lanread.app.ui.library

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.data.local.entity.BookmarkEntity
import com.lanread.app.data.local.entity.HighlightEntity
import com.lanread.app.di.AppContainer
import com.lanread.app.ui.theme.AccentBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BookmarkColorOption(val nameKey: Int, val hex: String)

val bookmarkColorOptions = listOf(
    BookmarkColorOption(R.string.color_option_white, "FFFFFF"),
    BookmarkColorOption(R.string.color_option_mint, "D6EFDE"),
    BookmarkColorOption(R.string.color_option_blue, "D4E7FF"),
    BookmarkColorOption(R.string.color_option_peach, "FFDCC8")
)

val highlightColorOptions = listOf(
    BookmarkColorOption(R.string.color_option_yellow, "FFE38F"),
    BookmarkColorOption(R.string.color_option_green, "D6EFD6"),
    BookmarkColorOption(R.string.color_option_blue, "D4E7FF"),
    BookmarkColorOption(R.string.color_option_pink, "FFD3E5")
)

@Composable
fun BookmarkListSheet(
    book: BookEntity,
    container: AppContainer,
    onSelect: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var bookmarks by remember { mutableStateOf<List<BookmarkEntity>>(emptyList()) }

    suspend fun reload() {
        bookmarks = container.database.bookmarkDao().byBook(book.id)
    }

    LaunchedEffect(book.id) { reload() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.bookmark_list_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
            }

            if (bookmarks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.BookmarkBorder, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.bookmark_list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(book.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    scope.launch(Dispatchers.IO) {
                                        container.database.bookmarkDao().delete(bookmark.id)
                                        reload()
                                    }
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    contentAlignment = Alignment.CenterEnd,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.error)
                                        .padding(end = 20.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.White)
                                }
                            }
                        ) {
                            val color = parseHexColor(bookmark.colorHex ?: "FFFFFF")
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(bookmark.chapterIndex, bookmark.pageIndex)
                                        onDismiss()
                                    }
                                    .background(color.copy(alpha = 0.34f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).background(color, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        bookmark.chapterTitle
                                            ?: stringResource(R.string.bookmark_chapter_format, bookmark.chapterIndex + 1),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 2
                                    )
                                    Text(
                                        stringResource(R.string.bookmark_page_format, bookmark.pageIndex + 1),
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .background(color.copy(alpha = 0.56f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    Text(
                                        stringResource(R.string.bookmark_chapter_format, bookmark.chapterIndex + 1) + " · " +
                                            stringResource(R.string.bookmark_page_format, bookmark.pageIndex + 1),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(formatDate(bookmark.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun HighlightListSheet(
    book: BookEntity,
    container: AppContainer,
    onSelect: (Int, Int, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var highlights by remember { mutableStateOf<List<HighlightEntity>>(emptyList()) }
    var editingHighlight by remember { mutableStateOf<HighlightEntity?>(null) }
    var noteDraft by remember { mutableStateOf("") }
    var deletingHighlight by remember { mutableStateOf<HighlightEntity?>(null) }

    suspend fun reload() {
        highlights = container.database.highlightDao().byBook(book.id)
    }

    LaunchedEffect(book.id) { reload() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.highlight_list_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_back)) }
            }

            if (highlights.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Highlight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.highlight_list_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(book.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.height(460.dp)) {
                    items(highlights, key = { it.id }) { highlight ->
                        val color = parseHexColor(highlight.colorHex)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).background(color, CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    highlight.chapter ?: stringResource(R.string.highlight_list_unknown_chapter),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(formatDate(highlight.updatedAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Text(
                                highlight.selectedText,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val anchor = com.lanread.app.ui.reader.ReaderHtmlBuilder.parseAnchor(highlight.startPosition)
                                        if (anchor != null && !com.lanread.app.ui.reader.ReaderHtmlBuilder.isMergedAnchor(highlight.startPosition)) {
                                            onSelect(anchor.first, anchor.second, anchor.third)
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            )

                            if (highlight.note != null) {
                                Row {
                                    Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(highlight.note ?: "", fontSize = 14.sp)
                                }
                            } else {
                                Text(
                                    stringResource(R.string.highlight_action_no_note),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
                                TextButton(onClick = {
                                    noteDraft = highlight.note ?: ""
                                    editingHighlight = highlight
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (highlight.note == null) stringResource(R.string.reader_note_add)
                                        else stringResource(R.string.highlight_list_edit_note),
                                        fontSize = 12.sp
                                    )
                                }
                                TextButton(onClick = { deletingHighlight = highlight }) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.highlight_list_delete), fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    editingHighlight?.let { highlight ->
        AlertDialog(
            onDismissRequest = { editingHighlight = null },
            title = {
                Text(
                    if (highlight.note == null) stringResource(R.string.reader_note_add)
                    else stringResource(R.string.highlight_list_edit_note)
                )
            },
            text = {
                Column {
                    Text(
                        highlight.selectedText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        maxLines = 4
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = noteDraft.trim()
                    scope.launch(Dispatchers.IO) {
                        container.database.highlightDao().updateNote(
                            highlight.id,
                            trimmed.ifEmpty { null },
                            System.currentTimeMillis()
                        )
                        withContext(Dispatchers.Main) {
                            editingHighlight = null
                            reload()
                        }
                    }
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingHighlight = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    deletingHighlight?.let { highlight ->
        AlertDialog(
            onDismissRequest = { deletingHighlight = null },
            title = { Text(stringResource(R.string.highlight_action_delete_highlight_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        container.database.highlightDao().delete(highlight.id)
                        withContext(Dispatchers.Main) {
                            deletingHighlight = null
                            reload()
                        }
                    }
                }) { Text(stringResource(R.string.highlight_list_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingHighlight = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
    } catch (_: Exception) {
        Color(0xFFFFE38F)
    }
}

fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("yyyy/M/d HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
