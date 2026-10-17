# Proguard rules for LOF Titan EOL Test Fixture
-keep class com.hoho.android.usbserial.** { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}
-keepattributes *Annotation*
-dontwarn com.hoho.android.usbserial.**
