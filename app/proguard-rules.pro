# ============================================================
# StreamingHubTv - ProGuard / R8 Rules
# ============================================================

# -------------------- General Kotlin --------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# -------------------- Kotlinx Serialization --------------------
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class it.streaminghub.tv.data.model.**$$serializer { *; }
-keepclassmembers class it.streaminghub.tv.**  {
    *** Companion;
}
-keepclasseswithmembers class it.streaminghub.tv.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class it.streaminghub.tv.data.model.** { *; }

# -------------------- Kotlinx Coroutines --------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# -------------------- Ktor Client --------------------
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keep class io.ktor.client.engine.okhttp.** { *; }
-dontwarn org.slf4j.**

# -------------------- OkHttp --------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# -------------------- Media3 / ExoPlayer --------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# -------------------- Coil --------------------
-keep class coil.** { *; }
-dontwarn coil.**

# -------------------- Jetpack Compose --------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# -------------------- AndroidX TV Material --------------------
-keep class androidx.tv.** { *; }
-dontwarn androidx.tv.**

# -------------------- AndroidX Navigation --------------------
-keepnames class androidx.navigation.** { *; }

# -------------------- AndroidX DataStore --------------------
-keep class androidx.datastore.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# -------------------- AndroidX Lifecycle --------------------
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <methods>;
}

# -------------------- App specific: keep sealed classes --------------------
-keep class it.streaminghub.tv.data.api.DiscoveryState { *; }
-keep class it.streaminghub.tv.data.api.DiscoveryState$* { *; }

# -------------------- General: keep enums --------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# -------------------- Remove debug logs in release --------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
