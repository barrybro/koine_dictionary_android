# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers so crash reports from the minified release still point at the right line;
# Play Console deobfuscates them with the mapping file bundled into the AAB.
-keepattributes SourceFile,LineNumberTable

# Hide the original source file names, which line numbers don't need.
-renamesourcefileattribute SourceFile