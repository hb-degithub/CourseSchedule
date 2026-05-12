# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room entities
-keep class com.hbde.courseschedule.data.local.entity.** { *; }

# Keep data classes used for JSON serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# iCal4j
-dontwarn net.fortuna.ical4j.**
-keep class net.fortuna.ical4j.** { *; }

# Apache POI
-dontwarn org.apache.poi.**
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.apache.xmlbeans.** { *; }

# OpenCSV
-dontwarn com.opencsv.**
-keep class com.opencsv.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
