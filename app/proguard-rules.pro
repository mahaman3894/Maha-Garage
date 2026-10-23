# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}

# Keep database entities and models for Room and Serialization
-keep class maha.garage.data.local.entity.** { *; }
-keep class maha.garage.data.model.** { *; }
-keep class maha.garage.data.catalog.** { *; }
-keep class maha.garage.domain.model.** { *; }
-keep class maha.garage.util.** { *; }

# Keep Kotlinx Serialization
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit2 & Moshi & OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class maha.garage.data.remote.** { *; }
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}

# ML Kit & CameraX
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**
