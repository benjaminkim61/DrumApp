package com.drumpractice.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.drumpractice.app.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val storageInfo by viewModel.storageInfo.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showClickStyleDialog by remember { mutableStateOf(false) }
    var showSubdivisionDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }
    var showCountdownDialog by remember { mutableStateOf(false) }
    var showLatencyDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "Theme",
                    subtitle = settings.theme.displayName,
                    onClick = { showThemeDialog = true }
                )
                SettingsToggle(
                    icon = Icons.Outlined.Vibration,
                    title = "Haptic Feedback",
                    subtitle = "Vibration on button presses",
                    checked = settings.hapticFeedback,
                    onCheckedChange = { viewModel.updateHapticFeedback(it) }
                )
            }

            // Metronome Section
            SettingsSection(title = "Metronome") {
                SettingsItem(
                    icon = Icons.Outlined.MusicNote,
                    title = "Click Style",
                    subtitle = settings.clickStyle.displayName,
                    onClick = { showClickStyleDialog = true }
                )
                SettingsItem(
                    icon = Icons.Outlined.Speed,
                    title = "Default BPM",
                    subtitle = "${settings.defaultBpm}",
                    onClick = { /* Could add a dialog */ }
                )
                SettingsItem(
                    icon = Icons.Outlined.GridView,
                    title = "Default Subdivision",
                    subtitle = settings.defaultSubdivision.displayName,
                    onClick = { showSubdivisionDialog = true }
                )
                SettingsToggle(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Accent First Beat",
                    subtitle = "Emphasize downbeat",
                    checked = settings.accentFirstBeat,
                    onCheckedChange = { viewModel.updateAccentFirstBeat(it) }
                )
                SettingsSlider(
                    icon = Icons.Outlined.VolumeUp,
                    title = "Default Metronome Volume",
                    value = settings.defaultMetronomeVolume,
                    onValueChange = { viewModel.updateDefaultMetronomeVolume(it) }
                )
            }

            // Recording Section
            SettingsSection(title = "Recording") {
                SettingsItem(
                    icon = Icons.Outlined.Timer,
                    title = "Countdown Duration",
                    subtitle = "${settings.countdownSeconds} seconds",
                    onClick = { showCountdownDialog = true }
                )
                SettingsItem(
                    icon = Icons.Outlined.HighQuality,
                    title = "Video Quality",
                    subtitle = settings.videoQuality.displayName,
                    onClick = { showVideoQualityDialog = true }
                )
                SettingsToggle(
                    icon = Icons.Outlined.Mic,
                    title = "Record Metronome Audio",
                    subtitle = "Include click in recording",
                    checked = settings.recordMetronomeAudio,
                    onCheckedChange = { viewModel.updateRecordMetronomeAudio(it) }
                )
            }

            // Audio Section
            SettingsSection(title = "Audio") {
                SettingsItem(
                    icon = Icons.Outlined.SettingsInputComponent,
                    title = "Latency Offset",
                    subtitle = "${settings.latencyOffsetMs}ms",
                    onClick = { showLatencyDialog = true }
                )
            }

            // Storage Section
            SettingsSection(title = "Storage") {
                storageInfo?.let { info ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${info.songCount}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${info.recordingCount}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Recordings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Storage Used: ${info.formattedStorage}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "Theme",
            options = AppTheme.entries.map { it.displayName },
            selectedIndex = AppTheme.entries.indexOf(settings.theme),
            onSelect = { index ->
                viewModel.updateTheme(AppTheme.entries[index])
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Click Style Dialog
    if (showClickStyleDialog) {
        SingleChoiceDialog(
            title = "Click Style",
            options = ClickStyle.entries.map { it.displayName },
            selectedIndex = ClickStyle.entries.indexOf(settings.clickStyle),
            onSelect = { index ->
                viewModel.updateClickStyle(ClickStyle.entries[index])
                showClickStyleDialog = false
            },
            onDismiss = { showClickStyleDialog = false }
        )
    }

    // Subdivision Dialog
    if (showSubdivisionDialog) {
        SingleChoiceDialog(
            title = "Default Subdivision",
            options = Subdivision.entries.map { it.displayName },
            selectedIndex = Subdivision.entries.indexOf(settings.defaultSubdivision),
            onSelect = { index ->
                viewModel.updateDefaultSubdivision(Subdivision.entries[index])
                showSubdivisionDialog = false
            },
            onDismiss = { showSubdivisionDialog = false }
        )
    }

    // Video Quality Dialog
    if (showVideoQualityDialog) {
        SingleChoiceDialog(
            title = "Video Quality",
            options = VideoQuality.entries.map { it.displayName },
            selectedIndex = VideoQuality.entries.indexOf(settings.videoQuality),
            onSelect = { index ->
                viewModel.updateVideoQuality(VideoQuality.entries[index])
                showVideoQualityDialog = false
            },
            onDismiss = { showVideoQualityDialog = false }
        )
    }

    // Countdown Dialog
    if (showCountdownDialog) {
        SingleChoiceDialog(
            title = "Countdown Duration",
            options = listOf("2 seconds", "3 seconds", "4 seconds", "5 seconds", "8 seconds"),
            selectedIndex = listOf(2, 3, 4, 5, 8).indexOf(settings.countdownSeconds),
            onSelect = { index ->
                val seconds = listOf(2, 3, 4, 5, 8)[index]
                viewModel.updateCountdownSeconds(seconds)
                showCountdownDialog = false
            },
            onDismiss = { showCountdownDialog = false }
        )
    }

    // Latency Dialog
    if (showLatencyDialog) {
        LatencyDialog(
            currentValue = settings.latencyOffsetMs,
            onValueChange = { viewModel.updateLatencyOffset(it) },
            onDismiss = { showLatencyDialog = false }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
fun SettingsSlider(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title)
            Spacer(modifier = Modifier.weight(1f))
            Text("${(value * 100).toInt()}%")
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(start = 40.dp)
        )
    }
}

@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LatencyDialog(
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentValue.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Latency Offset") },
        text = {
            Column {
                Text(
                    text = "Adjust if your recording sounds out of sync. Positive values delay the backing track/metronome.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${sliderValue.toInt()}ms",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = -200f..200f,
                    steps = 39
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("-200ms", style = MaterialTheme.typography.labelSmall)
                    Text("+200ms", style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValueChange(sliderValue.toInt())
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
