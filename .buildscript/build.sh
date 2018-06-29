#!/bin/bash -e

./gradlew clean

# See https://issuetracker.google.com/issues/79660649
mkdir -p library/common/build/libs
touch library/common/build/libs/common.jar

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew build -x lint

  # Copy APK generated from real signing key to upload to version history. Then generate APK from
  # upload signing key for publishing.
  mkdir --parents ../uploads/Robot-Scouter
  mv app/android-base/build/outputs/apk/release/android-base-release.apk $_/app-release.tmp
  mv app/upload-keystore.jks app/keystore.jks
  mv app/upload-keystore.properties app/keystore.properties
  ./gradlew publish

  .buildscript/deploy-firebase.sh
  sleep 30 # Wait long enough to circumvent function `finished with status: 'connection error'`
  ./google-cloud-sdk/bin/gcloud alpha pubsub topics publish update-default-templates --message '{}'
else
  ./gradlew assembleDebug check -x lint -x processReleaseMetadata
fi
