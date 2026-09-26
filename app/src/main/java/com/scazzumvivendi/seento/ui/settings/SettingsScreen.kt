package com.scazzumvivendi.seento.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.scazzumvivendi.seento.R
import com.scazzumvivendi.seento.ui.components.SeentoBottomBar
import com.scazzumvivendi.seento.ui.components.SeentoHeader

@Composable
fun SettingsScreen(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onHomeClick: () -> Unit,
    onDeviceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    isDeviceConnected: Boolean
) {
    Scaffold(
        topBar = { SeentoHeader() },
        bottomBar = {
            SeentoBottomBar(
                selected = 2,
                onPlaylistClick = onHomeClick,
                onDeviceClick = onDeviceClick,
                isDeviceConnected = isDeviceConnected,
                onSettingsClick = onSettingsClick,
                settingsSelected = true
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.language_settings_description))
            LanguageOption(
                title = stringResource(R.string.italian),
                selected = selectedLanguage == "it",
                onClick = { onLanguageSelected("it") }
            )
            LanguageOption(
                title = stringResource(R.string.english),
                selected = selectedLanguage != "it",
                onClick = { onLanguageSelected("en") }
            )
        }
    }
}

@Composable
private fun LanguageOption(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, modifier = Modifier.padding(start = 8.dp))
    }
}
