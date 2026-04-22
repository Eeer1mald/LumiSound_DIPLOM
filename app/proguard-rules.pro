# ── Общие ──────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ── Kotlin ──────────────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ── kotlinx.serialization ───────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.lumisound.**$$serializer { *; }
-keepclassmembers class com.example.lumisound.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.lumisound.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep all @Serializable data classes
-keep @kotlinx.serialization.Serializable class * { *; }

# ── Ktor ────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keep class io.ktor.client.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**

# ── Hilt / Dagger ───────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityComponentManager { *; }
-keepclasseswithmembernames class * { @dagger.* <methods>; }
-keepclasseswithmembernames class * { @javax.inject.* <methods>; }
-dontwarn dagger.**

# ── ExoPlayer / Media3 ──────────────────────────────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class com.google.android.exoplayer2.** { *; }

# ── Coil ────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── uCrop ───────────────────────────────────────────────────────────────────
-keep class com.yalantis.ucrop.** { *; }
-dontwarn com.yalantis.ucrop.**

# ── Compose ─────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Navigation ──────────────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }

# ── Google Play Services ────────────────────────────────────────────────────
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── App models — не обфусцировать data классы ───────────────────────────────
-keep class com.example.lumisound.data.** { *; }
-keep class com.example.lumisound.feature.** { *; }

# ── Enum классы ─────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
