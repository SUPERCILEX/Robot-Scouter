#!/bin/bash

if [ $TRAVIS_PULL_REQUEST = "false" ]; then
  openssl aes-256-cbc -K $encrypted_c4fd8e842577_key -iv $encrypted_c4fd8e842577_iv -in secrets.tar.enc -out secrets.tar -d &> /dev/null
  tar xvf secrets.tar
else
  mv travis-dummies/google-services.json app/google-services.json
  mv travis-dummies/keystore.properties keystore.properties
  mv travis-dummies/keystore.jks app/keystore.jks
fi
