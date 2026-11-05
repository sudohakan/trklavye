<div align="center">

<img src="docs/icon-512.png" alt="Türkçe Q Klavye" width="160" />

# Türkçe Q Klavye

**A Turkish Q keyboard for Android TV boxes whose firmware has no physical keyboard layout setting.**

</div>

## What it does

- Turns any Bluetooth or USB keyboard into a Turkish Q keyboard (ç, ğ, ı, İ, ö, ş, ü, AltGr symbols, caps lock) by reading Linux scan codes, so it works whatever layout the system thinks the keyboard has.
- When no physical keyboard is attached, shows an on-screen Turkish Q grid you drive with the remote's D-pad; holding select on backspace repeats it.
- Arrow and Enter keys on a real keyboard keep their usual meaning.
- The app name and the on-screen key labels are a Turkish UI; code and docs are English.
- No permissions besides the input-method binding, no network access.

## Build

Requirements: JDK 11+, Android SDK with `build-tools` and one `platforms/android-*` (`ANDROID_SDK`, default `~/android-sdk`). No Gradle.

```bash
cp signing.env.example signing.env   # point it at your keystore; the file is git-ignored
./build.sh                           # build/TurkceQKlavye.apk
```

## Install

```bash
adb install -r build/TurkceQKlavye.apk
```

Then pick **Türkçe Q Klavye** in Settings → Device Preferences → Keyboard. On boxes driven by a remote-control app that injects its own keyboard (for example Home Assistant's Android TV Remote integration with `enable_ime` on), turn that option off or it switches the keyboard back.

## Layout

```
AndroidManifest.xml                 input-method service, minSdk 23
src/dev/hakan/trklavye/
  TrKeyboard.java                   scan-code → Turkish Q mapping, grid visibility
  KeyGridView.java                  on-screen D-pad keyboard
res/                                icons, TV banner, input-method config
build.sh                            aapt2 + javac + d8 + apksigner, no Gradle
```

## License

MIT, see [LICENSE](LICENSE).
