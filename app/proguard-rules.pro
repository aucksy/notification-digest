# Notification Digest — ProGuard / R8 rules.
# The app keeps all data on-device; rules below preserve framework-reflected classes.

# --- Kotlin metadata ---
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# --- Hilt / Dagger generated code is handled by the Hilt Gradle plugin ---

# --- Room ---
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- WorkManager workers are instantiated reflectively by name ---
-keep class * extends androidx.work.ListenableWorker { *; }

# --- NotificationListenerService is bound by the system via its class name ---
-keep class com.notdigest.app.service.** { *; }

# --- Kotlinx coroutines ---
-dontwarn kotlinx.coroutines.**

# --- Keep enum values used in (de)serialization ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
