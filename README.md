# Robot Scouter — A scouting app for FIRST Robotics Competition Teams

[![Build Status](https://travis-ci.org/SUPERCILEX/Robot-Scouter.svg?branch=master)](https://travis-ci.org/SUPERCILEX/Robot-Scouter)

<a href='https://play.google.com/store/apps/details?id=com.supercilex.robotscouter&utm_source=https://github.com/SUPERCILEX/Robot-Scouter/&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>

FirebaseUI is an open-source library for Android that allows you to
quickly connect common UI elements to [Firebase](https://firebase.google.com)
APIs like the Realtime Database or Firebase Authentication.

A compatible FirebaseUI client is also available for [iOS](https://github.com/firebase/firebaseui-ios).

## Table of Content

  1. [Installation](#installation)
  1. [Usage](#usage)
  1. [Sample App](#sample-app)
  1. [Contributing](#contributing)

An Android app for accessing information about the FIRST Robotics Competition. This is a native mobile version of [The Blue Alliance](http://www.thebluealliance.com).

The app has been released to the [Play Store](https://play.google.com/store/apps/details?id=com.thebluealliance.androidclient&hl=en)! We also have a [beta tester community](https://plus.google.com/communities/108444518980185742549) that you can join to try some of the latest and greatest features without having to build the app yourself. Just remember, there may still be issues. If you find any, please report them on the issue tracker so we can fix them.

Contributing
============

Want to add features, fix bugs, or just poke around the code? No problem!

### Project Management 
 - Keep up to date with the [mailing list](https://groups.google.com/forum/#!forum/thebluealliance-developers).
 - Read through the [planning documents](https://drive.google.com/#folders/0B5RO2Yzh2z01MDBOVXYwM1lXdFk) so you know what's going on.
 - Chat with us on our [Slack team](https://the-blue-alliance.slack.com/). (Request an invite in the mailing list.)

### Learning
 - Read through the [Project Wiki](https://github.com/the-blue-alliance/the-blue-alliance-android/wiki) to get comfortable with some of the technologies we use.
 - Learn some basics of [Dependency Injection](https://en.wikipedia.org/wiki/Dependency_injection), specifically [Dagger2](http://google.github.io/dagger/), to understand one of the core components of our app.

### Make Commits!
1. Set up your development environment if you haven't used Android Studio before ([see below](#setup)).
2. Fork this repository, import the project to your IDE, and create a branch for your changes.
3. Make, commit, and push your changes to your branch.
4. Submit a pull request here and we'll review it and get it added in!

For more detailed instructions, see [GitHub's Guide to Contributing](https://guides.github.com/activities/contributing-to-open-source/).

<a name="style"></a>
Code Style
----------

We use the base [Android Code Style](https://github.com/android/platform_development/blob/master/ide/intellij/codestyles/AndroidStyle.xml).
When you set up Android Studio, copy `AndroidStyle.xml` into your config directory,
  * on Linux: `~/.AndroidStudioXX/config/codestyles/`
  * on OSX: `~/Library/Preferences/AndroidStudioXX/codestyles/`

Then start Android Studio, open Settings -> Editor -> Code Style, and in the
 dropdown, select `AndroidStyle`.

### <a name="setup"></a>
Environment Setup
-----------------

1. Ensure that you have git installed and that it is added to your system's `PATH` variable. You should open you system's shell, navigate to a git repository (like this one), run ```git status```, and get status info.
2. If you haven't already, make sure you have the Android development environment set up. You will need to have [Android Studio](https://developer.android.com/sdk/installing/studio.html) and a [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) installed.
3. See [Tips and Tricks](https://developer.android.com/sdk/installing/studio-tips.html) for developing with Android Studio.
4. Use the [Android SDK Manager](https://developer.android.com/tools/help/sdk-manager.html) to download the correct versions of the Android libraries. You will need to download the Android SDK Tools, Android SDK Platform-Tools, and the SDK Platform for Android. See `build.gradle` and `android/build.gradle` for the currently needed versions.
5. If you have an Android device to test on, make sure to [enable USB Debugging](http://stackoverflow.com/questions/16707137/how-to-find-and-turn-on-usb-debugging-mode-on-nexus-4) in its Settings menu. Otherwise, [configure an Android Virtual Device (AVD)](https://developer.android.com/tools/devices/managing-avds.html) to debug with. To use a virtual device, you'll have to download an Android System image via the SDK manager. Android x86-based System images run much faster than ARM-based System images but they require [Virtual Machine Acceleration](http://developer.android.com/tools/devices/emulator.html#accel-vm). Note that the SDK manager will install the Intel HAXM _installer_ on your local disk; you still have to run that installer unless the Android Studio installer [does it for you](http://developer.android.com/tools/studio/index.html#install-updates).
6. Set up a Firebase project to enable login authentication:
    1. Open the [Firebase console](https://console.firebase.google.com/).
    1. Create a Firebase project. (If you have a TBA dev web site AppEngine project, you can use that project to link them together. That ought to prove useful, but it's not required.)
    1. Click "Add Firebase to your Android app".
    1. Enter the package name `com.thebluealliance.androidclient.development` into the form.
    1. Follow [these instructions](https://developers.google.com/android/guides/client-auth) to determine your debug SHA1. Enter that into the form.
    1. Continue. It should download a `google-services.json` file.
    1. Move the downloaded `google-services.json` file into `android/src/debug/`. ***Note:*** _Don't_ put it in the directory shown in the "Add Firebase to your Android app" instructions.
    1. You should be able to build and run the app. If you have FirebaseCrash in use, it'll throw an exception in app init if the file is invalid.
6. To run the unit tests, do `./gradlew test`

### <a name="mytba"></a>
myTBA Debug Setup
------------------

Debug builds of the TBA app cannot receive (Google Cloud Messaging) push notifications from production TBA servers. To test the myTBA features of the app, e.g. to test push notifications end-to-end, you must set up a debug [TBA server](https://github.com/the-blue-alliance/the-blue-alliance) then configure the server and temporarily modify the app code. For help setting up your own development server to test myTBA with, see [this wiki page](https://github.com/the-blue-alliance/the-blue-alliance-android/wiki/myTBA-Configuration).

But you don't need to do all that to test the app's local handling of push notifications. The easy way is to run `scripts/test_notification.py`. It uses adb to send notification Intents locally.

### Contributor License Agreements

We'd love to accept your sample apps and patches! Before we can take them, we
have to jump a couple of legal hurdles.

Please fill out either the individual or corporate Contributor License Agreement
(CLA).

  * If you are an individual writing original source code and you're sure you
    own the intellectual property, then you'll need to sign an
    [individual CLA](https://developers.google.com/open-source/cla/individual).
  * If you work for a company that wants to allow you to contribute your work,
    then you'll need to sign a
    [corporate CLA](https://developers.google.com/open-source/cla/corporate).

Follow either of the two links above to access the appropriate CLA and
instructions for how to sign and return it. Once we receive it, we'll be able to
accept your pull requests.

### Contribution Process

1. Submit an issue describing your proposed change to the repo in question.
1. The repo owner will respond to your issue promptly.
1. If your proposed change is accepted, and you haven't already done so, sign a
   Contributor License Agreement (see details above).
1. Fork the desired repo, develop and test your code changes.
1. Ensure that your code adheres to the existing style of the library to which
   you are contributing.
1. Ensure that your code has an appropriate set of unit tests which all pass.
1. Submit a pull request and cc @puf or @samtstern
