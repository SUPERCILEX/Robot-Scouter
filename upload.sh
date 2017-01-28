#!/bin/bash

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH == 'master' ]; then
  mv app/build/outputs/apk/app-prod-release.apk app-release.apk
  cd ..
  git clone --branch=master "https://SUPERCILEX:${git_mapping_login}@github.com/SUPERCILEX/mapping.txt-files.git" uploads &> /dev/null
  git config --global user.email "saveau.alexandre@gmail.com"
  git config --global user.name "Alex Saveau"

  cp Robot-Scouter/app-release.apk uploads/app-release.apk
  mv Robot-Scouter/app/build/outputs/mapping/prod/release/mapping.txt uploads/mapping.txt
  cd uploads

  APK_INFO=$(/usr/local/android-sdk/build-tools/25.0.2/aapt dump badging app-release.apk) &> /dev/null
  VERSION_CODE=$(echo ${APK_INFO} | grep 'versionCode=' | awk -F: 'match($0,"versionCode="){ print substr($2,37,19)}' | tr -d "'") &> /dev/null
  git add mapping.txt app-release.apk
  git commit -a -m "$(printf "${VERSION_CODE}\n Full apk dump: ${APK_INFO}")"
  git push -u origin master &> /dev/null

  cd ..
  cd Robot-Scouter

  wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-141.0.0-linux-x86_64.tar.gz
  tar xf google-cloud-sdk-141.0.0-linux-x86_64.tar.gz
  echo "y" | ./google-cloud-sdk/bin/gcloud components update alpha
  ./google-cloud-sdk/bin/gcloud auth activate-service-account --key-file app/google-play-auto-publisher.json
  ./google-cloud-sdk/bin/gcloud alpha test android run --async --app app-release.apk --device-ids m0,Nexus6P --os-version-ids 18,25 --orientations portrait --max-depth 100 --robo-directives team_number=2521 --project robot-scouter-app
fi
