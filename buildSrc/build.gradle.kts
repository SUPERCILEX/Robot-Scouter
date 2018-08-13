repositories {
    jcenter()
    maven { url = uri("https://jitpack.io") }
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.github.SUPERCILEX.grgit:grgit-gradle:09a7767968")
    implementation("com.google.cloud:google-cloud-pubsub:1.38.0")
    // Needed to downgrade pubsub for AGP
    implementation("com.google.guava:guava:23.0") { isForce = true }
}
