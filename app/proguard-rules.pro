
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,Exceptions,InnerClasses
-renamesourcefileattribute SourceFile

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    static <1>$Companion Companion;
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.grace.app.**$$serializer { *; }
-keepclasseswithmembers class com.grace.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.grace.app.data.remote.bible.dto.** { *; }

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.grace.app.data.local.entity.** { *; }

-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
