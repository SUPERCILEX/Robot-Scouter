#!/bin/bash -xe

#if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
#  ./gradlew clean build publishApkRelease
#
#  cd firebase
#  firebase deploy
#  firebase database:set --confirm /default-template default-template.json
#  cd ..
#else
#  ./gradlew clean assembleDebug check
#fi

# TODO Those stupid Gradle plugin bugs are really annoying
./gradlew clean assembleDebug

set +xe
