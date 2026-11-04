package com.lanread.app.ui.reader

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lanread.app.R
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.data.local.entity.HighlightEntity
import com.lanread.app.di.AppContainer
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.theme.AccentBlue
import com.lanread.app.util.EPubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ReaderChapter(
    val title: String,
    val htmlContent: String,
    val order: Int,
    val sourceHref: String?
)

data class SelectionPayload(
    val text: String,
    val start: Int,
    val end: Int,
    val pageIndex: Int,
    val rectX: Float,
    val rectY: Float
)

/** Bridges WebView JS messages to Compose via a mutable state holder. */
class ReaderBridge(
    val onToolbarToggle: () -> Unit,
    val onSelection: (SelectionPayload?) -> Unit,
    val onHighlightTap: (String, String) -> Unit,
    val onLinkTap: (String, String, Boolean) -> Unit,
    val onPageMetrics: (String, Int) -> Unit,
    val onPageTap: (String) -> Unit = {}
) {
    @JavascriptInterface
    fun postMessage(json: String) {
        try {
            val obj = JSONObject(json)
            val name = obj.optString("name")
            val payload = obj.optJSONObject("payload") ?: JSONObject()
            Handler(Looper.getMainLooper()).post {
                when (name) {
                    "toggleToolbar" -> onToolbarToggle()
                    "textSelection" -> {
                        val text = payload.optString("text", "")
                        if (text.isEmpty() || payload.optBoolean("cleared", false)) {
                            onSelection(null)
                        } else {
                            val rect = payload.optJSONObject("rect")
                            onSelection(
                                SelectionPayload(
                                    text = text,
                                    start = payload.optInt("start", 0),
                                    end = payload.optInt("end", 0),
                                    pageIndex = payload.optInt("pageIndex", 0),
                                    rectX = rect?.optDouble("x")?.toFloat() ?: 0f,
                                    rectY = rect?.optDouble("y")?.toFloat() ?: 0f
                                )
                            )
                        }
                    }
                    "highlightTap" -> onHighlightTap(payload.optString("id"), payload.optString("text"))
                    "contentLinkTap" -> onLinkTap(
                        payload.optString("href"),
                        payload.optString("text"),
                        payload.optBoolean("isExternal")
                    )
                    "pageMetrics" -> onPageMetrics(
                        payload.optString("type"),
                        payload.optInt("value", 0)
                    )
                    "pageTap" -> onPageTap(payload.optString("zone"))
                }
            }
        } catch (_: Exception) {
        }
    }
}
