package com.supercilex.robotscouter.build.internal

import java.util.concurrent.Executors

internal fun shell(
        command: String,
        log: Boolean = true,
        config: ProcessBuilder.() -> Unit = {}
): () -> String {
    if (log) println("$ $command")

    val process = ProcessBuilder("sh", "-c", command)
            .apply(config)
            .redirectErrorStream(true)
            .start()

    val pool = Executors.newSingleThreadExecutor()
    val pendingOutput = pool.submit<List<String>?> {
        process.inputStream?.bufferedReader()?.useLines {
            it.onEach { if (log) println(it) }.toList()
        }
    }

    val code = process.waitFor()
    check(code == 0) { "Command failed with exit code $code." }

    val output = pendingOutput.get()
    pool.shutdownNow()

    return { output?.joinToString("\n").orEmpty() }
}
