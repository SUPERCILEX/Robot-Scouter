import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("kotlin2js")
    id("kotlin-platform-js")
}

dependencies {
    implementation(project(":library:common"))
    expectedBy(project(":library:common"))
    implementation(Config.Libs.Kotlin.js)
    implementation(Config.Libs.Kotlin.coroutinesJs)
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    kotlinOptions {
        moduleKind = "commonjs"
        outputFile = "$buildDir/classes/kotlin/main/firebase.js"
    }
}
