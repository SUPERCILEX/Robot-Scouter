#!/bin/bash

./gradlew firebase:clean firebase:build

cd firebase
mv build/classes/kotlin/main/firebase.js functions/index.js
firebase deploy
rm functions/index.js
cd ..
