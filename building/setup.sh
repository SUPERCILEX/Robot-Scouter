#!/bin/bash

echo y | ${ANDROID_HOME}tools/bin/sdkmanager --channel=3 "build-tools;${BUILD_TOOLS_VERSION}"

if [ $TRAVIS_PULL_REQUEST = "false" ]; then
  cd app
  openssl aes-256-cbc -K $encrypted_8e2b28f9b71e_key -iv $encrypted_8e2b28f9b71e_iv -in secrets.tar.enc -out secrets.tar -d &> /dev/null
  tar -xvf secrets.tar
  cd ..

  if [ $TRAVIS_BRANCH = 'master' ]; then
    npm install -g firebase-tools
    cd firebase/functions
    npm install
    cd ../..
  fi
else
  mv travis-dummies/google-services.json app/google-services.json
  mv travis-dummies/google-play-auto-publisher.json app/google-play-auto-publisher.json
  mv travis-dummies/keystore.properties app/keystore.properties
  mv travis-dummies/keystore.jks app/keystore.jks
  mv travis-dummies/config.xml app/src/main/res/values/config.xml
fi
