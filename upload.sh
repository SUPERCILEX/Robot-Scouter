#!/bin/bash

if [ "$TRAVIS_PULL_REQUEST" = "false" ]
  mv app/build/outputs/apk/app-release.apk app-release.apk
  mv app/build/outputs/mapping/release/mapping.txt mapping.txt

  wget https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-133.0.0-linux-x86_64.tar.gz
  tar xf google-cloud-sdk-133.0.0-linux-x86_64.tar.gz
  echo "y" | ./google-cloud-sdk/bin/gcloud components update alpha
  ./google-cloud-sdk/bin/gcloud auth activate-service-account --key-file app/google-play-auto-publisher.json

  git init
  git remote add mapping $git_mapping_login
  git add mapping.txt app-release.apk
  git config --global user.email $github_email
  git config --global user.name "Alexandre Saveau"
  git commit -a -m "Added new apk + mapping"
  git push -u mapping master &> /dev/null

  ./google-cloud-sdk/bin/gcloud alpha test android run --async --app app-release.apk --device-ids m0,Nexus6P --os-version-ids 18,25 --orientations portrait --project robot-scouter-app
fi
