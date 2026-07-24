# Add project specific ProGuard rules here.

# Gson + Retrofit reflect on model classes and their fields by name — keep them
# fully intact (fields un-renamed, no removed no-arg constructors) or
# (de)serialization silently breaks/loses data under R8 shrinking+obfuscation.
-keep class com.fintrack.android.data.model.** { *; }
-keep class com.fintrack.android.data.sync.PendingOperation { *; }
-keep class com.fintrack.android.data.sync.PendingOpType { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Gson's TypeToken-based generic deserialization (used throughout OfflineCache/FinTrackRepository)
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

# Retrofit / OkHttp: platform-specific optional dependencies R8 can't see are safe to warn-suppress
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Retrofit service method annotations (Retrofit builds calls via reflection on these)
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep interface com.fintrack.android.data.network.FinTrackApi { *; }
-keep interface com.fintrack.android.data.network.NextcloudAuthApi { *; }
