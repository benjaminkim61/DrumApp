# Drum Practice App

A comprehensive Android application for practicing drums with a metronome, backing tracks, and video recording capabilities.

## Features

### 🎵 Metronome
- Two click sound variations (configurable in Settings)
- Custom MP3 click sounds loaded from assets
- Multiple subdivisions: Quarter, Eighth, Triplet, Sixteenth, Thirty-Second
- Adjustable BPM (30-300)
- Accent first beat option
- Subdivision sounds (pitched-down versions of main click)

### 🎶 Music Library
- Import songs as backing tracks
- Automatic BPM detection using TarsosDSP
- Manual BPM override
- Search and organize your library

### 📹 Video Recording
- Record yourself practicing with front/back camera
- Play metronome and/or backing track while recording
- Countdown timer before recording starts
- Audio level visualization during recording

### 🎚️ Post-Recording Editing
- Adjust delay offset (-500ms to +500ms) to sync audio
- Independent volume control for:
  - Your recording
  - Backing track/song
  - Metronome click
- Save and export recordings

### ⚙️ Settings
- Theme: Light, Dark, or System default
- Click style selection (Style 1 / Style 2)
- Default BPM and subdivision
- Countdown duration
- Video quality (SD, HD 720p, Full HD 1080p)
- Latency calibration
- Haptic feedback toggle

## Project Structure

```
app/src/main/
├── assets/clicks/          # Place metronome MP3 files here
│   ├── click1.mp3         # Normal click - Style 1
│   ├── click_accent1.mp3  # Accent click - Style 1
│   ├── click2.mp3         # Normal click - Style 2
│   └── click_accent2.mp3  # Accent click - Style 2
├── java/com/drumpractice/app/
│   ├── audio/             # Audio engine components
│   ├── data/              # Data layer (Room, repositories)
│   ├── di/                # Hilt dependency injection
│   ├── service/           # Background services
│   └── ui/                # Compose UI
└── res/                   # Resources
```

## Setup Instructions

1. **Clone/Download** the project

2. **Add Metronome Sounds**
   - Place your MP3 click sounds in `app/src/main/assets/clicks/`
   - Required files:
     - `click1.mp3` - Normal click for Style 1
     - `click_accent1.mp3` - Accented click for Style 1
     - `click2.mp3` - Normal click for Style 2
     - `click_accent2.mp3` - Accented click for Style 2

3. **Open in Android Studio**
   - Open the `Main` folder as an Android Studio project
   - Wait for Gradle sync to complete

4. **Build and Run**
   - Connect an Android device or start an emulator
   - Click Run or press Shift+F10

## Tech Stack

- **UI**: Jetpack Compose with Material 3
- **Audio Playback**: Media3/ExoPlayer
- **BPM Detection**: TarsosDSP
- **Metronome Engine**: AudioTrack with MP3 decoding
- **Video Recording**: CameraX
- **Database**: Room
- **Dependency Injection**: Hilt
- **Settings Storage**: DataStore Preferences
- **Background Processing**: Foreground Services

## Permissions Required

- `RECORD_AUDIO` - Audio recording
- `CAMERA` - Video recording
- `FOREGROUND_SERVICE` - Background playback/recording
- `VIBRATE` - Haptic feedback
- Storage permissions for saving/loading media

## Minimum Requirements

- Android API 24 (Android 7.0) or higher
- Camera (front and/or back)
- Microphone
- Storage space for recordings

## Building for Release

```bash
./gradlew assembleRelease
```

The signed APK will be in `app/build/outputs/apk/release/`

## License

This project is for personal use.
