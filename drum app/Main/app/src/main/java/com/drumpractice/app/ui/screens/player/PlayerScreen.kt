package com.drumpractice.app.ui.screens.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.drumpractice.app.data.model.Subdivision
import com.drumpractice.app.ui.theme.MetronomeAccent
import com.drumpractice.app.ui.theme.MetronomeBeat
import com.drumpractice.app.ui.theme.WaveformColor
import com.drumpractice.app.ui.theme.WaveformPlayedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    songId: Long,
    viewModel: PlayerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRecording: () -> Unit
) {
    val song by viewModel.song.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val metronomeState by viewModel.metronomeState.collectAsState()
    val isMetronomeEnabled by viewModel.isMetronomeEnabled.collectAsState()
    val bpmState by viewModel.bpmDetectionState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showBpmDialog by remember { mutableStateOf(false) }
    var showSubdivisionSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Sync metronome when playback state changes
    LaunchedEffect(playerState.isPlaying) {
        viewModel.syncMetronomeWithPlayback()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = song?.title ?: "Player",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToRecording) {
                        Icon(Icons.Filled.FiberManualRecord, contentDescription = "Record")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Song Info
            song?.let { currentSong ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Album art placeholder
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(120.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = currentSong.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        currentSong.artist?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Waveform/Progress
            WaveformProgress(
                progress = playerState.progress,
                onSeek = { progress ->
                    viewModel.seekTo((progress * playerState.duration).toLong())
                }
            )

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = playerState.formattedPosition,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = playerState.formattedRemaining,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed control
                IconButton(onClick = { showSpeedDialog = true }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Speed, contentDescription = "Speed")
                        Text(
                            text = "${playerState.playbackSpeed}x",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                IconButton(onClick = { viewModel.seekBackward() }) {
                    Icon(
                        Icons.Filled.Replay10, 
                        contentDescription = "Rewind 10s",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play/Pause button
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.togglePlayPause()
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) 
                            Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(onClick = { viewModel.seekForward() }) {
                    Icon(
                        Icons.Filled.Forward10, 
                        contentDescription = "Forward 10s",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Volume control
                var showVolumeSlider by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showVolumeSlider = !showVolumeSlider }) {
                        Icon(
                            imageVector = when {
                                playerState.volume == 0f -> Icons.Filled.VolumeOff
                                playerState.volume < 0.5f -> Icons.Filled.VolumeDown
                                else -> Icons.Filled.VolumeUp
                            },
                            contentDescription = "Volume"
                        )
                    }
                    DropdownMenu(
                        expanded = showVolumeSlider,
                        onDismissRequest = { showVolumeSlider = false }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Volume", style = MaterialTheme.typography.labelMedium)
                            Slider(
                                value = playerState.volume,
                                onValueChange = { viewModel.setVolume(it) },
                                modifier = Modifier.width(150.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metronome Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Metronome",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isMetronomeEnabled,
                            onCheckedChange = { viewModel.setMetronomeEnabled(it) }
                        )
                    }

                    if (isMetronomeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // BPM Display with beat indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pulseScale by animateFloatAsState(
                                targetValue = if (metronomeState.isPlaying && 
                                    metronomeState.currentSubdivision == 0) 1.15f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessHigh
                                ),
                                label = "pulse"
                            )

                            Surface(
                                shape = CircleShape,
                                color = when {
                                    metronomeState.isPlaying && metronomeState.currentBeat == 0 -> 
                                        MetronomeAccent.copy(alpha = 0.2f)
                                    metronomeState.isPlaying -> 
                                        MetronomeBeat.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .scale(pulseScale)
                                    .clickable { showBpmDialog = true }
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${metronomeState.bpm}",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "BPM",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Beat indicators
                        if (metronomeState.isPlaying) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(metronomeState.beatsPerMeasure) { beat ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(if (beat == metronomeState.currentBeat) 14.dp else 10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    beat == metronomeState.currentBeat && beat == 0 -> MetronomeAccent
                                                    beat == metronomeState.currentBeat -> MetronomeBeat
                                                    else -> MaterialTheme.colorScheme.outlineVariant
                                                }
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Metronome controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(onClick = { showBpmDialog = true }) {
                                Icon(Icons.Filled.Tune, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("BPM")
                            }

                            OutlinedButton(onClick = { showSubdivisionSheet = true }) {
                                Text(metronomeState.subdivision.displayName)
                            }

                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.tapTempo()
                                }
                            ) {
                                Icon(Icons.Filled.TouchApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tap")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // BPM Detection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (bpmState.isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Detecting BPM... ${(bpmState.progress * 100).toInt()}%")
                            } else {
                                TextButton(onClick = { viewModel.detectBpm() }) {
                                    Icon(Icons.Outlined.Analytics, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Detect BPM")
                                }

                                bpmState.result?.let { result ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    TextButton(onClick = { viewModel.applyDetectedBpm() }) {
                                        Text("Apply ${result.bpm} BPM")
                                    }
                                }
                            }
                        }

                        // Volume slider
                        Text(
                            text = "Metronome Volume: ${(metronomeState.volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Slider(
                            value = metronomeState.volume,
                            onValueChange = { viewModel.setMetronomeVolume(it) }
                        )
                    }
                }
            }
        }
    }

    // BPM Dialog
    if (showBpmDialog) {
        BpmEditDialog(
            currentBpm = metronomeState.bpm,
            onBpmChange = { viewModel.setBpm(it) },
            onDismiss = { showBpmDialog = false }
        )
    }

    // Subdivision Sheet
    if (showSubdivisionSheet) {
        SubdivisionSheet(
            currentSubdivision = metronomeState.subdivision,
            onSubdivisionSelected = { 
                viewModel.setSubdivision(it)
                showSubdivisionSheet = false
            },
            onDismiss = { showSubdivisionSheet = false }
        )
    }

    // Speed Dialog
    if (showSpeedDialog) {
        SpeedDialog(
            currentSpeed = playerState.playbackSpeed,
            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
            onDismiss = { showSpeedDialog = false }
        )
    }
}

@Composable
fun WaveformProgress(
    progress: Float,
    onSeek: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Played portion
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            WaveformPlayedColor.copy(alpha = 0.3f),
                            WaveformPlayedColor.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // Fake waveform visualization
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(50) { index ->
                val height = (20 + (Math.random() * 30)).dp
                val isPlayed = index.toFloat() / 50 <= progress
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(height)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isPlayed) WaveformPlayedColor
                            else WaveformColor.copy(alpha = 0.5f)
                        )
                )
            }
        }

        // Seekable slider overlay
        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
    }
}

