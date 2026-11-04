package com.lanread.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lanread.app.R
import com.lanread.app.ui.theme.AccentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    selected: LibraryFilterType,
    onSelect: (LibraryFilterType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.library_filter_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
            }

            FilterRow(
                label = stringResource(R.string.common_all),
                isSelected = selected is LibraryFilterType.All
            ) { onSelect(LibraryFilterType.All) }

            FilterRow(
                label = stringResource(R.string.library_favorite_title),
                isSelected = selected is LibraryFilterType.Favorites,
                leadingIcon = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color(0xFFFF2D55),
                        modifier = Modifier.size(18.dp)
                    )
                }
            ) { onSelect(LibraryFilterType.Favorites) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                stringResource(R.string.library_filter_reading_status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            ReadingStatus.entries.forEach { status ->
                FilterRow(
                    label = status.displayName(),
                    isSelected = selected is LibraryFilterType.Status && (selected as LibraryFilterType.Status).status == status
                ) { onSelect(LibraryFilterType.Status(status)) }
            }
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    isSelected: Boolean,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingIcon?.invoke()
        Text(label, modifier = Modifier.padding(start = if (leadingIcon != null) 8.dp else 0.dp))
        Spacer(Modifier.weight(1f))
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = AccentBlue)
        }
    }
}

@Composable
fun ReadingStatus.displayName(): String = when (this) {
    ReadingStatus.WANT_TO_READ -> stringResource(R.string.reading_status_want_to_read)
    ReadingStatus.READING -> stringResource(R.string.reading_status_reading)
    ReadingStatus.FINISHED -> stringResource(R.string.reading_status_finished)
    ReadingStatus.PAUSED -> stringResource(R.string.reading_status_paused)
}
