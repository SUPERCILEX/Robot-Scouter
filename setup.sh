#!/bin/bash -xe

if [ $TRAVIS_PULL_REQUEST = "false" ]; then
  openssl aes-256-cbc -K $encrypted_b2fd50a80bbe_key -iv $encrypted_b2fd50a80bbe_iv -in secrets.tar.enc -out secrets.tar -d
  tar xvf secrets.tar
else
  mv travis-dummies/google-services.json app/google-services.json
  mv travis-dummies/keystore.properties keystore.properties
  mv travis-dummies/keystore.jks app/keystore.jks
fi

set +xe
