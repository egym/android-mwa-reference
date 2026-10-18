# 📱 Android Ionic MWA Example

This project contains an example of integrating an **Ionic Micro Web App (MWA)** into an Android application.

> This branch is the BMACG-751 proof of concept of the EGYM MWA navigation package (section 6). It installs the package from a local folder until the package has its own repo.

## 🔑 What’s included

### 0. Build from a fresh clone
1. Install Node 22 or newer. The NFC plugin needs it.
2. Use JDK 21. The JDK inside Android Studio works. On macOS: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.
3. Tell Gradle where the Android SDK is: set `sdk.dir` in `local.properties`, or set `ANDROID_HOME`. Android Studio writes `local.properties` when it opens the project.
4. Get the package folder `lib-capacitor-mwa-navigation` and run `npm install` in it. `package.json` points at it with the path `../../libs/lib-capacitor-mwa-navigation`, relative to this folder.
5. In the root folder, run `npm ci`. If the package folder sits elsewhere, run `npm install <path to the package folder>` instead.
6. Run `./gradlew assembleDebug`, or Run in Android Studio.

Run `npm ci` again after any change to `package.json` or `package-lock.json`. Gradle builds the plugin code inside `node_modules`.

The EGYM package opens each MWA full screen (`MainActivity.kt`). Before each open it syncs the MWA's live update, as the BMA apps do, so the first open on a fresh install shows no error page: a loading view shows until the newest build is on the device. The sync waits at most 30 seconds. After that, or when the sync fails, the package opens a build downloaded earlier or a build preloaded into the assets, and with none it shows "Try again" and "Close".

### 1. Preload an MWA build (optional)
The `build.gradle` (app module) has Gradle tasks that **download and unzip the MWA build from Appflow** into the `assets` directory. They run only when you give an Appflow token:
1. Install the [Appflow CLI](https://ionic.io/docs/appflow/cli).
2. Run `./gradlew assembleDebug -PionicToken=<token>` or `IONIC_TOKEN=<token> ./gradlew assembleDebug`.

Never write the token into a file in the repo. Prefer `IONIC_TOKEN` set from a secret store, because a typed `-PionicToken` stays in the shell history. Do not run the preload with `--info`, because it logs the command line. To drop a preloaded bundle, run `./gradlew cleanMwaAssets`.

### 2. Open an MWA
Each button in `MainActivity.kt` calls `EgymMwa.open` with the app id, the live update channel, the starting route, the initial context and `startDir`, the assets folder `<appId>-<channel>` that section 1 can fill. The package shows the MWA full screen and closes it on `dismiss` or Android back. The close callback gets the reason.

### 3. Messages from the MWAs
The package relays every message that an MWA it opened sends on the topic `subscription` to the shared Portals channel, except `dismiss`, which closes its screen. `HostMessages.kt` listens there, logs each message type with the tag `HostMessages`, and answers `authToken` for the BMA login (section 5). The app has no `dismiss` handler.

### 4. Capacitor plugins
The package screen registers the package's own plugin list. A Capacitor plugin that the MWAs need goes into the package, not into this app.

### 5. Log in as a BMA user (optional, for EGYM testing)
By default the MWAs get the sample context from `MainActivity.kt` and run without a logged-in member. For EGYM testing, the app can first log in as a BMA (Galaxy) user and then open Workouts and NFC as that user, the way the BMA apps do:
1. Copy `bma-login.properties.example` to `bma-login.properties` in the root folder. Git ignores this file.
2. Fill in its three keys with the values of an EGYM test environment: `galaxyUrl` (the Galaxy host of the brand), `mwaApiUrl` (the mwa-api host that the MWAs call) and `liveUpdateChannel` (the Appflow channel for Workouts and NFC).
3. Build and run the app. It starts with "Log in as a BMA user".

After the login, Workouts and NFC open on that channel with `clientId` `egym`, the MWA token as `authToken`, the mwa-api host as `url`, and the user's home gym and email. Bioage keeps the sample context and the channel `reference`. When an MWA asks for a new token (type `authToken` on the topic `subscription`), `HostMessages.kt` gets one from Galaxy and publishes it on the topic `authToken`, and the package passes it on to the MWA. When Galaxy no longer accepts the session, the app starts over at the login.

The password goes only to Galaxy and is never stored or logged. The token stays in memory, so a new app process starts at the login again. Never commit the file, a user, a password or a host name. To build without the login, delete the file.

### 6. The EGYM MWA navigation package (proof of concept)
`@egym/capacitor-mwa-navigation` brings Capacitor 8, Ionic Portals 0.13, Live Updates 0.5 and nine Capacitor plugins (Preferences, App, Haptics, Keyboard, Status Bar, Browser, Device, community SQLite and the EGYM NFC wallet plugin). Its open call `EgymMwa.open` shows an MWA full screen with one shared live update loader. Its plugin `EgymMwaNavigation` lets an MWA open another MWA full screen (today only NFC, `dcbe378a`). The package closes both on `dismiss`, so the host writes no live update or `dismiss` code.

What this app does for it:
- `package.json`: the package and `@capacitor/android`, nothing else.
- `settings.gradle`: includes the package's Gradle module and the SQLite and NFC modules from `node_modules`.
- `app/build.gradle`: one line, `implementation project(':egym-capacitor-mwa-navigation')`.
- `MainActivity.kt`: each MWA opens with `EgymMwa.open`. The package screen pads the system bars and brings the plugin list, the Portal config and the loading and error views.

Lines the package cannot take over:
- The `include` and `projectDir` lines in `settings.gradle`. Only the app's settings can add Gradle projects, and SQLite and the NFC plugin reach Android only as Gradle modules in `node_modules`.
- `@capacitor/android` and the `:capacitor-android` project, because the SQLite module depends on `project(':capacitor-android')`.

The EGYM NFC wallet plugin comes with the package, so `package.json` does not list it. The NFC MWA calls it as `CapacitorNFCPassWallet`: on its home screen it asks `isWalletAvailable` and shows "Add to Google Wallet" only when the answer is true. When that button shows, the plugin works in this app.

The package's Status Bar plugin turns on the MWAs' own safe area padding (40px at the top, 20px at the bottom), and the package screen sets the CSS safe areas to 0, because it pads natively. MWA builds made before the safe area fix of BMACG-582 use fixed values, so they show the padding twice. The Workouts and NFC builds on the channel `reference` are such builds.

Try the plugin without a login: open Workouts, connect Chrome DevTools to its WebView (`chrome://inspect`) and run `Capacitor.Plugins.EgymMwaNavigation.open({ appId: 'dcbe378a', channel: 'reference', startingRoute: '/nfc/home', initialContext: { ...JSON.parse(AndroidInitialContext.initialContext()).value, startingRoute: '/nfc/home' } })`. NFC opens full screen; its back arrow resolves the call with `closedBy: 'dismiss'`, Android back with `closedBy: 'back'`.

---

## ✅ Summary
This project demonstrates the **full integration cycle of MWA**:  
**preloading the bundle → rendering it on screen → handling event communication between Android and Ionic.**
