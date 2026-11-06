package com.lanread.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.lanread.app.ui.LanReadRoot
import com.lanread.app.ui.effectiveDarkMode
import com.lanread.app.ui.rememberAppSettingsViewModel
import com.lanread.app.ui.theme.LanReadTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val settingsViewModel = rememberAppSettingsViewModel()
            val darkMode = settingsViewModel.effectiveDarkMode()
            LanReadTheme(darkTheme = darkMode) {
                LanReadRoot(settingsViewModel = settingsViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val importPath = intent.getStringExtra("import_file") ?: intent.getStringExtra("import_path")
        android.util.Log.i("MainActivity", "handleIntent: importPath=$importPath")
        if (!importPath.isNullOrBlank()) {
            val file = File(importPath)
            android.util.Log.i("MainActivity", "handleIntent: file=$file, exists=${file.exists()}")
            if (file.exists()) {
                importEpubFile(file)
            } else {
                android.util.Log.e("MainActivity", "handleIntent: file does not exist: $file")
            }
            return
        }

        val uri = intent.data ?: return
        if (intent.action == Intent.ACTION_VIEW) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val app = application as LanReadApp
                    val temp = File(cacheDir, "intent_import_${System.currentTimeMillis()}.epub")
                    contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (temp.exists() && temp.length() > 0) {
                        app.container.bookImportService.importBook(temp)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Book imported successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun importEpubFile(file: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val app = application as LanReadApp
                app.container.bookImportService.importBook(file)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Imported: ${file.name}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
