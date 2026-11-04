package com.lanread.app.ui.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.data.BookImportService
import com.lanread.app.data.ImportException
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.ui.theme.AccentBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ImportBookSheet(
    importService: BookImportService,
    onDismiss: () -> Unit,
    onImported: (BookEntity) -> Unit
) {
    val isImporting by importService.isImporting.collectAsState()
    val importProgress by importService.importProgress.collectAsState()
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var importedBook by remember { mutableStateOf<BookEntity?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = !isImporting, onBack = onDismiss)

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val temp = withContext(Dispatchers.IO) { copyToCache(importService, uri) }
                val book = importService.importBook(temp)
                importedBook = book
                alertMessage = book.title
            } catch (e: ImportException) {
                alertMessage = resolveImportError(e)
            } catch (e: Exception) {
                alertMessage = e.message ?: "Import failed"
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        TextButton(
            onClick = { if (!isImporting) onDismiss() },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) {
            Text(stringResource(R.string.common_cancel))
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isImporting) {
                LinearProgressIndicator(
                    progress = { importProgress.toFloat() },
                    modifier = Modifier.width(200.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.library_import_in_progress), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${(importProgress * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
            } else {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.library_import_sheet_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.library_import_sheet_description),
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { filePicker.launch(arrayOf("application/epub+zip")) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.library_import_source_files_app))
                }
            }
        }
    }

    alertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text(stringResource(R.string.library_import_result_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    alertMessage = null
                    importedBook?.let { book ->
                        importedBook = null
                        onImported(book)
                    }
                }) { Text(stringResource(R.string.common_confirm)) }
            }
        )
    }
}

private fun copyToCache(importService: BookImportService, uri: Uri): File {
    val context = importService.appContext
    val temp = File(context.cacheDir, "import_${System.currentTimeMillis()}.epub")
    context.contentResolver.openInputStream(uri)?.use { input ->
        temp.outputStream().use { output -> input.copyTo(output) }
    } ?: throw ImportException("file_not_accessible")
    return temp
}

private fun resolveImportError(e: ImportException): String = when (e.messageKey) {
    "book_already_exists" -> "书籍《${e.arg}》已存在于书架中"
    "file_not_accessible" -> "无法访问选择的文件"
    else -> e.message ?: "导入失败"
}
