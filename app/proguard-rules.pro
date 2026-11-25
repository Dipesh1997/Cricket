# Proguard & R8 optimization rules for Cricket Auction

# Preserve reflection attributes and annotations for serialization
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# Preserve Firebase annotations and POJO reflection
-dontwarn com.google.firebase.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.PropertyName <methods>;
    @com.google.firebase.database.IgnoreExtraProperties <fields>;
    @com.google.firebase.database.Exclude <fields>;
    @com.google.firebase.database.Exclude <methods>;
}

-keep @interface com.google.firebase.database.PropertyName
-keep @interface com.google.firebase.database.IgnoreExtraProperties
-keep @interface com.google.firebase.database.Exclude

# Keep all Cricket data models, local schemas, and remote data classes intact
-keep class cricket.player.auction.model.** { *; }
-keepclassmembers class cricket.player.auction.model.** { *; }
-keep class cricket.player.auction.data.local.** { *; }
-keepclassmembers class cricket.player.auction.data.local.** { *; }
-keep class cricket.player.auction.data.remote.** { *; }
-keepclassmembers class cricket.player.auction.data.remote.** { *; }

# Keep Google Play Services Auth classes
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**
-keep class com.google.android.gms.tasks.** { *; }

# Gson serialization rules
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-keepclassmembers enum * { *; }
-dontwarn sun.misc.Unsafe

# Retrofit rules
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Coil image loading rules
-keep class coil.** { *; }
-dontwarn coil.**
