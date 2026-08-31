# ABIZ LOCKER — Android CLI Build Guide
## No Android Studio Required

---

## Environment Setup (one-time)

Add to your PowerShell profile or set before each build session:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
```

Or set persistently via System Properties -> Environment Variables.

---

## Verified Component Versions (this machine)

| Component | Version | Location |
|-----------|---------|----------|
| JDK | 21.0.8 (OpenJDK / JBR) | C:\Program Files\Android\Android Studio\jbr |
| ADB | 1.0.41 (version 36.0.0) | %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe |
| Android SDK | Build-tools 35.0.0, 36.1.0 | %LOCALAPPDATA%\Android\Sdk\build-tools |
| Android Platform | android-36 | %LOCALAPPDATA%\Android\Sdk\platforms |
| Gradle | 8.13 (cached) | %USERPROFILE%\.gradle\wrapper\dists |

---

## Android Project Location

```
abiz-locker/mdmesh/agent-android/
├── gradlew              # Linux/Mac wrapper
├── gradlew.bat          # Windows wrapper (use this)
├── build.gradle.kts     # Root build file
├── app/
│   └── build.gradle.kts # App module config
├── core/
├── policy/
├── kiosk/
├── oem/
├── proto/
├── remote/
└── gradle/
    ├── libs.versions.toml      # Version catalog
    └── wrapper/
        ├── gradle-wrapper.jar
        └── gradle-wrapper.properties
```

---

## Build Commands

Navigate to agent-android first:
```batch
cd C:\Users\MNA\.gemini\antigravity\scratch\abiz-locker\mdmesh\agent-android
```

### Debug APK (development/testing)
```batch
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk

gradlew.bat assembleDebug

rem Output: app\build\outputs\apk\debug\app-debug.apk
rem ApplicationId: com.abiz.locker.debug
```

### Release APK (production — requires keystore)
```batch
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
set ABIZ_RELEASE_STORE_FILE=C:\path\to\secure\abiz-locker-release.jks
set ABIZ_RELEASE_STORE_PASSWORD=<from password manager>
set ABIZ_RELEASE_KEY_ALIAS=abiz-locker-release
set ABIZ_RELEASE_KEY_PASSWORD=<from password manager>

gradlew.bat assembleRelease

rem Output: app\build\outputs\apk\release\app-release.apk
rem ApplicationId: com.abiz.locker
```

### Unit Tests
```batch
gradlew.bat test
```

### Clean build
```batch
gradlew.bat clean assembleDebug
```

### List all available tasks
```batch
gradlew.bat tasks
```

---

## First Build Notes

The first build downloads:
- AGP (Android Gradle Plugin) ~50MB
- All Kotlin/AndroidX dependencies ~200MB+
- Compilation caches

Estimated first build time on low-end PC: 10-30 minutes (depends on internet speed)
Subsequent builds: 2-5 minutes

Build output is cached in `%USERPROFILE%\.gradle\caches` and `agent-android/.gradle`.

---

## Gradle Wrapper Version Note

The gradle-wrapper.properties was updated to use Gradle 8.13 (already cached on this machine).
Original MDMesh specified 8.10.2. Gradle 8.13 is backward compatible with AGP 8.6.0.

If you need to restore to 8.10.2 (online download required):
```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
```

---

## Common Build Errors

### "SDK location not found"
Set ANDROID_HOME before building.

### "JAVA_HOME not set" or wrong JDK
Set JAVA_HOME to Android Studio JBR.

### "Could not resolve AGP"
Check internet connection. First build requires downloading all dependencies.

### "Execution failed for task :app:dexDebug"
Usually a duplicate class. Check if multiple dependencies bring in conflicting versions.

### "Gradle build daemon disappeared unexpectedly"
Low memory. Close other applications. Add `--no-daemon` flag:
```batch
gradlew.bat assembleDebug --no-daemon
```
