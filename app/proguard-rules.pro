# Proguard rules for R8 code shrinking

# Keep data models in cricket.player.auction.model
-keep class cricket.player.auction.model.** { *; }

# Gson rules
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
