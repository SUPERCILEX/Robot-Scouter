#!/bin/bash -e

mv travis-dummies/google-services.json app/google-services.json
mv travis-dummies/google-play-auto-publisher.json app/google-play-auto-publisher.json
mv travis-dummies/keystore.properties app/keystore.properties
mv travis-dummies/keystore.jks app/keystore.jks
mv travis-dummies/config.xml library/core-data/src/main/res/values/config.xml
