package com.drumpractice.app.data.mapper

import com.drumpractice.app.data.local.entity.RecordingEntity
import com.drumpractice.app.data.local.entity.SongEntity
import com.drumpractice.app.data.model.Recording
import com.drumpractice.app.data.model.Song
import com.drumpractice.app.data.model.Subdivision

fun SongEntity.toModel(): Song = Song(
    id = id,
    title = title,
    artist = artist,
    filePath = filePath,
    duration = duration,
    detectedBpm = detectedBpm,
    manualBpm = manualBpm,
    dateAdded = dateAdded,
    lastPlayed = lastPlayed,
    artworkPath = artworkPath,
    fileSize = fileSize
)

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    title = title,
    artist = artist,
    filePath = filePath,
    duration = duration,
    detectedBpm = detectedBpm,
    manualBpm = manualBpm,
    dateAdded = dateAdded,
    lastPlayed = lastPlayed,
    artworkPath = artworkPath,
    fileSize = fileSize
)

fun RecordingEntity.toModel(): Recording = Recording(
    id = id,
    name = name,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    duration = duration,
    isVideo = isVideo,
    songId = songId,
    bpmUsed = bpmUsed,
    subdivisionUsed = subdivisionUsed?.let { Subdivision.fromString(it) },
    audioDelay = audioDelay,
    recordingVolume = recordingVolume,
    backtrackVolume = backtrackVolume,
    metronomeVolume = metronomeVolume,
    dateCreated = dateCreated,
    fileSize = fileSize
)

fun Recording.toEntity(): RecordingEntity = RecordingEntity(
    id = id,
    name = name,
    filePath = filePath,
    thumbnailPath = thumbnailPath,
    duration = duration,
    isVideo = isVideo,
    songId = songId,
    bpmUsed = bpmUsed,
    subdivisionUsed = subdivisionUsed?.name,
    audioDelay = audioDelay,
    recordingVolume = recordingVolume,
    backtrackVolume = backtrackVolume,
    metronomeVolume = metronomeVolume,
    dateCreated = dateCreated,
    fileSize = fileSize
)

fun List<SongEntity>.toSongModels(): List<Song> = map { it.toModel() }
fun List<RecordingEntity>.toRecordingModels(): List<Recording> = map { it.toModel() }
