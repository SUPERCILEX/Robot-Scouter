#!/bin/bash

if [[ $TRAVIS_PULL_REQUEST = "false" ]]; then
  mv app/build/outputs/apk/app-release.apk app-release.apk

  wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-133.0.0-linux-x86_64.tar.gz
  tar xf google-cloud-sdk-133.0.0-linux-x86_64.tar.gz
  echo "y" | ./google-cloud-sdk/bin/gcloud components update alpha
  ./google-cloud-sdk/bin/gcloud auth activate-service-account --key-file app/google-play-auto-publisher.json

  cd ..
  git clone --branch=master $git_mapping_login uploads
  mv Robot-Scouter/app-release.apk uploads/app-release.apk
  mv Robot-Scouter/app/build/outputs/mapping/release/mapping.txt uploads/mapping.txt
  cd uploads

  VERSION_CODE="$HOME/.android-sdk/build-tools/25.0.0/aapt dump badging app-release.apk"
  echo $VERSION_CODE

  git add mapping.txt app-release.apk
  git config --global user.email $github_email
  git config --global user.name "Alexandre Saveau"
  git commit -a -m $VERSION_CODE
  git push -u origin master &> /dev/null

  cd ..
  cd Robot-Scouter

  ./google-cloud-sdk/bin/gcloud alpha test android run --async --app app-release.apk --device-ids m0,Nexus6P --os-version-ids 18,25 --orientations portrait --project robot-scouter-app
fi
