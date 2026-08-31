# ABIZ LOCKER — Local Development Guide

## Prerequisites Check

Run these commands to verify your environment:

```batch
rem Check JDK (uses Android Studio JBR)
"C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -version

rem Check ADB
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" version

rem Check Node.js
node --version

rem Check npm
npm --version

rem Check Git
git --version

rem Check PostgreSQL
psql --version
```

Expected minimum versions:
- JDK: 17+ (Android Studio JBR 21 is installed and works)
- ADB: 1.0.41+
- Node.js: 18+
- npm: 9+
- Git: 2.x
- PostgreSQL: 14+ (18 is installed locally)

---

## Environment Variables

Create a `.env.local` in the project root (this file is gitignored):

```
# Backend (Java Server)
DB_NAME=abizlocker_dev
DB_USER=abizlocker
DB_PASSWORD=your_dev_password_here
DB_HOST=localhost
DB_PORT=5432

BASE_URL=https://your-cloudflare-tunnel-url.trycloudflare.com
HASH_SECRET=generate_with_openssl_rand_hex_24
SECURE_ENROLLMENT=0

# ABIZ-specific
ABIZ_JWT_SECRET=generate_with_openssl_rand_hex_32
ABIZ_ACTIVATION_CREDIT_COST=1
ABIZ_SUPER_ADMIN_EMAIL=admin@abizlocker.com
ABIZ_SUPER_ADMIN_PASSWORD=change_me_on_first_login

# Android signing (debug builds do NOT need these)
# ABIZ_RELEASE_STORE_FILE=
# ABIZ_RELEASE_STORE_PASSWORD=
# ABIZ_RELEASE_KEY_ALIAS=
# ABIZ_RELEASE_KEY_PASSWORD=

# Email (optional for dev)
SMTP_HOST=
SMTP_PORT=25
SMTP_FROM=dev@localhost
SMTP_USERNAME=
SMTP_PASSWORD=
```

---

## Step 1 — Set up PostgreSQL Database

PostgreSQL 18 is already installed. Start it:

```batch
rem Check if running:
sc query postgresql-x64-18

rem Start if needed:
net start postgresql-x64-18

rem Create database and user:
psql -U postgres -c "CREATE USER abizlocker WITH PASSWORD 'your_dev_password_here';"
psql -U postgres -c "CREATE DATABASE abizlocker_dev OWNER abizlocker;"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE abizlocker_dev TO abizlocker;"
```

---

## Step 2 — Build the Java Backend

Requires Maven. Install if not present:

```batch
rem Option A: Chocolatey
choco install maven -y

rem Option B: Download from https://maven.apache.org/download.cgi
rem Extract to C:\tools\maven and add bin\ to PATH
```

Set JAVA_HOME before building:

```batch
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

rem Build (from abiz-locker/mdmesh/)
cd mdmesh
mvn clean package -DskipTests

rem Output: server\target\launcher.war
```

---

## Step 3 — Configure the Server

Edit `mdmesh/server/conf/hmdm.properties` (or copy from example):

```properties
db.host=localhost
db.port=5432
db.name=abizlocker_dev
db.user=abizlocker
db.password=your_dev_password_here

base.url=https://your-cloudflare-tunnel.trycloudflare.com
hash.secret=your_hash_secret
```

---

## Step 4 — Start the Java Backend

```batch
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
java -jar mdmesh\server\target\launcher.war
```

Or via Maven embedded Tomcat:
```batch
cd mdmesh
mvn -pl server tomcat7:run -Dmaven.tomcat.port=8080
```

Server starts at http://localhost:8080
API available at http://localhost:8080/rest/

---

## Step 5 — Start the React Frontend

```batch
cd mdmesh\web
npm install
npm run dev
```

React dev server starts at http://localhost:5173
It proxies /rest/* to the Java backend at localhost:8080

---

## Step 6 — Set Up Cloudflare Tunnel

REQUIRED: The Android device on your local network needs HTTPS to reach the backend.

```batch
rem Install cloudflared (if not present):
rem Download from https://github.com/cloudflare/cloudflare-warp/releases
rem Or use the installed Cloudflare WARP

rem Quick tunnel (temporary, free, no account needed):
cloudflared tunnel --url http://localhost:8080

rem This gives you a URL like:
rem https://random-name.trycloudflare.com
```

Use that URL as `BASE_URL` in your .env.local and rebuild the Android APK with it.

---

## Step 7 — Build Android APK

```batch
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk

cd mdmesh\agent-android

rem First build (downloads dependencies — may take 5-15 minutes):
gradlew.bat assembleDebug

rem Output:
rem app\build\outputs\apk\debug\app-debug.apk
```

---

## Step 8 — Install APK on Physical Android Device

```batch
rem Enable Developer Mode on your phone:
rem Settings -> About Phone -> Build Number (tap 7 times)
rem Settings -> Developer Options -> USB Debugging: ON

rem Connect phone via USB cable

rem Verify connection:
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" devices

rem Install APK:
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" install -r mdmesh\agent-android\app\build\outputs\apk\debug\app-debug.apk
```

---

## Development Workflow Summary

```
Terminal 1: java -jar server.war                  (Java backend :8080)
Terminal 2: npm run dev                           (React frontend :5173)
Terminal 3: cloudflared tunnel --url http://localhost:8080  (HTTPS tunnel)
Terminal 4: adb logcat                            (Android device logs)
```

---

## Common Issues

### Gradle build fails: "SDK location not found"
Set environment variables before building:
```batch
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
set ANDROID_SDK_ROOT=%LOCALAPPDATA%\Android\Sdk
```

### Gradle build fails: "JAVA_HOME not set"
```batch
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
```

### ADB not found
```batch
set PATH=%PATH%;%LOCALAPPDATA%\Android\Sdk\platform-tools
```

### Device not detected by ADB
1. Try different USB port
2. Check USB mode on phone: "File Transfer" (MTP), not "Charging only"
3. Accept the USB debugging authorization prompt on phone
4. Some cables are charge-only: use a data cable

### Maven not found
Install with Chocolatey: `choco install maven -y`
Or download manually from maven.apache.org
