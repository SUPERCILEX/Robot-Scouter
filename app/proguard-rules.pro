# Keeps line numbers and file name obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Kotlin
-dontwarn kotlin.**
-dontwarn com.supercilex.robotscouter.util.data.TeamsLiveData$merger$1$mergeTeams$2$1$1

# In-app billing
-keep class com.android.vending.billing.**

# Retrofit
-dontnote retrofit2.Platform
-dontwarn retrofit2.**
-dontwarn okio.**
-dontwarn okhttp3.**

# Other
-dontnote com.google.**
-dontnote com.facebook.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
