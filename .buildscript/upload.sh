#!/bin/bash -e

if [ $TRAVIS_PULL_REQUEST = "false" ] && [ $TRAVIS_BRANCH = 'master' ]; then
  cd ..
  git clone --branch=master "https://SUPERCILEX:${GIT_TOKEN}@github.com/SUPERCILEX/app-version-history.git" uploads &> /dev/null
  git config --global user.email "saveau.alexandre@gmail.com"
  git config --global user.name "Alex Saveau"

  mv app-base.tmp uploads/Robot-Scouter/app-base.apk
  mv app-release.tmp uploads/Robot-Scouter/app-release.aab
  cp Robot-Scouter/app/android-base/build/outputs/mapping/r8/release/mapping.txt uploads/Robot-Scouter/mapping.txt
  cd uploads/Robot-Scouter

  # Duplicated in build.sh
  APK_DUMP=$(/usr/local/android-sdk/build-tools/${BUILD_TOOLS_VERSION}/aapt dump badging app-base.apk) &> /dev/null
  VERSION_CODE="$(echo ${APK_DUMP} | grep -o -P "(?<=versionCode=\047).*(?=\047 versionName)")"
  DIFF="https://github.com/SUPERCILEX/Robot-Scouter/compare/${TRAVIS_COMMIT_RANGE}"
  git add mapping.txt app-release.aab
  git commit -a -m "$(printf "${VERSION_CODE}\n${DIFF}\nFull apk dump:\n${APK_DUMP}")"
  git push -u origin master &> /dev/null
fi
