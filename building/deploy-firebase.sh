#!/bin/bash -e

./gradlew server:clean server:build

cd server
mv build/classes/kotlin/main/firebase.js functions/index.js
firebase deploy
rm functions/index.js
cd ..
