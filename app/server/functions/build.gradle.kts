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

tasks.withType<Kotlin2JsCompile>().configureEach {
    kotlinOptions {
        moduleKind = "commonjs"
        outputFile = "$buildDir/classes/kotlin/main/firebase.js"
    }
}

tasks.named<Delete>("clean").configure {
    delete("index.js")
}
