# StingrayShield ProGuard Rules
# Comprehensive obfuscation for privacy and security

#---------------------------------
# General Android Rules
#---------------------------------
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep application class
-keep class com.stingrayshield.StingrayShieldApp { *; }

#---------------------------------
# Firebase Obfuscation Rules
#---------------------------------
# Keep Firebase classes needed for functionality but obfuscate internals
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Analytics - minimize data collection
-keep class com.google.firebase.analytics.** { *; }
-keep class com.google.android.gms.measurement.** { *; }

# Firebase Cloud Messaging
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# Firebase Remote Config
-keep class com.google.firebase.remoteconfig.** { *; }

#---------------------------------
# Privacy-Focused Obfuscation
#---------------------------------
# Obfuscate all internal classes aggressively
-repackageclasses 'x'
-allowaccessmodification
-overloadaggressively

# Obfuscate detection algorithms to prevent reverse engineering
-keep,allowobfuscation class com.stingrayshield.detection.** { *; }

# Obfuscate device identification logic
-keep,allowobfuscation class com.stingrayshield.detection.DeviceIdentifier { *; }

# Keep but obfuscate stingray device data model
-keep,allowobfuscation class com.stingrayshield.domain.model.StingrayDevice { *; }

#---------------------------------
# Room Database
#---------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao class * { *; }

#---------------------------------
# Hilt/Dagger
#---------------------------------
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
}

#---------------------------------
# Kotlin/Coroutines
#---------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

#---------------------------------
# Retrofit/OkHttp (if used for future API calls)
#---------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

#---------------------------------
# Compose
#---------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

#---------------------------------
# Parcelize
#---------------------------------
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

#---------------------------------
# OSMDroid Map
#---------------------------------
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

#---------------------------------
# MPAndroidChart
#---------------------------------
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

#---------------------------------
# Remove Logging in Release
#---------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

#---------------------------------
# Telephony Classes (Keep for detection)
#---------------------------------
-keep class android.telephony.** { *; }
-dontwarn android.telephony.**

#---------------------------------
# Security: Prevent String Constant Extraction
#---------------------------------
# This helps prevent extraction of hardcoded strings that might reveal app functionality
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

#---------------------------------
# Remove Debug Information
#---------------------------------
-renamesourcefileattribute SourceFile













