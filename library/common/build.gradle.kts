import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("kotlin-multiplatform")
}

kotlin {
    targets {
        add(presets["jvm"].createTarget("android"))
        add(presets["js"].createTarget("server"))
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

tasks.withType<Kotlin2JsCompile>().configureEach {
    kotlinOptions.moduleKind = "commonjs"
}
