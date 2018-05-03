buildscript {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.fabric.io/public") }
    }

    dependencies {
        classpath(Config.Plugins.android)
        classpath(Config.Plugins.kotlin)
        classpath(Config.Plugins.google)
        classpath(Config.Plugins.firebase)
        classpath(Config.Plugins.fabric)
        classpath(Config.Plugins.publishing)
    }

    // TMP: remove when last .gradle files are terminated
    extra["compileSdk"] = 27
    extra["targetSdk"] = 27
    extra["minSdk"] = 27
}

allprojects {
    repositories {
        google()
        jcenter()
        maven { url = uri("https://jitpack.io") }
    }

    // Resource packaging breaks otherwise for some reason
    tasks.whenTaskAdded {
        if (name.contains("Test")) enabled = false
    }
}
