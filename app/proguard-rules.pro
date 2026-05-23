# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt
-dontwarn com.google.dagger.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.lendlog.app.**$$serializer { *; }
-keepclassmembers class com.lendlog.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.lendlog.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Billing
-keep class com.android.billingclient.** { *; }

# WorkManager / HiltWorker — keep AssistedInject generated classes
-keep class * extends androidx.work.ListenableWorker { *; }

# Konfetti — keep models used via reflection
-keep class nl.dionsegijn.konfetti.** { *; }
