# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep TarsosDSP classes
-keep class be.tarsos.dsp.** { *; }

# Keep Room entities
-keep class com.drumpractice.app.data.local.entity.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Compose classes
-keep class androidx.compose.** { *; }

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# CameraX
-keep class androidx.camera.** { *; }
