package com.supercilex.robotscouter.build.internal

internal val isMaster = System.getenv("TRAVIS_BRANCH") == "master"
internal val isPr = System.getenv("TRAVIS_PULL_REQUEST") ?: "false" != "false"
internal val isRelease = isMaster && !isPr
