# FeedbackKit consumer ProGuard rules

# Keep all public API classes
-keep class com.swiftlydeveloped.feedbackkit.** { *; }

# Keep Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.swiftlydeveloped.feedbackkit.**$$serializer { *; }
-keepclassmembers class com.swiftlydeveloped.feedbackkit.** {
    *** Companion;
}
-keepclasseswithmembers class com.swiftlydeveloped.feedbackkit.** {
    kotlinx.serialization.KSerializer serializer(...);
}
