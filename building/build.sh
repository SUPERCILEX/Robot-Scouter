#!/bin/bash -xe

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew clean build publishApkRelease

  cd firebase
  firebase deploy
  firebase database:set --confirm /default-templates default-templates.json
  cd ..
else
  ./gradlew clean assembleDebug check
fi

set +xe
