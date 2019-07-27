repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

tasks.withType<ValidateTaskProperties>().configureEach {
    enableStricterValidation = true
    failOnWarning = true
}

dependencies {
    implementation("com.google.guava:guava:27.0-jre")
    implementation("org.ajoberstar.grgit:grgit-gradle:3.0.0")
    implementation("com.google.cloud:google-cloud-pubsub:1.52.0")
}
