#!/bin/bash -e

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew clean build publishApkRelease

  building/deploy-firebase.sh
  ./google-cloud-sdk/bin/gcloud alpha pubsub topics publish update-default-templates --message '{}'
else
  ./gradlew clean assembleDebug check
fi
