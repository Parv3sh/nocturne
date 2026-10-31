# Nocturne — Ambient clock for Fire TV Stick 4K

Native Kotlin + Jetpack Compose TV app. Five faces (Minimal, Flip, Analog, Dashboard, Ambient),
Open-Meteo weather/forecast/AQI, IP-based location, sunrise/sunset, moon phase, private iCal
calendar feeds, world clocks, LAN phone notepad, brightness/night dimming, burn-in pixel shift,
boot receiver, DreamService, and interactive app mode.

## Files
- `Nocturne-debug.apk` — debug APK, sideload via `adb install -r Nocturne-debug.apk`
- `source/` — Android Studio/Gradle project

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
