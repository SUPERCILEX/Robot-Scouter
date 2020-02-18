repositories {
    google()
    jcenter()
}

plugins {
    `kotlin-dsl`
}

tasks.withType<ValidatePlugins>().configureEach {
    enableStricterValidation.set(true)
}

dependencies {
    implementation("com.android.tools.build:gradle:4.0.0-alpha09")
    implementation("org.ajoberstar.grgit:grgit-gradle:4.0.1")
    implementation("com.google.cloud:google-cloud-pubsub:1.102.0")

    // TODO remove when GPP 2.7 ships
    implementation("com.google.guava:guava:28.1-jre")
}
