#!/bin/bash

if [[ $TRAVIS_PULL_REQUEST = "false" ]]; then
  ./gradlew clean build publishApkRelease
else
  ./gradlew clean build publishApkRelease
fi
