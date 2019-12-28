package com.supercilex.robotscouter.build.internal

import org.eclipse.jgit.util.io.NullOutputStream
import org.gradle.process.ExecSpec

internal fun ExecSpec.redactLogs() {
    standardOutput = NullOutputStream.INSTANCE
    errorOutput = NullOutputStream.INSTANCE
}
