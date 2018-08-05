#!/bin/bash -e

./gradlew app:server:functions:clean app:server:functions:build

cd app/server
mv functions/build/classes/kotlin/main/firebase.js functions/index.js
firebase deploy --non-interactive
rm functions/index.js
cd ../..
