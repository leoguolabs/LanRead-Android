package com.lanread.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.lanread.app.R
import com.lanread.app.ui.library.LibraryScreen
import com.lanread.app.ui.progress.ProgressScreen
import com.lanread.app.ui.settings.SettingsScreen

private enum class PhoneTab(val titleRes: Int) {
    LIBRARY(R.string.tab_library),
    PROGRESS(R.string.tab_progress),
    SETTINGS(R.string.tab_settings)
}

@Composable
fun LanReadRoot() {
    val settingsViewModel = rememberAppSettingsViewModel()
    var selectedTab by rememberSaveable { mutableStateOf(PhoneTab.LIBRARY) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                PhoneTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon(selected),
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = { Text(stringResource(tab.titleRes)) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (selectedTab) {
            PhoneTab.LIBRARY -> LibraryScreen(appSettings = settingsViewModel, modifier = modifier)
            PhoneTab.PROGRESS -> ProgressScreen(appSettings = settingsViewModel, modifier = modifier)
            PhoneTab.SETTINGS -> SettingsScreen(appSettings = settingsViewModel, modifier = modifier)
        }
    }
}

private fun PhoneTab.icon(selected: Boolean): ImageVector = when (this) {
    PhoneTab.LIBRARY ->
        if (selected) Icons.Filled.CollectionsBookmark else Icons.Outlined.CollectionsBookmark
    PhoneTab.PROGRESS ->
        if (selected) Icons.Filled.AutoGraph else Icons.Outlined.AutoGraph
    PhoneTab.SETTINGS ->
        if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
