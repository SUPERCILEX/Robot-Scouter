repositories {
    jcenter()
    maven { url = uri("https://jitpack.io") }
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.google.guava:guava:26.0-jre")
    implementation("com.github.SUPERCILEX.grgit:grgit-gradle:09a7767968")
    implementation("com.google.cloud:google-cloud-pubsub:1.41.0")
}
