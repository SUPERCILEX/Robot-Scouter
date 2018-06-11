#!/bin/bash -e

cp travis-dummies/google-services.json app/google-services.json
cp travis-dummies/google-play-auto-publisher.json app/google-play-auto-publisher.json
cp travis-dummies/keystore.properties app/keystore.properties
cp travis-dummies/keystore.jks app/keystore.jks
cp travis-dummies/config.xml library/core-data/src/main/res/values/config.xml