@Composable
fun BpmEditDialog(
    currentBpm: Int,
    onBpmChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var bpmText by remember { mutableStateOf(currentBpm.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set BPM") },
        text = {
            Column {
                OutlinedTextField(
                    value = bpmText,
                    onValueChange = { bpmText = it.filter { char -> char.isDigit() } },
                    label = { Text("BPM") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = (bpmText.toIntOrNull() ?: currentBpm).toFloat(),
                    onValueChange = { bpmText = it.toInt().toString() },
                    valueRange = 20f..300f
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    bpmText.toIntOrNull()?.let { onBpmChange(it.coerceIn(20, 300)) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubdivisionSheet(
    currentSubdivision: Subdivision,
    onSubdivisionSelected: (Subdivision) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Subdivision",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Subdivision.getAll().forEach { subdivision ->
                ListItem(
                    headlineContent = { Text(subdivision.displayName) },
                    leadingContent = {
                        RadioButton(
                            selected = subdivision == currentSubdivision,
                            onClick = { onSubdivisionSelected(subdivision) }
                        )
                    },
                    modifier = Modifier.clickable { onSubdivisionSelected(subdivision) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SpeedDialog(
    currentSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback Speed") },
        text = {
            Column {
                speeds.forEach { speed ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSpeedChange(speed) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSpeed == speed,
                            onClick = { onSpeedChange(speed) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${speed}x")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
