#!/bin/bash

if [ "$TRAVIS_PULL_REQUEST" = "false" ]
  ./gradlew clean build publishApkRelease
else
  ./gradlew clean build publishApkRelease
fi
