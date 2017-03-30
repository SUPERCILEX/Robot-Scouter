#!/bin/bash

if [ $TRAVIS_PULL_REQUEST = "false" ]; then
  openssl aes-256-cbc -K $encrypted_8e2b28f9b71e_key -iv $encrypted_8e2b28f9b71e_iv -in secrets.tar.enc -out secrets.tar -d &> /dev/null
  tar xvf secrets.tar
else
  mv travis-dummies/google-services.json app/google-services.json
  mv travis-dummies/google-play-auto-publisher.json app/google-play-auto-publisher.json
  mv travis-dummies/keystore.properties keystore.properties
  mv travis-dummies/keystore.jks app/keystore.jks
fi
