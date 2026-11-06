package com.lanread.app.ui.progress

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lanread.app.R
import com.lanread.app.di.progressSnapshot
import com.lanread.app.ui.AppSettingsViewModel
import com.lanread.app.ui.theme.AccentBlue
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

enum class TimeRange { WEEK, MONTH, YEAR }

data class RecentEntry(
    val bookId: String,
    val title: String,
    val author: String?,
    val cover: ByteArray?,
    val progress: Double,
    val lastReadAt: Long,
    val detailedSeconds: Long,
    val skimmingSeconds: Long,
    val statusFinished: Boolean
)

@Composable
fun ProgressScreen(
    appSettings: AppSettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.lanread.app.LanReadApp).container
    var selectedRange by remember { mutableStateOf(TimeRange.WEEK) }
    var entries by remember { mutableStateOf<List<RecentEntry>>(emptyList()) }

    androidx.compose.runtime.LaunchedEffect(selectedRange) {
        val data = container.progressSnapshot()
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val (start, end) = when (selectedRange) {
            TimeRange.WEEK -> {
                val today = LocalDate.now()
                val weekStart = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
                weekStart.atStartOfDay(zone).toInstant().toEpochMilli() to Long.MAX_VALUE
            }
            TimeRange.MONTH -> {
                val monthStart = LocalDate.now().withDayOfMonth(1)
                monthStart.atStartOfDay(zone).toInstant().toEpochMilli() to Long.MAX_VALUE
            }
            TimeRange.YEAR -> {
                val yearStart = LocalDate.now().withDayOfYear(1)
                yearStart.atStartOfDay(zone).toInstant().toEpochMilli() to Long.MAX_VALUE
            }
        }
        entries = data
            .filter { it.lastReadAt in start..end }
            .sortedByDescending { it.lastReadAt }
    }

    val totalDetailed = entries.sumOf { it.detailedSeconds }
    val totalSkimming = entries.sumOf { it.skimmingSeconds }
    val totalSeconds = totalDetailed + totalSkimming
    val averageProgress = if (entries.isEmpty()) 0.0 else entries.sumOf { it.progress } / entries.size
    val completed = entries.count { it.statusFinished || it.progress >= 0.99 }
    val daysInRange = when (selectedRange) {
        TimeRange.WEEK -> 7
        TimeRange.MONTH -> LocalDate.now().lengthOfMonth()
        TimeRange.YEAR -> LocalDate.now().lengthOfYear()
    }
    val goalMinutes = appSettings.state.dailyReadingGoal
    val targetMinutes = goalMinutes * daysInRange
    val goalPercent = if (targetMinutes > 0) {
        ((totalSeconds / 60) * 100 / targetMinutes).coerceAtMost(100)
    } else 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                stringResource(R.string.progress_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                TimeRange.entries.forEachIndexed { index, range ->
                    SegmentedButton(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeRange.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            inactiveContainerColor = Color.Transparent,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            stringResource(
                                when (range) {
                                    TimeRange.WEEK -> R.string.progress_time_range_week
                                    TimeRange.MONTH -> R.string.progress_time_range_month
                                    TimeRange.YEAR -> R.string.progress_time_range_year
                                }
                            )
                        )
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Schedule,
                        color = AccentBlue,
                        value = formatReadingTime(totalDetailed),
                        label = stringResource(R.string.progress_detailed_reading_time)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Bolt,
                        color = Color(0xFF30B0C7),
                        value = formatReadingTime(totalSkimming),
                        label = stringResource(R.string.progress_skimming_reading_time)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        color = Color(0xFF34C759),
                        value = String.format("%.1f%%", averageProgress * 100),
                        label = stringResource(R.string.progress_average_progress)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.MenuBook,
                        color = Color(0xFFFF9500),
                        value = "${entries.size}",
                        label = stringResource(R.string.progress_books_read)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.CheckCircle,
                        color = Color(0xFF5856D6),
                        value = "$completed",
                        label = stringResource(R.string.progress_completed_books)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.TrackChanges,
                        color = Color(0xFFAF52DE),
                        value = "$goalPercent%",
                        label = stringResource(R.string.progress_goal_achievement)
                    )
                }
            }
        }
        item {
            GoalCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                dailyGoal = goalMinutes,
                currentMinutes = (totalSeconds / 60).toInt(),
                totalMinutes = targetMinutes
            )
        }
        if (entries.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.progress_recent_reads),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            items(entries.take(5), key = { it.bookId }) { entry ->
                RecentReadingCard(entry, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    color: Color,
    value: String,
    label: String
) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalCard(
    modifier: Modifier = Modifier,
    dailyGoal: Int,
    currentMinutes: Int,
    totalMinutes: Int
) {
    val percentage = if (totalMinutes > 0) (currentMinutes.toFloat() / totalMinutes).coerceIn(0f, 1f) else 0f
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
        Row {
            Column {
                Text(stringResource(R.string.progress_daily_goal_title), fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.progress_daily_goal_minutes_format, dailyGoal),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("$currentMinutes/$totalMinutes", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(stringResource(R.string.common_minutes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 1.4f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .height(8.dp)
                    .background(
                        Brush.horizontalGradient(listOf(AccentBlue, Color(0xFFAF52DE))),
                        RoundedCornerShape(4.dp)
                    )
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                stringResource(R.string.progress_completed_format, percentage * 100),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (percentage >= 1f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.progress_goal_achieved), fontSize = 12.sp, color = Color(0xFF34C759), fontWeight = FontWeight.Medium)
                }
            }
        }
        }
    }
}

@Composable
private fun RecentReadingCard(entry: RecentEntry, modifier: Modifier = Modifier) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(70.dp)
                .background(
                    Brush.linearGradient(listOf(Color.Blue.copy(alpha = 0.6f), Color(0xFF8A2BE2).copy(alpha = 0.8f))),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (entry.cover != null) {
                AsyncImage(
                    model = entry.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(entry.author ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    String.format("%.1f%%", entry.progress * 100),
                    fontSize = 12.sp,
                    color = AccentBlue,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text(formatRelativeTime(entry.lastReadAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        }
    }
}

fun formatReadingTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1440 -> "${minutes / 60}h"
        minutes < 43200 -> "${minutes / 1440}d"
        else -> "${minutes / 43200}mo"
    }
}
