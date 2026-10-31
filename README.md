# Nocturne — Ambient clock for Fire TV Stick 4K

Native Kotlin + Jetpack Compose TV app. Five faces (Minimal, Flip, Analog, Dashboard, Ambient),
Open-Meteo weather/forecast/AQI, IP-based location, sunrise/sunset, moon phase, private iCal
calendar feeds, world clocks, LAN phone notepad, brightness/night dimming, burn-in pixel shift,
boot receiver, DreamService, and interactive app mode.

## Screenshots

Actual captures from a Fire TV Stick 4K and a 390 × 844 phone viewport.

<p align="center">
  <img src="docs/screenshots/minimal-notepad.png" alt="Minimal face with a note sent from a phone" width="88%">
</p>

| Flip | Analog | Dashboard | Ambient |
| :---: | :---: | :---: | :---: |
| ![Flip clock face](docs/screenshots/flip.png) | ![Analog clock face](docs/screenshots/analog.png) | ![Dashboard face](docs/screenshots/dashboard.png) | ![Ambient face](docs/screenshots/ambient.png) |

### Phone notepad

<p align="center">
  <img src="docs/screenshots/phone-notepad.png" alt="Phone drawing page in a mobile viewport" width="300">
  <img src="docs/screenshots/settings-phone-notepad.png" alt="Phone notepad settings with live preview" width="520">
</p>

### Settings

<p align="center">
  <img src="docs/screenshots/settings.png" alt="Settings overlay" width="88%">
</p>

## Files
- `Nocturne-debug.apk` — debug APK, sideload via `adb install -r Nocturne-debug.apk`
- `source/` — Android Studio/Gradle project
- `docs/screenshots/` — captures used in the gallery above

## Build
Requires JDK 21 + Android SDK 34 + Gradle 8.9.

```bash
cd source
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Install & run on Fire TV
```bash
adb connect <fire-tv-ip>:5555
adb install -r Nocturne-debug.apk
adb shell am start -n dev.nocturne.ambient/.MainActivity
```

Remote: D-pad left/right = face, up/down = brightness, OK = settings, Menu = toggle settings,
1-5 = jump to face, Info = weather HUD, Media Play/Pause = Ambient face.

## Phone notepad
Enabled by default and shown on the Minimal face only. Toggle it in
**Settings → Phone notepad**. While enabled, the TV runs a small local web server:

1. Open `http://<fire-tv-ip>:8080` on any phone, tablet, or computer on the same Wi-Fi.
2. Draw with a finger or mouse, pick a pen size/color, then tap **Send to TV**.
3. The note appears in the right-hand area of the Minimal face and survives restarts.

Wipe it from the phone with **Remove from TV**, or on the TV with
**Settings → Phone notepad → Clear note**. Sending an empty canvas also removes it. The server
is open to anyone on the same LAN — turn the notepad off to stop it. No cloud, account, or
internet access is involved.

## Notes
- Fonts use system Amazon Ember (Thin/Regular) on Fire OS; falls back to sans-serif.
- Auto-location resolves via IP (ipapi.co/ipinfo.io) and may differ from GPS. Override in
  Settings → Weather location.
- Fire OS doesn't surface DreamService; the interactive activity with KEEP_SCREEN_ON is the
  working TV display path.

No ads, accounts, subscriptions, or telemetry.
