plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm("android")

    js("server") {
        useCommonJs()
        nodejs()
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(Config.Libs.Kotlin.common)
            }
        }

        named("androidMain") {
            dependencies {
                implementation(Config.Libs.Kotlin.jvm)
            }
        }

        named("serverMain") {
            dependencies {
                implementation(Config.Libs.Kotlin.js)
            }
        }
    }
}
