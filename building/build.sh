#!/bin/bash -xe

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew clean build publishApkRelease

# TODO It's broken in the latest release
#  cd firebase
#  firebase deploy
#  cd ..
else
  ./gradlew clean assembleDebug check
fi

set +xe
