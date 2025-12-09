package com.drumpractice.app.ui.screens.recording

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import com.drumpractice.app.data.model.Subdivision
import com.drumpractice.app.ui.theme.MetronomeAccent
import com.drumpractice.app.ui.theme.MetronomeBeat
import com.drumpractice.app.ui.theme.RecordingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecordingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onRecordingComplete: (Long) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val recordingState by viewModel.recordingState.collectAsState()
    val metronomeState by viewModel.metronomeState.collectAsState()
    val isVideoMode by viewModel.isVideoMode.collectAsState()
    val useMetronome by viewModel.useMetronome.collectAsState()
    val useBackingTrack by viewModel.useBackingTrack.collectAsState()
    val selectedSong by viewModel.selectedSong.collectAsState()
    val songs by viewModel.songs.collectAsState()

    var showSongPicker by remember { mutableStateOf(false) }
    var showSubdivisionSheet by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(false) }

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        val audioPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED
        
        val cameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PermissionChecker.PERMISSION_GRANTED

        hasPermissions = audioPermission && (!isVideoMode || cameraPermission)

        if (!hasPermissions) {
            val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (isVideoMode) {
                permissions.add(Manifest.permission.CAMERA)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isVideoMode) "Record Video" else "Record Audio") },
                navigationIcon = {
                    if (!recordingState.isRecording && !recordingState.isCountingDown) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!recordingState.isRecording && !recordingState.isCountingDown) {
                        // Toggle video/audio mode
                        IconButton(
                            onClick = { viewModel.setVideoMode(!isVideoMode) }
                        ) {
                            Icon(
                                imageVector = if (isVideoMode) Icons.Filled.Videocam else Icons.Filled.Mic,
                                contentDescription = "Toggle mode"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (recordingState.isCountingDown) {
                // Countdown overlay
                CountdownOverlay(count = recordingState.countdownValue)
            } else if (recordingState.isRecording) {
                // Recording in progress
                RecordingInProgressView(
                    recordingState = recordingState,
                    metronomeState = metronomeState,
                    isVideoMode = isVideoMode,
                    onPause = { viewModel.pauseRecording() },
                    onResume = { viewModel.resumeRecording() },
                    onStop = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.stopRecording(onRecordingComplete)
                    },
                    onCancel = {
                        viewModel.cancelRecording()
                    }
                )
            } else {
                // Setup view
                RecordingSetupView(
                    metronomeState = metronomeState,
                    useMetronome = useMetronome,
                    useBackingTrack = useBackingTrack,
                    selectedSong = selectedSong,
                    isVideoMode = isVideoMode,
                    hasPermissions = hasPermissions,
                    onUseMetronomeChange = { viewModel.setUseMetronome(it) },
                    onUseBackingTrackChange = { viewModel.setUseBackingTrack(it) },
                    onSelectSong = { showSongPicker = true },
                    onClearSong = { viewModel.selectSong(null) },
                    onBpmChange = { viewModel.setBpm(it) },
                    onSubdivisionClick = { showSubdivisionSheet = true },
                    onTapTempo = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.tapTempo()
                    },
                    onToggleMetronome = { viewModel.toggleMetronome() },
                    onStartRecording = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.startRecording()
                    },
                    onRequestPermissions = {
                        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (isVideoMode) {
                            permissions.add(Manifest.permission.CAMERA)
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                )
            }
        }
    }

    // Song picker sheet
    if (showSongPicker) {
        SongPickerSheet(
            songs = songs,
            selectedSong = selectedSong,
            onSongSelected = {
                viewModel.selectSong(it)
                showSongPicker = false
            },
            onDismiss = { showSongPicker = false }
        )
    }

    // Subdivision sheet
    if (showSubdivisionSheet) {
        SubdivisionPickerSheet(
            currentSubdivision = metronomeState.subdivision,
            onSubdivisionSelected = {
                viewModel.setSubdivision(it)
                showSubdivisionSheet = false
            },
            onDismiss = { showSubdivisionSheet = false }
        )
    }
}

@Composable
fun CountdownOverlay(count: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "countdown"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.scale(scale)
        )
    }
}

