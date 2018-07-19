#!/bin/bash -e

if [ $TRAVIS_PULL_REQUEST = "false" ]; then
  openssl aes-256-cbc -K $encrypted_c4fd8e842577_key -iv $encrypted_c4fd8e842577_iv -in secrets.tar.enc -out secrets.tar -d &> /dev/null
  tar -xvf secrets.tar &> /dev/null

  if [ $TRAVIS_BRANCH = 'master' ]; then
    echo y | ${ANDROID_HOME}tools/bin/sdkmanager --channel=3 "build-tools;${BUILD_TOOLS_VERSION}"

    npm install -gq firebase-tools@4.0.2
    cd app/server/functions
    npm install -q
    cd ../../..

    wget -q https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-207.0.0-linux-x86_64.tar.gz
    tar xf google-cloud-sdk-207.0.0-linux-x86_64.tar.gz
    echo "y" | ./google-cloud-sdk/bin/gcloud components update alpha
    ./google-cloud-sdk/bin/gcloud alpha auth activate-service-account \
      --key-file app/android-base/google-play-auto-publisher.json
    ./google-cloud-sdk/bin/gcloud alpha config set project robot-scouter-app
  fi
else
  cp travis-dummies/keystore.jks app/android-base/keystore.jks
  cp travis-dummies/keystore.properties app/android-base/keystore.properties
  cp travis-dummies/google-services.json app/android-base/google-services.json
  cp travis-dummies/google-play-auto-publisher.json app/android-base/google-play-auto-publisher.json
  cp travis-dummies/config.xml library/core-data/src/main/res/values/config.xml
fi
