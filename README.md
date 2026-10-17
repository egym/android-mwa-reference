# 📱 Android Ionic MWA Example

This project contains an example of integrating an **Ionic Micro Web App (MWA)** into an Android application.

## 🔑 What’s included

### 0. Build from a fresh clone
1. Install Node 22 or newer. The NFC plugin needs it.
2. Use JDK 21. The JDK inside Android Studio works. On macOS: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
3. Tell Gradle where the Android SDK is: set `sdk.dir` in `local.properties`, or set `ANDROID_HOME`. Android Studio writes `local.properties` when it opens the project.
4. In the root folder, run `npm ci`.
5. Run `./gradlew assembleDebug`, or Run in Android Studio.

Run `npm ci` again after any change to `package.json` or `package-lock.json`. Gradle builds the plugin code inside `node_modules`.

Before each open the app syncs the MWA's live update, as the BMA apps do, so the first open on a fresh install shows no error page: a loading view shows until the newest build is on the device (`LiveUpdateLoader.kt`). The sync waits at most 30 seconds. After that, or when the sync fails, the app opens a build downloaded earlier, and with none it shows "Try again" and "Close".

### 1. Preload an MWA build (optional)
The `build.gradle` (app module) has Gradle tasks that **download and unzip the MWA build from Appflow** into the `assets` directory. They run only when you give an Appflow token:
1. Install the [Appflow CLI](https://ionic.io/docs/appflow/cli).
2. Run `./gradlew assembleDebug -PionicToken=<token>` or `IONIC_TOKEN=<token> ./gradlew assembleDebug`.

Never write the token into a file in the repo. Prefer `IONIC_TOKEN` set from a secret store, because a typed `-PionicToken` stays in the shell history. Do not run the preload with `--info`, because it logs the command line. To drop a preloaded bundle, run `./gradlew cleanMwaAssets`.

### 2. Working with MWA in `IonicSampleActivity`
- Creating a **`Portal`** with an **`initialContext`**
- Enabling **Live Updates** (fetching updated MWA bundles without releasing a new app version)
- Adding a **`PortalView`** to the layout to display the Ionic screen

### 3. PubSub event subscriptions
The project demonstrates how to:
- Subscribe to events sent from the web app (e.g., `dismiss`)

### 4. Add an npm Capacitor plugin
1. Run `npm install <plugin>`.
2. In `settings.gradle`, add the module to the `include` line and set `project(':<name>').projectDir = new File('node_modules/<plugin>/android')`.
3. In `app/build.gradle`, add `implementation project(':<name>')`.
4. Add the plugin class to `setPlugins(...)` in `IonicSampleActivity.kt`.

A plugin from Maven instead (like Preferences) needs `exclude group: 'com.capacitorjs', module: 'core'` on its line, so the app keeps one Capacitor core.

### 5. Log in as a BMA user (optional, for EGYM testing)
By default the MWAs get the sample context from `IonicSampleActivity` and run without a logged-in member. For EGYM testing, the app can first log in as a BMA (Galaxy) user and then open Workouts and NFC as that user, the way the BMA apps do:
1. Copy `bma-login.properties.example` to `bma-login.properties` in the root folder. Git ignores this file.
2. Fill in its three keys with the values of an EGYM test environment: `galaxyUrl` (the Galaxy host of the brand), `mwaApiUrl` (the mwa-api host that the MWAs call) and `liveUpdateChannel` (the Appflow channel for Workouts and NFC).
3. Build and run the app. It starts with "Log in as a BMA user".

After the login, Workouts and NFC open on that channel with `clientId` `egym`, the MWA token as `authToken`, the mwa-api host as `url`, and the user's home gym and email. Bioage keeps the sample context and the channel `reference`. When an MWA asks for a new token (type `authToken` on the topic `subscription`), the app gets one from Galaxy and publishes it on the topic `authToken`. When Galaxy no longer accepts the session, the app starts over at the login.

The password goes only to Galaxy and is never stored or logged. The token stays in memory, so a new app process starts at the login again. Never commit the file, a user, a password or a host name. To build without the login, delete the file.

---

## ✅ Summary
This project demonstrates the **full integration cycle of MWA**:  
**preloading the bundle → rendering it on screen → handling event communication between Android and Ionic.**
