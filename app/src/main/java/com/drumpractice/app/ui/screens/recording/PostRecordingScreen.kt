package com.drumpractice.app.ui.screens.recording

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRecordingScreen(
    recordingId: Long,
    viewModel: PostRecordingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val recording by viewModel.recording.collectAsState()
    val backingSong by viewModel.backingSong.collectAsState()
    val state by viewModel.state.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var editedName by remember(recording) { mutableStateOf(recording?.name ?: "") }

    // Handle back press with unsaved changes
    fun handleBack() {
        if (state.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Recording") },
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                    TextButton(
                        onClick = { 
                            viewModel.setName(editedName)
                            viewModel.saveChanges(onSaveComplete) 
                        },
                        enabled = state.hasUnsavedChanges || editedName != recording?.name
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            recording?.let { rec ->
                // Name input
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Recording Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Recording info card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Recording Info",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        InfoRow("Duration", rec.formattedDuration)
                        InfoRow("Type", if (rec.isVideo) "Video" else "Audio")
                        InfoRow("Date", rec.formattedDate)
                        InfoRow("Size", rec.formattedFileSize)
                        rec.bpmUsed?.let { InfoRow("BPM Used", "$it") }
                        rec.subdivisionUsed?.let { InfoRow("Subdivision", it.displayName) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Playback controls
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress
                        LinearProgressIndicator(
                            progress = { playerState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = playerState.formattedPosition,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = playerState.formattedDuration,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.seekTo(0) }) {
                                Icon(Icons.Filled.SkipPrevious, contentDescription = "Restart")
                            }
                            FilledIconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) 
                                        Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Volume & Delay adjustments
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Adjustments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Audio Delay
                        Text(
                            text = "Audio Delay: ${state.audioDelayMs}ms",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("-500ms", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = state.audioDelayMs.toFloat(),
                                onValueChange = { viewModel.setAudioDelay(it.toLong()) },
                                valueRange = -500f..500f,
                                modifier = Modifier.weight(1f)
                            )
                            Text("+500ms", style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Recording Volume
                        Text(
                            text = "Recording Volume: ${(state.recordingVolume * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = state.recordingVolume,
                            onValueChange = { viewModel.setRecordingVolume(it) }
                        )

                        // Backtrack volume (if applicable)
                        if (backingSong != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Backtrack Volume: ${(state.backtrackVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = state.backtrackVolume,
                                onValueChange = { viewModel.setBacktrackVolume(it) }
                            )
                        }

                        // Metronome volume (if applicable)
                        if (rec.bpmUsed != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Metronome Volume: ${(state.metronomeVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Slider(
                                value = state.metronomeVolume,
                                onValueChange = { viewModel.setMetronomeVolume(it) }
                            )
                        }
                    }
                }

                // Backing track info
                backingSong?.let { song ->
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Backing Track",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(song.title, fontWeight = FontWeight.SemiBold)
                                    song.artist?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete this recording? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecording(onNavigateBack)
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Discard changes dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
