# Add project specific ProGuard rules here.

# Razorpay Checkout SDK — required rules per Razorpay's own Android integration docs.
# Without these the WebView JS bridge and payment callback silently break under R8/minify.
-keep class com.razorpay.** {*;}
-keepattributes JavascriptInterface
-keep class com.razorpay.RzpWebViewClient$JsBridge {*;}
-keepclassmembers class * implements com.razorpay.PaymentResultWithDataListener {
    public *;
}
-dontwarn com.razorpay.**
-optimizations !method/inlining/*
-keepattributes Signature,Exceptions,*Annotation*,InnerClasses,EnclosingMethod

# kotlinx.serialization — keep our own DTOs' generated serializers/fields. The compiler plugin
# wires these up by direct reference at compile time (not reflection), but this is a safety net
# since Supabase decode failures under R8 are silent JSON-shape mismatches, not compile errors.
-keepclassmembers class com.quizmaker.android.data.remote.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.quizmaker.android.data.remote.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.quizmaker.android.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.quizmaker.android.data.remote.dto.** {
    <fields>;
}

# fastexcel (.xlsx import/template) — keep whole; it isn't exercised via reflection on our own
# classes, but its own internal zip/POI-style handling isn't something we can runtime-verify
# under R8 without a device, so don't let the shrinker touch it.
-keep class org.dhatim.fastexcel.** { *; }
-dontwarn org.dhatim.fastexcel.**

# Optional codec paths pulled in transitively by fastexcel's Apache Commons Compress dependency
# (ZSTD/XZ compression, StAX XML) — .xlsx is plain zip+DEFLATE, so these are never reached at
# runtime; R8 just can't prove that statically since the classes genuinely aren't on the classpath.
-dontwarn com.github.luben.zstd.**
-dontwarn javax.xml.stream.**
-dontwarn org.tukaani.xz.**

# Multiplatform artifacts (Ktor/Supabase/coroutines) reference classes not present on Android;
# these are shrinker warnings only, not real problems.
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**
