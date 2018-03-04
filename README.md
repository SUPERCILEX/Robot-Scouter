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

Robot Scouter is an open-source Android app with three core goals to make FIRST competition robot
scouting easy, efficient, and collaborative.

<img src="docs/demo.gif" width="50%" height="50%" />

## Features

- **Full offline support** 📡
- **No setup required i.e. no databases** 🗄️ (managing databases = 💩)
- Easy sharing between users 🔗
- Seamless collaboration: multiple people can scout the same team, different teams, or go solo on
  Android N and use split screen mode to scout several teams at once.
- **Customizable scouting templates** 📃 with several different metrics to choose from:
  - header/title 🔖
  - true/false (checkbox)
  - stopwatch (for keeping track of cycle time) ⏱️
  - number (counter)
  - list (item selector)
  - text (notes) 📜
- **Integration with [The Blue Alliance](https://www.thebluealliance.com)** to automatically populate relevant team info
- **Advanced spreadsheet exporting:**
  - Averages are computed for each team automatically
  - A global average sheet is included to compare teams against each other 📊
  - Fancy formatting on devices with Android Lollipop (21) and above 🎀
  - **Charts!** 📈
  - PSA: Excel spreadsheets can easily be
    [converted to Google Sheets](https://support.google.com/docs/answer/6055139).
- And more!

## Contributing 💗

Want to add features, fix bugs, or just poke around the code? No problem! Just make sure to read
the [contribution guidelines](.github/CONTRIBUTING.md) before getting in too deep.

If you know another language, please help
[translate Robot Scouter](https://www.transifex.com/supercilex/robot-scouter/)!

### Learning 📚
- Get familiar with [Firebase](https://firebase.google.com) to understand the core technologies behind Robot Scouter
- Take careful note of [Firestore](https://firebase.google.com/docs/firestore/) and
  [Firebase-UI](https://github.com/firebase/FirebaseUI-Android)

## Environment setup 💻

1. [Move the dummy files](https://github.com/SUPERCILEX/Robot-Scouter/blob/083ee0b7fd672d09ecaddf73d777d4fe9c4612d2/building/setup.sh#L25-L29)
   into position
1. If you are working with Firestore or need access to other Firebase technologies,
   [create your own Firebase project](https://firebase.google.com/docs/android/setup#manually_add_firebase)
   and replace the dummy [google-services.json](travis-dummies/google-services.json) with the one
   created in your Firebase project
1. Run `./gradlew check` to make sure the Travis build will pass
1. That's it! 🚀

**Note:** to improve build performance, pass in the `devBuild` flag to Gradle by searching for
`Gradle-Android Compiler` in Intellij and adding `-PdevBuild` to the CLI options.

## Psst... 🤐

If you want to get the latest build from master, you can join the
[alpha testers community](https://plus.google.com/communities/111840458526472018249)
and the [beta](https://play.google.com/apps/testing/com.supercilex.robotscouter).
