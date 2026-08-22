# Add project specific ProGuard rules here.

# ─── Annotations ──────────────────────────────────────────────────────
-keepattributes *Annotation*

# ─── Core Models ──────────────────────────────────────────────────────
-keep class com.twentyfoursolve.core.model.** { *; }

# ─── Room ─────────────────────────────────────────────────────────────
-keep class com.twentyfoursolve.data.local.entity.** { *; }
-keep class com.twentyfoursolve.data.local.dao.** { *; }
-keep class com.twentyfoursolve.data.local.AppDatabase { *; }

# ─── Hilt / Dagger ───────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ─── DataStore ────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ─── Kotlin Serialization (if used in future) ────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ─── Coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── Compose (generally not needed but safe to keep) ─────────────────
-keep class androidx.compose.** { *; }
