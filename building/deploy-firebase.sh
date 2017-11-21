#!/bin/bash

./gradlew firebase:build

cd firebase
mv build/classes/kotlin/main/firebase.js functions/index.js
firebase deploy --only functions # TODO Fix Firestore rules
rm functions/index.js
cd ..
