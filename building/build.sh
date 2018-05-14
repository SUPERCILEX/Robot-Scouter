#!/bin/bash -e

./gradlew clean

# See https://issuetracker.google.com/issues/79660649
mkdir -p library/common/build/libs
touch library/common/build/libs/common.jar

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew build -x lint

#  ./gradlew build publishApkRelease --no-parallel
#
#  building/deploy-firebase.sh
#  sleep 30 # Wait long enough to circumvent function `finished with status: 'connection error'`
#  ./google-cloud-sdk/bin/gcloud alpha pubsub topics publish update-default-templates --message '{}'
else
  ./gradlew assembleDebug check -x lint -x processPackageMetadataRelease
fi