@Composable
fun RecordingInProgressView(
    recordingState: com.drumpractice.app.data.model.RecordingState,
    metronomeState: com.drumpractice.app.data.model.MetronomeState,
    isVideoMode: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val recordingIndicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "indicator"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Recording indicator
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(RecordingIndicator.copy(alpha = recordingIndicatorAlpha))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (recordingState.isPaused) "PAUSED" else "RECORDING",
                style = MaterialTheme.typography.titleMedium,
                color = RecordingIndicator
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Duration display
        Text(
            text = recordingState.formattedDuration,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )

        // Audio level meter (for audio recording)
        if (!isVideoMode) {
            Spacer(modifier = Modifier.height(24.dp))
            AudioLevelMeter(level = recordingState.audioLevel)
        }

        // Beat indicators
        if (metronomeState.isPlaying) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(metronomeState.beatsPerMeasure) { beat ->
                    Box(
                        modifier = Modifier
                            .size(if (beat == metronomeState.currentBeat) 20.dp else 14.dp)
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
        }

        Spacer(modifier = Modifier.weight(1f))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            OutlinedIconButton(
                onClick = onCancel,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel")
            }

            // Pause/Resume button
            FilledIconButton(
                onClick = { if (recordingState.isPaused) onResume() else onPause() },
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Icon(
                    imageVector = if (recordingState.isPaused) 
                        Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (recordingState.isPaused) "Resume" else "Pause",
                    modifier = Modifier.size(36.dp)
                )
            }

            // Stop button
            FilledIconButton(
                onClick = onStop,
                modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = RecordingIndicator
                )
            ) {
                Icon(
                    Icons.Filled.Stop,
                    contentDescription = "Stop",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AudioLevelMeter(level: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(level)
                .background(
                    when {
                        level > 0.8f -> RecordingIndicator
                        level > 0.5f -> MaterialTheme.colorScheme.tertiary
                        else -> MetronomeBeat
                    }
                )
        )
    }
}

@Composable
fun RecordingSetupView(
    metronomeState: com.drumpractice.app.data.model.MetronomeState,
    useMetronome: Boolean,
    useBackingTrack: Boolean,
    selectedSong: com.drumpractice.app.data.model.Song?,
    isVideoMode: Boolean,
    hasPermissions: Boolean,
    onUseMetronomeChange: (Boolean) -> Unit,
    onUseBackingTrackChange: (Boolean) -> Unit,
    onSelectSong: () -> Unit,
    onClearSong: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onSubdivisionClick: () -> Unit,
    onTapTempo: () -> Unit,
    onToggleMetronome: () -> Unit,
    onStartRecording: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Metronome toggle
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Timer, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Metronome", style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = useMetronome,
                        onCheckedChange = onUseMetronomeChange
                    )
                }

                if (useMetronome) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // BPM display
                    val pulseScale by animateFloatAsState(
                        targetValue = if (metronomeState.isPlaying && 
                            metronomeState.currentSubdivision == 0) 1.1f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        ),
                        label = "pulse"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onBpmChange(metronomeState.bpm - 1) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (metronomeState.isPlaying) 
                                MetronomeBeat.copy(alpha = 0.2f) 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .scale(pulseScale)
                                .clickable { onToggleMetronome() }
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${metronomeState.bpm}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "BPM",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        IconButton(onClick = { onBpmChange(metronomeState.bpm + 1) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase")
                        }
                    }

                    // Beat indicators
                    if (metronomeState.isPlaying) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(metronomeState.beatsPerMeasure) { beat ->
                                Box(
                                    modifier = Modifier
                                        .size(if (beat == metronomeState.currentBeat) 12.dp else 8.dp)
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metronome controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(onClick = onSubdivisionClick) {
                            Text(metronomeState.subdivision.displayName)
                        }
                        OutlinedButton(onClick = onTapTempo) {
                            Icon(Icons.Filled.TouchApp, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tap")
                        }
                        OutlinedButton(onClick = onToggleMetronome) {
                            Icon(
                                imageVector = if (metronomeState.isPlaying) 
                                    Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (metronomeState.isPlaying) "Stop" else "Test")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Backing track toggle
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.MusicNote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backing Track", style = MaterialTheme.typography.titleMedium)
                    }
                    Switch(
                        checked = useBackingTrack,
                        onCheckedChange = onUseBackingTrackChange
                    )
                }

                if (useBackingTrack) {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedSong != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedSong.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                selectedSong.artist?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = onClearSong) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSelectSong,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Song")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Record button
        if (hasPermissions) {
            FilledIconButton(
                onClick = onStartRecording,
                modifier = Modifier.size(96.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = RecordingIndicator
                )
            ) {
                Icon(
                    imageVector = if (isVideoMode) Icons.Filled.Videocam else Icons.Filled.Mic,
                    contentDescription = "Start Recording",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to start recording",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Button(onClick = onRequestPermissions) {
                Text("Grant Permissions")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPickerSheet(
    songs: List<com.drumpractice.app.data.model.Song>,
    selectedSong: com.drumpractice.app.data.model.Song?,
    onSongSelected: (com.drumpractice.app.data.model.Song) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Backing Track",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (songs.isEmpty()) {
                Text(
                    text = "No songs imported yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(songs) { song ->
                        ListItem(
                            headlineContent = { Text(song.title) },
                            supportingContent = { 
                                Text("${song.formattedDuration}${song.effectiveBpm?.let { " • $it BPM" } ?: ""}")
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = song.id == selectedSong?.id,
                                    onClick = { onSongSelected(song) }
                                )
                            },
                            modifier = Modifier.clickable { onSongSelected(song) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubdivisionPickerSheet(
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
