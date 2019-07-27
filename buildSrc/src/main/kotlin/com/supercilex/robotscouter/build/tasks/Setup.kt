package com.supercilex.robotscouter.build.tasks

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
    }

    private fun extractSecrets() {
        var success = extractRawSecrets()
        if (!success) success = extractEncryptedSecrets()
        if (!success) success = extractDummies()

        check(success) { "Project file extraction failed." }
    }

    private fun extractRawSecrets(): Boolean {
        val secrets = project.file("secrets.tar").orNull() ?: return false
        shell("tar -xvf ${secrets.name}", false)
        return true
    }

    private fun extractEncryptedSecrets(): Boolean {
        val secrets = project.file("secrets.tar.enc").orNull() ?: return false
        val password = System.getenv("SECRETS_PASS") ?: return false

        shell("openssl aes-256-cbc -md sha256 -d -k '$password'" +
                      " -in ${secrets.name} -out ${secrets.nameWithoutExtension}",
              false)

        return extractRawSecrets()
    }

    private fun extractDummies(): Boolean {
        check(!isRelease) { "Cannot use dummies for release builds." }

        val dummies = project.file("ci-dummies").listFiles() ?: return false

        for (dummy in dummies) {
            val dest = secrets.full.first { it.name == dummy.name }
            if (!dest.exists()) dummy.copyTo(dest)
        }

        return true
    }
}
