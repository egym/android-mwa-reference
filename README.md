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

On a fresh install, the first open of each MWA shows an error page while its live update downloads. Go back and open it again.

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

---

## ✅ Summary
This project demonstrates the **full integration cycle of MWA**:  
**preloading the bundle → rendering it on screen → handling event communication between Android and Ionic.**
