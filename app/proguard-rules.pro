# Proguard rules for R8 code shrinking

# Keep data models and local database schema classes
-keep class cricket.player.auction.model.** { *; }
-keep class cricket.player.auction.data.local.** { *; }

# Keep Firebase classes and annotations
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Google Play Services Auth classes
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.auth.**

# Gson serialization rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.Unsafe
-keep class com.google.gson.** { *; }

# Retrofit rules
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# Coil image loading rules
-keep class coil.** { *; }
-dontwarn coil.**

