# Build Notes - SMSController APK

## Env Setup
```
$env:ANDROID_HOME = "C:\Users\Admin\AppData\Local\Android\Sdk"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## Gradle location
```
C:\Users\Admin\Desktop\OpenCode\S-Cals-Beta\gradle-9.3.1\bin\gradle.bat
```

## Build command (run from project root)
```powershell
& "C:\Users\Admin\Desktop\OpenCode\S-Cals-Beta\gradle-9.3.1\bin\gradle.bat" assembleDebug -x lint --no-daemon
```

## APK output
```
app/build/outputs/apk/debug/app-debug.apk
```

## Project root
```
C:\Users\Admin\Desktop\SMSController
```

## Notes
- AGP 9.0.1 + Gradle 9.3.1
- Kotlin plugin is built into AGP 9.x (no need for `org.jetbrains.kotlin.android`)
- Compose uses `org.jetbrains.kotlin.plugin.compose` plugin (Kotlin 2.2.10)
- First build downloads dependencies; subsequent builds are faster
