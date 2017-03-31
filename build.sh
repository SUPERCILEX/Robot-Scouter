#!/bin/bash -xe

cd firebase
firebase deploy
firebase database:set --confirm /default-template default-template.json
cd ..


if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew clean build publishApkProdRelease
else
  ./gradlew clean assembleDebug check
fi

set +xe
