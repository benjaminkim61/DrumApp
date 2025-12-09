package com.drumpractice.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.drumpractice.app.data.model.MetronomeState
import com.drumpractice.app.data.model.Song
import com.drumpractice.app.data.model.Subdivision
import com.drumpractice.app.ui.theme.MetronomeAccent
import com.drumpractice.app.ui.theme.MetronomeBeat
import com.drumpractice.app.ui.theme.MetronomeSubdivision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToPlayer: (Long) -> Unit,
    onNavigateToRecording: () -> Unit
) {
    val metronomeState by viewModel.metronomeState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val recentSongs by viewModel.recentSongs.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showSubdivisionSelector by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importSong(it) { song ->
                song?.let { onNavigateToPlayer(it.id) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "Drum Practice",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Metronome Section
        MetronomeSection(
            state = metronomeState,
            onToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleMetronome()
            },
            onBpmChange = { viewModel.setBpm(it) },
            onTapTempo = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.tapTempo()
            },
            onSubdivisionClick = { showSubdivisionSelector = true },
            onVolumeChange = { viewModel.setMetronomeVolume(it) },
            onSubdivisionVolumeChange = { viewModel.setSubdivisionVolume(it) },
            onBeatsPerMeasureChange = { viewModel.setBeatsPerMeasure(it) },
            onAccentChange = { viewModel.setAccentFirstBeat(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickActionButton(
                icon = Icons.Filled.FiberManualRecord,
                label = "Record",
                color = MaterialTheme.colorScheme.error,
                onClick = onNavigateToRecording
            )
            QuickActionButton(
                icon = Icons.Filled.Add,
                label = "Import Song",
                color = MaterialTheme.colorScheme.primary,
                onClick = { filePickerLauncher.launch("audio/*") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Recent Songs
        if (recentSongs.isNotEmpty()) {
            Text(
                text = "Recent Songs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentSongs) { song ->
                    SongCard(
                        song = song,
                        onClick = { onNavigateToPlayer(song.id) }
                    )
                }
            }
        }
    }

    // Subdivision Selector Bottom Sheet
    if (showSubdivisionSelector) {
        SubdivisionSelectorSheet(
            currentSubdivision = metronomeState.subdivision,
            onSubdivisionSelected = {
                viewModel.setSubdivision(it)
                showSubdivisionSelector = false
            },
            onDismiss = { showSubdivisionSelector = false }
        )
    }
}

@Composable
fun MetronomeSection(
    state: MetronomeState,
    onToggle: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onTapTempo: () -> Unit,
    onSubdivisionClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSubdivisionVolumeChange: (Float) -> Unit,
    onBeatsPerMeasureChange: (Int) -> Unit,
    onAccentChange: (Boolean) -> Unit
) {
    val pulseScale by animateFloatAsState(
        targetValue = if (state.isPlaying && state.currentSubdivision == 0) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pulse"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "metronome")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (60000 / state.bpm),
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Metronome Visual
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .scale(if (state.isPlaying) ringScale else 1f)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 4.dp,
                        color = when {
                            state.isPlaying && state.currentBeat == 0 && state.accentFirstBeat -> MetronomeAccent
                            state.isPlaying -> MetronomeBeat
                            else -> MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
                    .clickable { onToggle() }
                    .scale(pulseScale)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.bpm}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Stop" else "Start",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Beat indicators
            if (state.isPlaying) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(state.beatsPerMeasure) { beat ->
                        Box(
                            modifier = Modifier
                                .size(if (beat == state.currentBeat) 16.dp else 12.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        beat == state.currentBeat && beat == 0 && state.accentFirstBeat -> MetronomeAccent
                                        beat == state.currentBeat -> MetronomeBeat
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BPM Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBpmChange(state.bpm - 1) }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease BPM")
                }
                Slider(
                    value = state.bpm.toFloat(),
                    onValueChange = { onBpmChange(it.toInt()) },
                    valueRange = MetronomeState.MIN_BPM.toFloat()..MetronomeState.MAX_BPM.toFloat(),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onBpmChange(state.bpm + 1) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase BPM")
                }
            }

            // Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Tap Tempo
                OutlinedButton(onClick = onTapTempo) {
                    Icon(Icons.Filled.TouchApp, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tap")
                }

                // Subdivision
                OutlinedButton(onClick = onSubdivisionClick) {
                    Icon(Icons.Filled.GridView, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(state.subdivision.displayName)
                }

                // Time Signature
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("${state.beatsPerMeasure}/4")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf(2, 3, 4, 5, 6, 7, 8, 9, 12).forEach { beats ->
                            DropdownMenuItem(
                                text = { Text("$beats/4") },
                                onClick = {
                                    onBeatsPerMeasureChange(beats)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume Controls
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Click Volume: ${(state.volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = state.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.subdivision != Subdivision.QUARTER) {
                    Text(
                        text = "Subdivision Volume: ${(state.subdivisionVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = state.subdivisionVolume,
                        onValueChange = onSubdivisionVolumeChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Accent toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accent first beat",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.accentFirstBeat,
                    onCheckedChange = onAccentChange
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            song.artist?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            song.effectiveBpm?.let { bpm ->
                Text(
                    text = "$bpm BPM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubdivisionSelectorSheet(
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
                text = "Select Subdivision",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Subdivision.getAll().forEach { subdivision ->
                ListItem(
                    headlineContent = { Text(subdivision.displayName) },
                    supportingContent = { 
                        Text("${subdivision.clicksPerBeat} clicks per beat") 
                    },
                    leadingContent = {
                        RadioButton(
                            selected = subdivision == currentSubdivision,
                            onClick = { onSubdivisionSelected(subdivision) }
                        )
                    },
                    trailingContent = {
                        // Visual representation of subdivision
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(subdivision.clicksPerBeat.coerceAtMost(8)) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == 0) 10.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == 0) MetronomeBeat 
                                            else MetronomeSubdivision
                                        )
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onSubdivisionSelected(subdivision) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
