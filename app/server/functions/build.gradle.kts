plugins {
    id("org.jetbrains.kotlin.js")
}

kotlin {
    target {
        useCommonJs()
        nodejs()
    }
}

dependencies {
    implementation(project(":library:common"))

    implementation(Config.Libs.Kotlin.js)
    implementation(Config.Libs.Kotlin.coroutinesJs)
}

tasks.named<Delete>("clean") {
    delete("upload/index.js", "upload/common/index.js")
    delete("upload/node_modules")
}
