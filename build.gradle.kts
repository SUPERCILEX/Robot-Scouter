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
    extra["compileSdk"] = Config.SdkVersions.compile
    extra["targetSdk"] = Config.SdkVersions.target
    extra["minSdk"] = Config.SdkVersions.min
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
