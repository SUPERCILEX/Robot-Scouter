import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("kotlin2js")
    id("kotlin-platform-js")
}

dependencies {
    implementation(project(":common"))
    expectedBy(project(":common"))
    implementation(Config.Libs.Kotlin.js)
}

tasks {
    "compileKotlin2Js"(Kotlin2JsCompile::class) {
        kotlinOptions {
            moduleKind = "commonjs"
            outputFile = "server/build/classes/kotlin/main/firebase.js"
        }
    }
}
