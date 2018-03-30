# Keeps line numbers and file name obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keepnames public class * extends com.supercilex.robotscouter.util.ui.FragmentBase
-keepnames public class * extends com.supercilex.robotscouter.util.ui.DialogFragmentBase
-keepnames public class * extends com.supercilex.robotscouter.util.ui.BottomSheetDialogFragmentBase
-keepnames public class * extends com.supercilex.robotscouter.util.ui.PreferenceFragmentBase

# Ignore Kotlin errors TODO https://youtrack.jetbrains.com/issue/KT-23172
-dontwarn com.supercilex.robotscouter.**

# Retrofit
-dontwarn okhttp3.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
