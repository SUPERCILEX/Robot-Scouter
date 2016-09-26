# For Firebase reflection to work
-keepclassmembers class com.supercilex.robotscouter.models.** {
  *;
}

# Retrofit
-dontwarn okio.**
-dontnote retrofit2.Platform
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes Exceptions
