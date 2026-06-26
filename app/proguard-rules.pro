# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.baseline.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.baseline.model.**$$serializer { *; }
-keepclasseswithmembers class com.baseline.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
