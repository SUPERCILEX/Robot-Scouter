<p align="center">
    <a href="https://play.google.com/store/apps/details?id=com.supercilex.robotscouter&utm_source=https://github.com/SUPERCILEX/Robot-Scouter/">
        <img alt="Logo" src="https://supercilex.github.io/Robot-Scouter/assets/logo.svg" width="30%" />
    </a>
</p>

<h1 align="center">
    Robot Scouter
</h1>

<h4 align="center">
    Easy, efficient, and collaborative FIRST robot scouting
</h4>

<p align="center">
    <a href="https://travis-ci.org/SUPERCILEX/Robot-Scouter">
        <img src="https://img.shields.io/travis/SUPERCILEX/Robot-Scouter/master.svg?style=flat-square" />
    </a>
</p>

<p align="center">
    <a href="https://play.google.com/store/apps/details?id=com.supercilex.robotscouter&utm_source=https://github.com/SUPERCILEX/Robot-Scouter/">
         <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="30%" />
    </a>
</p>

Robot Scouter is an open-source Android app with three core goals:
to make FIRST competition robot scouting _easy_, _efficient_, and _collaborative_.

<img src="docs/demo.gif" width="50%" height="50%" />

## Table of Contents

- [Features](#features)
  - [Highlights](#highlights-)
  - [Scouting](#scouting-)
  - [Templates](#templates-)
  - [Exporting](#exporting-)
- [Contributing](#contributing-)
  - [Updating templates](#updating-templates-️)
  - [Learning](#learning-)
- [Environment setup](#environment-setup-)
- [Psst...](#psst-)

## Features

### Highlights ✨

- Full offline support 📡
- No setup required (i.e. no databases)
- Customizable scouting templates
- Advanced export capabilities
- Integration with [The Blue Alliance](https://www.thebluealliance.com)

### Scouting 📃

- Simple yet powerful collaboration both within and between teams 🔗
- Multiple people can scout the same team, different teams, or go solo on Android N and use split
  screen mode to scout several teams at once 😎
- Numerous metrics to choose from:
  - Header (title) 🔖
  - Checkbox (true/false) ✅
  - Stopwatch (for keeping track of cycle time) ⏱️
  - Counter (numerical) 🔢
  - Item selector (list) 📝
  - Notes (textual) 📜

### Templates 📋

- Fully customizable and independent from each year's game
- Defaults are provided for the current year's game

### Exporting 📊

- Advanced spreadsheet exporting:
  - Statistics and charts are automatically generated for each team
  - A global average sheet is included to compare teams against each other
  - Fancy formatting on devices with Android Lollipop (API 21) and above
- PSA: Excel spreadsheets can easily be
  [converted to Google Sheets](https://support.google.com/docs/answer/6055139)
- JSON exports are also supported for custom analysis (e.g. in Tableau)

## Contributing 💗

Want to add features, fix bugs, or just poke around the code? No problem! Just make sure to read
the [contribution guidelines](CONTRIBUTING.md) before getting in too deep.

If you know another language, please help
[translate Robot Scouter](https://www.transifex.com/supercilex/robot-scouter/)!

### Updating templates ⬆️

Anyone can do it! Simply follow
[the instructions](https://github.com/SUPERCILEX/Robot-Scouter/blob/master/app/server/functions/src/main/kotlin/com/supercilex/robotscouter/server/functions/Templates.kt)
throughout the file and send a pull request with your changes.

### Learning 📚

- Get familiar with [Firebase](https://firebase.google.com) to understand the core technologies behind Robot Scouter
- Take careful note of [Firestore](https://firebase.google.com/docs/firestore/) and
  [Firebase-UI](https://github.com/firebase/FirebaseUI-Android)

## Environment setup 💻

1. Run `./gradlew setup`
1. If you are working with Firestore or need access to other Firebase technologies,
   [create your own Firebase project](https://firebase.google.com/docs/android/setup#manually_add_firebase)
   and replace the dummy [google-services.json](ci-dummies/google-services.json) with the one
   created in your Firebase project
1. Run `./gradlew check` to make sure the Travis build will pass
1. That's it! 🚀

## Psst... 🤐

If you want to get the latest build from master, you can join the
[alpha testers community](https://plus.google.com/communities/111840458526472018249)
and the [beta](https://play.google.com/apps/testing/com.supercilex.robotscouter).
