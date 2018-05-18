#!/bin/bash -e

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew clean build publishApkRelease

  building/deploy-firebase.sh
  sleep 30 # Wait long enough to circumvent function `finished with status: 'connection error'`
  ./google-cloud-sdk/bin/gcloud alpha pubsub topics publish update-default-templates --message '{}'
else
  ./gradlew clean assembleDebug check
fi
