#!/bin/bash -e

./gradlew app:server:clean app:server:build

cd app/server
mv build/classes/kotlin/main/firebase.js functions/index.js
firebase deploy --non-interactive
rm functions/index.js
cd ../..
