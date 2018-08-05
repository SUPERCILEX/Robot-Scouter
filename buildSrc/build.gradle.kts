import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile

repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.ajoberstar:grgit:2.3.0")
    implementation("com.google.cloud:google-cloud-pubsub:1.38.0")
    // Needed to downgrade pubsub for AGP
    implementation("com.google.guava:guava:23.0") { isForce = true }
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
    }
}
