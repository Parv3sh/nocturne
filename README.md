# Nocturne — Ambient clock for Fire TV Stick 4K

Native Kotlin + Jetpack Compose TV app. Five faces (Minimal, Flip, Analog, Dashboard, Ambient),
Open-Meteo weather/forecast/AQI, IP-based location, sunrise/sunset, moon phase, private iCal
calendar feeds, world clocks, brightness/night dimming, burn-in pixel shift, boot receiver,
DreamService, and interactive app mode.

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

## Notes
- Fonts use system Amazon Ember (Thin/Regular) on Fire OS; falls back to sans-serif.
- Auto-location resolves via IP (ipapi.co/ipinfo.io) and may differ from GPS. Override in
  Settings → Weather location.
- Fire OS doesn't surface DreamService; the interactive activity with KEEP_SCREEN_ON is the
  working TV display path.

No ads, accounts, subscriptions, or telemetry.
