# ---------------------------------------------------------------------------
# GRACE ProGuard / R8 rules
# ---------------------------------------------------------------------------

-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,Exceptions,InnerClasses
-renamesourcefileattribute SourceFile

# --- Kotlinx Serialization (Supabase DTOs) ---
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static <1>$Companion Companion;
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.grace.app.**$$serializer { *; }
-keepclasseswithmembers class com.grace.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Supabase / Ktor ---
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --- Retrofit / OkHttp / Gson ---
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.grace.app.data.remote.bible.dto.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- Room entities ---
-keep class com.grace.app.data.local.entity.** { *; }

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# --- Strip verbose logging in release ---
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
