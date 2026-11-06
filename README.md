<div align="center">

<img src="docs/icon-512.png" alt="trklavye" width="160" />

# trklavye

**A Turkish Q keyboard for Android TV boxes whose firmware has no physical keyboard layout setting.**

[![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/android-6%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/java-11%2B-orange?style=flat-square)](https://adoptium.net)

</div>

## Features

- Turns any Bluetooth or USB keyboard into a Turkish Q keyboard (dotted and dotless i, c-cedilla, g-breve, o and u umlaut, s-cedilla, AltGr symbols, caps lock) by reading Linux scan codes, so it works whatever layout the system thinks the keyboard has.
- When no physical keyboard is attached, shows an on-screen Turkish Q grid you drive with the remote's D-pad; holding select on backspace repeats it.
- Arrow and Enter keys on a real keyboard keep their usual meaning.
- The app name and the on-screen key labels are a Turkish UI; code and docs are English.
- No permissions besides the input-method binding, no network access.

## Installation

Requirements: JDK 11+, Android SDK with `build-tools` and one `platforms/android-*` (`ANDROID_SDK`, default `~/android-sdk`). No Gradle.

```bash
cp signing.env.example signing.env   # point it at your keystore; the file is git-ignored
./build.sh                           # build/TurkceQKlavye.apk
adb install -r build/TurkceQKlavye.apk
```

Android installs an update over an existing copy only when both are signed with the same key, so keep a backup of your keystore. It never enters this repo.

## Usage

Pick the Turkish Q keyboard in Settings > Device Preferences > Keyboard.

On boxes driven by a remote-control app that injects its own keyboard (for example Home Assistant's Android TV Remote integration with `enable_ime` on), turn that option off or it switches the keyboard back.

## Project structure

```
AndroidManifest.xml                 input-method service, minSdk 23
src/dev/hakan/trklavye/
  TrKeyboard.java                   scan-code to Turkish Q mapping, grid visibility
  KeyGridView.java                  on-screen D-pad keyboard
res/                                icons, TV banner, input-method config
build.sh                            aapt2 + javac + d8 + apksigner, no Gradle
```

## License

MIT, see [LICENSE](LICENSE).
