repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

tasks.withType<ValidatePlugins>().configureEach {
    enableStricterValidation.set(true)
}

dependencies {
    implementation("com.google.guava:guava:28.1-jre")
    implementation("org.ajoberstar.grgit:grgit-gradle:4.0.1")
    implementation("com.google.cloud:google-cloud-pubsub:1.102.0")
}
