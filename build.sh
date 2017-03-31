#!/bin/bash -xe

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew clean build publishApkProdRelease
else
  ./gradlew clean assembleDebug check
fi

  cd firebase
  firebase deploy
  cd ..

set +xe
