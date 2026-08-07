# Add project specific ProGuard rules here.

# Keep Room database entities and DAOs
-keep class com.example.data.** { *; }
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Retrofit models
-keep class com.example.data.WooOrder { *; }
-keep class com.example.data.WooProduct { *; }
-keep class com.example.data.WooCustomer { *; }
-keep class com.example.data.WooCoupon { *; }

# Keep Moshi generated adapters
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonQualifier @interface *
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    @com.squareup.moshi.* <fields>;
}

# Keep Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep security crypto
-keep class androidx.security.crypto.** { *; }

# Keep OkHttp logging
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**

# General Android
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
