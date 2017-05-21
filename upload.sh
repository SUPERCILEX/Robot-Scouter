#!/bin/bash

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH == 'master' ]; then
  cp app/build/outputs/apk/app-release.apk app-release.apk
  cd ..
  git clone --branch=master "https://SUPERCILEX:${GIT_MAPPING_LOGIN}@github.com/SUPERCILEX/app-version-history.git" uploads &> /dev/null
  git config --global user.email "saveau.alexandre@gmail.com"
  git config --global user.name "Alex Saveau"

  cp Robot-Scouter/app-release.apk uploads/Robot-Scouter/app-release.apk
  cp Robot-Scouter/app/build/outputs/mapping/release/mapping.txt uploads/Robot-Scouter/mapping.txt
  cd uploads/Robot-Scouter

  APK_DUMP=$(/usr/local/android-sdk/build-tools/26.0.0-preview/aapt dump badging app-release.apk) &> /dev/null
  VERSION_CODE="$(echo ${APK_DUMP} | grep -o -P "(?<=versionCode=\047).*(?=\047 versionName)")"
  DIFF="https://github.com/SUPERCILEX/Robot-Scouter/compare/${TRAVIS_COMMIT_RANGE}"
  git add mapping.txt app-release.apk
  git commit -a -m "$(printf "${VERSION_CODE}\n${DIFF}\nFull apk dump:\n${APK_DUMP}")"
  git push -u origin master &> /dev/null

  cd ../..
  cd Robot-Scouter

  sed -i "s/\(FirebaseCrashVersionCode=\).*\$/\1${VERSION_CODE}/" gradle.properties
  ./gradlew firebaseUploadArchivedProguardMapping

  wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-155.0.0-linux-x86_64.tar.gz
  tar xf google-cloud-sdk-155.0.0-linux-x86_64.tar.gz
  echo "y" | ./google-cloud-sdk/bin/gcloud components update alpha
  ./google-cloud-sdk/bin/gcloud auth activate-service-account --key-file app/google-play-auto-publisher.json
  ./google-cloud-sdk/bin/gcloud alpha firebase test android run --project robot-scouter-app --app app-release.apk \
    --async --timeout=30m \
    --device model=sailfish,version=26 --device model=m0,version=18 \
    --device model=Nexus9,version=25 --device model=NexusLowRes,version=25 \
    --max-depth 100 --robo-directives team_number=2521
fi
