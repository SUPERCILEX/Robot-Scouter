import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("kotlin2js")
}

dependencies {
    compile(project(":library:common"))

    implementation(Config.Libs.Kotlin.js)
    implementation(Config.Libs.Kotlin.coroutinesJs)
}

tasks.withType<Kotlin2JsCompile>().configureEach {
    kotlinOptions.moduleKind = "commonjs"
}

tasks.named<Delete>("clean").configure {
    delete("index.js")
}
