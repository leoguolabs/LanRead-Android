package com.lanread.app.ui.reader

import com.lanread.app.ads.AdMobBannerView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.ui.library.parseHexColor
import com.lanread.app.ui.theme.AccentBlue

/** Default highlight color (iOS system yellow). */
const val DEFAULT_HIGHLIGHT_HEX = "FFE38F"

/**
 * Floating selection toolbar shown above/below the selected text.
 * Mirrors iOS selectionToolbar: 高亮标记 / 添加笔记 / AI 解释 / AI 翻译 / 复制 / 问问 AI.
 */
@Composable
fun SelectionToolbar(
    selection: SelectionPayload,
    onHighlight: () -> Unit,
    onNote: () -> Unit,
    onExplain: () -> Unit,
    onTranslate: () -> Unit,
    onCopy: () -> Unit,
    onAsk: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            // Action capsules
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp)
            ) {
                SelectionAction(
                    icon = { Icon(Icons.Filled.Highlight, contentDescription = null, tint = Color(0xFFFFD60A), modifier = Modifier.size(16.dp)) },
                    label = stringResource(R.string.reader_highlight_action),
                    onClick = onHighlight
                )
                SelectionAction(
                    icon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp)) },
                    label = stringResource(R.string.reader_note_add),
                    onClick = onNote
                )
                SelectionAction(
                    icon = { Icon(Icons.Filled.Psychology, contentDescription = null, tint = Color(0xFFAF52DE), modifier = Modifier.size(16.dp)) },
                    label = stringResource(R.string.reader_ai_explain),
                    onClick = onExplain
                )
                SelectionAction(
                    icon = { Icon(Icons.Filled.Language, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(16.dp)) },
                    label = stringResource(R.string.reader_ai_translate),
                    onClick = onTranslate
                )
                SelectionAction(
                    icon = { Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) },
                    label = stringResource(R.string.common_copy),
                    onClick = onCopy
                )
            }

            Spacer(Modifier.height(8.dp))

            // Ask-AI bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
                    .clickable(onClick = onAsk)
                    .padding(horizontal = 12.dp, vertical = 11.dp)
            ) {
                Icon(Icons.Filled.Psychology, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.reader_ai_question_placeholder),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .background(AccentBlue, CircleShape)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectionAction(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Highlight detail sheet: quoted content, note, AI actions, delete.
 * Mirrors iOS highlightActionSheet (高亮详情).
 */
@Composable
fun HighlightDetailSheet(
    highlight: com.lanread.app.data.local.entity.HighlightEntity,
    onExplain: () -> Unit,
    onTranslate: () -> Unit,
    onAsk: () -> Unit,
    onCopy: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.highlight_action_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Quoted highlight content
            Text(
                highlight.selectedText,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        parseHexColor(highlight.colorHex).copy(alpha = 0.48f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Note row
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                if (highlight.note != null) {
                    Text(
                        highlight.note ?: "",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onEditNote)
                    )
                } else {
                    Text(
                        stringResource(R.string.highlight_action_no_note),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onEditNote)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // AI actions grid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DetailAction(stringResource(R.string.reader_ai_explain), Icons.Filled.Psychology, onExplain, Modifier.weight(1f))
                DetailAction(stringResource(R.string.reader_ai_translate), Icons.Filled.Language, onTranslate, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DetailAction(stringResource(R.string.reader_ai_ask), Icons.Filled.Psychology, onAsk, Modifier.weight(1f))
                DetailAction(stringResource(R.string.common_copy), Icons.Filled.ContentCopy, onCopy, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                DetailAction(
                    if (highlight.note == null) stringResource(R.string.reader_note_add)
                    else stringResource(R.string.highlight_list_edit_note),
                    Icons.Filled.Edit, onEditNote, Modifier.weight(1f)
                )
                DetailAction(stringResource(R.string.highlight_action_delete_highlight), Icons.Filled.Delete, onDelete, Modifier.weight(1f), danger = true)
            }

            Spacer(Modifier.height(16.dp))
            AdMobBannerView(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DetailAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(40.dp)
            .background(
                if (danger) MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 13.sp,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Note editor dialog for a highlight (mirrors iOS noteEditorSheet). */
@Composable
fun NoteEditorDialog(
    quotedText: String,
    initialNote: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { androidx.compose.runtime.mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_note_add)) },
        text = {
            Column {
                Text(
                    quotedText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    maxLines = 4
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft.trim()) }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}

/** Ask-AI composer dialog (mirrors iOS selectionAskComposerSheet). */
@Composable
fun AskComposerDialog(
    quotedText: String,
    question: String,
    onQuestionChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_ai_ask)) },
        text = {
            Column {
                Text(
                    quotedText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    maxLines = 5
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = question,
                    onValueChange = onQuestionChange,
                    placeholder = { Text(stringResource(R.string.reader_ai_question_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSend, enabled = question.trim().isNotEmpty()) {
                Text(stringResource(R.string.reader_ai_question_send))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}

/** AI response sheet with loading and content (mirrors iOS aiResponseSheet). */
@Composable
fun AiResponseDialog(
    title: String,
    content: String,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (loading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.ai_generating), fontSize = 14.sp)
                }
            } else {
                Text(
                    content,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}
