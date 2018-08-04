#!/bin/bash -e

./gradlew clean

# See https://issuetracker.google.com/issues/79660649
mkdir -p library/common/build/libs
touch library/common/build/libs/common.jar

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew build bundleRelease -x lint -w

  # Copy APK generated from real signing key to upload to version history. Then generate APK from
  # upload signing key for publishing.
  mv app/android-base/build/outputs/apk/release/android-base-release.apk ../app-base.tmp
  mv app/android-base/build/outputs/bundle/release/android-base.aab ../app-release.tmp
  mv app/android-base/upload-keystore.jks app/android-base/keystore.jks
  mv app/android-base/upload-keystore.properties app/android-base/keystore.properties
  ./gradlew publish crashlyticsUploadDeobs

  .buildscript/deploy-firebase.sh
  sleep 30 # Wait long enough to circumvent function `finished with status: 'connection error'`
  ./google-cloud-sdk/bin/gcloud alpha pubsub topics publish update-default-templates --message '{}'
else
  ./gradlew assembleDebug check -x lint -x processReleaseMetadata
fi
