# SMSController

**Remote control for Android devices via SMS commands**

SMSController turns your Android phone into a fully remote-controlled device. Send an SMS from any authorized number — lock the screen, trigger a loud alarm, get GPS coordinates, capture photos, wipe data, and more. No internet required, no data plan needed. Works anywhere your phone has cellular signal.

---

## Features

| Command | SMS Keyword | What It Does |
|---|---|---|
| **Lock** | `LOCK` | Instantly locks the device (requires Device Admin) |
| **Beep** | `BEEP` | Plays a full-volume alarm + vibration for a configurable duration |
| **GPS** | `GPS` | Replies with the device's current GPS coordinates (Google Maps link) |
| **Battery** | `BATTERY` | Returns battery level, temperature, and charging status |
| **Photo** | `PHOTO` | Takes a front/rear camera photo and sends it via MMS |
| **Wipe** | `WIPE` | Factory resets the device (requires Device Admin) |
| **Flash** | `FLASH` | Toggles the camera flashlight on/off |
| **Call Me** | `CALLME` | Calls the configured owner number |
| **Wi-Fi** | `WIFI` | Enables Wi-Fi |
| **Mobile Data** | `DATA` | Enables mobile data |
| **Record** | `RECORD_START` | Starts audio recording in the background |
| **Record Stop** | `RECORD_STOP` | Stops recording and sends the audio file |
| **Screenshot** | `SCREENSHOT` | Captures the screen and sends the image |

---

## How It Works

1. Install SMSController on the target Android device.
2. Grant the requested permissions (SMS, Phone, Camera, Storage, Device Admin).
3. Add one or more authorized phone numbers in the app settings.
4. From any authorized number, send an SMS with a command keyword to the target device.
5. The device executes the command silently (no notification, no log) and replies via SMS.

All communication happens over the cellular SMS channel — no internet connection required on either end.

---

## Permissions & Why

| Permission | Purpose |
|---|---|
| **SMS** | Read incoming SMS commands, send response SMS |
| **Phone** | Make outgoing calls (CALLME command) |
| **Camera** | Take photos, toggle flashlight |
| **Storage** | Save and send captured media |
| **Device Admin** | Lock screen, wipe data |
| **Location** | Get GPS coordinates |
| **Record Audio** | Background audio recording |
| **Notification Access** | Capture screenshots |
| **System Overlay** | Display UI when necessary |

---

## Installation

### Option 1: Download the APK

Grab the latest APK from the [Releases](https://github.com/YOUR_USERNAME/SMSController/releases) page.

> ⚠️ After installing, open the app and enable **Device Admin** in Settings > Security > Device Admin for LOCK and WIPE commands to work.

### Option 2: Build from Source

```bash
git clone https://github.com/YOUR_USERNAME/SMSController.git
cd SMSController
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Getting Started

### 1. Open the app

Launch SMSController on the target device. You'll see the main dashboard.

### 2. Enable the service

Toggle the switch at the top to **Enabled**.

### 3. Authorize your number(s)

Tap **Authorized Numbers** → **Add** → enter your phone number (including country code, e.g. `+1234567890`).

> If no numbers are added, **any** sender can control the device. Always add at least one authorized number for security.

### 4. Send a command

From your authorized phone, send an SMS to the target device:

```
LOCK
```

You'll receive a reply: `Device locked.`

---

## Security Features

- **Authorization list** — only approved phone numbers can send commands
- **Stealth mode** — no notification or toast shown when processing commands
- **Per-command enable/disable** — turn off commands you don't need
- **No internet required** — all communication is SMS-only, no data leak path
- **Deduplication** — protects against duplicate SMS broadcasts (common on some OEM devices)

---

## Anti-Loop Protection

Some Android OEM devices fire the `SMS_RECEIVED` broadcast multiple times for a single incoming SMS. SMSController includes two layers of protection:

1. **SMS-level dedup** — identical messages from the same sender are ignored for 45 seconds
2. **Command-level cooldown** — each command type has a built-in cooldown (e.g., LOCK: 30s, BEEP: 60s, WIPE: 120s)

This prevents runaway loops like repeated locking, endless beeping, or spam SMS replies.

---

## Build Requirements

- **Android Studio** Hedgehog (2023.1.1) or later
- **JDK** 17+
- **Android SDK** 34+
- **Gradle** 8.x+

---

## Architecture

```
app/
├── src/main/java/com/smscontroller/
│   ├── SmsReceiver.kt          # SMS broadcast receiver
│   ├── MainActivity.kt         # UI / dashboard
│   ├── SettingsActivity.kt     # Settings screen
│   ├── AdminReceiver.kt        # Device admin receiver
│   ├── SMSControllerApp.kt     # Application class
│   └── util/
│       ├── CommandExecutor.kt  # Command dispatch & execution
│       ├── SmsSender.kt        # SMS response utility
│       ├── SmsCommand.kt       # Command enum & parser
│       ├── PrefsManager.kt     # SharedPreferences wrapper
│       ├── BatteryHelper.kt    # Battery status reader
│       ├── CameraHelper.kt     # Camera & photo capture
│       ├── FlashlightHelper.kt # Flashlight toggle
│       ├── NetworkHelper.kt    # Wi-Fi / mobile data control
│       ├── GpsProvider.kt      # Location provider
│       ├── AudioRecorderHelper.kt  # Background recording
│       └── ScreenCaptureHelper.kt  # Screenshot capture
└── src/main/res/               # Layouts, strings, icons
```

---

## License

```
MIT License

Copyright (c) 2026 SMSController

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions...

[Full license text in LICENSE file]
```

---

## Disclaimer

This software is intended for **legitimate security and anti-theft purposes only**. Only install it on devices you own or are authorized to manage. Unauthorized use may violate applicable laws. The authors assume no liability for misuse.
