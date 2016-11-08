#!/bin/bash

set -v

if [ $TRAVIS_PULL_REQUEST = "false" ]; then
  openssl aes-256-cbc -K $encrypted_2efb677b0ec0_key -iv $encrypted_2efb677b0ec0_iv -in secrets.tar.enc -out secrets.tar -d
  tar -xvf secrets.tar
  mv keystore.jks app/keystore.jks
  mv google-services.json app/google-services.json
  mv google-play-auto-publisher.json app/google-play-auto-publisher.json
else
  mv travis-dummies/google-services.json app/google-services.json
  mv travis-dummies/keystore.properties keystore.properties
  mv travis-dummies/keystore.jks app/keystore.jks
fi

set +v
