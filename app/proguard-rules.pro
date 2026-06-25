# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
# Cloudinary ProGuard Rules
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep WorkManager classes and their constructors intact
-keep class androidx.work.** { *; }
-keep public class * extends androidx.work.Worker {
    public <init>(...);
}
-keep public class * extends androidx.work.InputMerger {
    public <init>(...);
}