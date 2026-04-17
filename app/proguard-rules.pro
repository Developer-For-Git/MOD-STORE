# Add project specific ProGuard rules here.

# Moshi
-keep class com.sorwe.store.data.model.** { *; }
-keepclassmembers class com.sorwe.store.data.model.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes Exceptions

# Room
-keep class * extends androidx.room.RoomDatabase
