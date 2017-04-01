#!/bin/bash

if [ $TRAVIS_PULL_REQUEST = "false" ]; then
  cd app
  openssl aes-256-cbc -K $encrypted_79745bcca107_key -iv $encrypted_79745bcca107_iv -in secrets.tar.enc -out secrets.tar -d &> /dev/null
  tar -xvf secrets.tar
  cd ..

  if [ $TRAVIS_BRANCH = 'master' ]; then
    npm install -g firebase-tools
  fi
else
  mv travis-dummies/google-services.json app/google-services.json
  mv travis-dummies/google-play-auto-publisher.json app/google-play-auto-publisher.json
  mv travis-dummies/keystore.properties app/keystore.properties
  mv travis-dummies/keystore.jks app/keystore.jks
fi
