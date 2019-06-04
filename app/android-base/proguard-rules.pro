# Keeps line numbers and file name obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-printconfiguration build/outputs/mapping/configuration.txt

# Keep bridges
-keep class com.supercilex.robotscouter.Bridge
-keep @com.supercilex.robotscouter.Bridge class * {
    *** Companion;
}

# Ignore Kotlin errors TODO https://youtrack.jetbrains.com/issue/KT-23172
-dontwarn com.supercilex.robotscouter.**
-dontwarn kotlinx.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Apache POI - TODO remove once the next POIA version comes out
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellProtection { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellProtectionImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.STBorderStyle$Enum { *; }
