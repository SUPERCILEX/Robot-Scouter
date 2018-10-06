repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.google.guava:guava:26.0-jre")
    implementation("org.ajoberstar.grgit:grgit-gradle:3.0.0-rc.2")
    implementation("com.google.cloud:google-cloud-pubsub:1.41.0")
}
