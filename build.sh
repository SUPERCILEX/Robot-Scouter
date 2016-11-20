#!/bin/bash -xe

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH == 'master' ]; then
  ./gradlew clean build publishApkRelease
else
  ./gradlew clean assembleDebug check
fi

set +xe
