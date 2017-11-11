#!/bin/bash

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  ./gradlew clean build publishApkRelease

# TODO It's broken in the latest release
#  cd firebase
#  firebase deploy
#  cd ..

  mv app/build/outputs/apk/release/app-release.apk app-release.apk
  # Duplicated in upload.sh
  APK_DUMP=$(/usr/local/android-sdk/build-tools/${BUILD_TOOLS_VERSION}/aapt dump badging app-release.apk) &> /dev/null
  VERSION_CODE="$(echo ${APK_DUMP} | grep -o -P "(?<=versionCode=\047).*(?=\047 versionName)")"

  sed -i "s/\(FirebaseCrashVersionCode=\).*\$/\1${VERSION_CODE}/" gradle.properties
  ./gradlew firebaseUploadArchivedProguardMapping
else
  ./gradlew clean assembleDebug check
fi
