# 📱 Android Ionic MWA Example

This project contains an example of integrating an **Ionic Micro Web App (MWA)** into an Android application.

> This branch is the BMACG-751 proof of concept of the EGYM MWA navigation package (section 6). Do not merge it: it installs the package from a local folder that has no repo yet.

## 🔑 What’s included

### 0. Build from a fresh clone
1. Install Node 22 or newer. The NFC plugin needs it.
2. Use JDK 21. The JDK inside Android Studio works. On macOS: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
3. Tell Gradle where the Android SDK is: set `sdk.dir` in `local.properties`, or set `ANDROID_HOME`. Android Studio writes `local.properties` when it opens the project.
4. Get the package folder `lib-capacitor-mwa-navigation` and run `npm install` in it. `package.json` points at it with the path `../../libs/lib-capacitor-mwa-navigation`, relative to this folder.
5. In the root folder, run `npm ci`. If the package folder sits elsewhere, run `npm install <path to the package folder>` instead.
6. Run `./gradlew assembleDebug`, or Run in Android Studio.

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
4. Add the plugin class to the list in `setPlugins(...)` in `IonicSampleActivity.kt`, for example `setPlugins((EgymMwa.portalPlugins + MyPlugin::class.java).toMutableList())`.

A plugin from Maven instead needs `exclude group: 'com.capacitorjs', module: 'core'` on its line, so the app keeps one Capacitor core.

### 5. Log in as a BMA user (optional, for EGYM testing)
By default the MWAs get the sample context from `IonicSampleActivity` and run without a logged-in member. For EGYM testing, the app can first log in as a BMA (Galaxy) user and then open Workouts and NFC as that user, the way the BMA apps do:
1. Copy `bma-login.properties.example` to `bma-login.properties` in the root folder. Git ignores this file.
2. Fill in its three keys with the values of an EGYM test environment: `galaxyUrl` (the Galaxy host of the brand), `mwaApiUrl` (the mwa-api host that the MWAs call) and `liveUpdateChannel` (the Appflow channel for Workouts and NFC).
3. Build and run the app. It starts with "Log in as a BMA user".

After the login, Workouts and NFC open on that channel with `clientId` `egym`, the MWA token as `authToken`, the mwa-api host as `url`, and the user's home gym and email. Bioage keeps the sample context and the channel `reference`. When an MWA asks for a new token (type `authToken` on the topic `subscription`), the app gets one from Galaxy and publishes it on the topic `authToken`.

The password goes only to Galaxy and is never stored or logged. The token stays in memory, so a new app process starts at the login again. Never commit the file, a user, a password or a host name. To build without the login, delete the file.

### 6. The EGYM MWA navigation package (proof of concept)
`@egym/capacitor-mwa-navigation` brings Capacitor 8, Ionic Portals 0.13, Live Updates 0.5 and nine Capacitor plugins (Preferences, App, Haptics, Keyboard, Status Bar, Browser, Device, community SQLite and the EGYM NFC wallet plugin). It also brings the plugin `EgymMwaNavigation`, which lets an MWA open another MWA full screen (today only NFC, `dcbe378a`) and closes it again when that MWA sends `dismiss`. The host writes no code for the opened MWA.

What this app does for it:
- `package.json`: the package and `@capacitor/android`, nothing else.
- `settings.gradle`: includes the package's Gradle module and the SQLite and NFC modules from `node_modules`.
- `app/build.gradle`: one line, `implementation project(':egym-capacitor-mwa-navigation')`.
- `IonicSampleActivity.kt`: every Portal gets `setPlugins(EgymMwa.portalPlugins.toMutableList())` and `addWebViewListener(EgymMwa.zeroCssSafeAreaListener())`.

Lines the package cannot take over:
- The `include` and `projectDir` lines in `settings.gradle`. Only the app's settings can add Gradle projects, and SQLite and the NFC plugin reach Android only as Gradle modules in `node_modules`.
- `@capacitor/android` and the `:capacitor-android` project, because the SQLite module depends on `project(':capacitor-android')`.
- The `CapConfig` of each Portal: the SystemBars opt-out, because this activity pads the system bars, and `App` `disableBackButtonHandler`, because the package's App plugin would take Android back and do nothing when the MWA has no back handler.
- `EgymMwa.zeroCssSafeAreaListener()`. The package's Status Bar plugin turns on the MWAs' own safe area padding (40px at the top, 20px at the bottom); the listener sets the CSS safe areas to 0, because this activity pads natively. MWA builds made before the safe area fix of BMACG-582 use fixed values that the listener cannot undo, so they show the padding twice. The Workouts and NFC builds on the channel `reference` are such builds.

Try the plugin without a login: open Workouts, connect Chrome DevTools to its WebView (`chrome://inspect`) and run `Capacitor.Plugins.EgymMwaNavigation.open({ appId: 'dcbe378a', channel: 'reference', startingRoute: '/nfc/home', initialContext: { ...JSON.parse(AndroidInitialContext.initialContext()).value, startingRoute: '/nfc/home' } })`. NFC opens full screen; its back arrow resolves the call with `closedBy: 'dismiss'`, Android back with `closedBy: 'back'`.

---

## ✅ Summary
This project demonstrates the **full integration cycle of MWA**:  
**preloading the bundle → rendering it on screen → handling event communication between Android and Ionic.**
