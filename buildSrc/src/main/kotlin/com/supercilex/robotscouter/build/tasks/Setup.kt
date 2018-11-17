package com.supercilex.robotscouter.build.tasks

import child
import com.supercilex.robotscouter.build.internal.isRelease
import com.supercilex.robotscouter.build.internal.orNull
import com.supercilex.robotscouter.build.internal.secrets
import com.supercilex.robotscouter.build.internal.shell
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class Setup : DefaultTask() {
    @TaskAction
    fun setup() {
        extractSecrets()
        if (isRelease) prepForReleaseBuild()
    }

    private fun extractSecrets() {
        var success = extractRawSecrets()
        if (!success) success = extractEncryptedSecrets()
        if (!success) success = extractDummies()

        check(success) { "Project file extraction failed." }
    }

    private fun extractRawSecrets(): Boolean {
        val secrets = project.file("secrets.tar").orNull() ?: return false
        shell("tar -xvf ${secrets.name}")
        return true
    }

    private fun extractEncryptedSecrets(): Boolean {
        val secrets = project.file("secrets.tar.enc").orNull() ?: return false
        val key = System.getenv("encrypted_c4fd8e842577_key") ?: return false
        val iv = System.getenv("encrypted_c4fd8e842577_iv") ?: return false

        shell("openssl aes-256-cbc -K $key -iv $iv -in ${secrets.name} -out ${secrets.nameWithoutExtension} -d",
              false)

        return extractRawSecrets()
    }

    private fun extractDummies(): Boolean {
        check(!isRelease) { "Cannot use dummies for release builds." }

        val dummies = project.file("ci-dummies").listFiles() ?: return false

        for (dummy in dummies) {
            val dest = secrets.first { it.name == dummy.name }
            if (!dest.exists()) dummy.copyTo(dest)
        }

        return true
    }

    private fun prepForReleaseBuild() {
        shell("echo y | \${ANDROID_HOME}tools/bin/sdkmanager --channel=3 \"build-tools;\${BUILD_TOOLS_VERSION}\"")
        shell("npm install -gq firebase-tools@6.1.0")
        shell("npm install -q") { directory(project.child("functions").projectDir) }
    }
}
