#!/bin/bash -e

cp travis-dummies/google-services.json app/android-base/google-services.json
cp travis-dummies/google-play-auto-publisher.json app/android-base/google-play-auto-publisher.json
cp travis-dummies/keystore.properties app/android-base/keystore.properties
cp travis-dummies/keystore.jks app/android-base/keystore.jks
cp travis-dummies/config.xml library/core-data/src/main/res/values/config.xml
