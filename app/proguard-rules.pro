# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Room database classes
-keep class com.apptimer.database.** { *; }
-keep class com.apptimer.database.entities.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep custom views
-keep public class com.apptimer.ui.CircularProgressView { *; }
