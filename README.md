# 📱 Android Ionic MWA Example

This project contains an example of integrating an **Ionic Micro Web App (MWA)** into an Android application.

## 🔑 What’s included

### 0. Install
1. In the root folder, run `npm ci` to install the Capacitor plugins.
2. For the existing reference-channel preload (not needed for the account-linking
   harness), authenticate the Appflow CLI locally; never put an Appflow token in
   Gradle or source files.

### 1. Preloading MWA assets
The `build.gradle` (app module) is configured with custom Gradle tasks to automatically **download and unzip the MWA build from Appflow** into the `assets` directory.

### 2. Working with MWA in `IonicSampleActivity`
- Registering the **`PortalManager`**
- Creating a **`Portal`** with an **`initialContext`**
- Enabling **Live Updates** (fetching updated MWA bundles without releasing a new app version)
- Adding a **`PortalView`** to the layout to display the Ionic screen

### 3. PubSub event subscriptions
The project demonstrates how to:
- Subscribe to events sent from the web app (e.g., `dismiss`)

---

## ✅ Summary
This project demonstrates the **full integration cycle of MWA**:  
**preloading the bundle → rendering it on screen → handling event communication between Android and Ionic.**

### Verify the standalone MWA account-linking intro

This path is **MWA**, not the ext-1-only rMWA BioAge/Workouts membership flow. Build the
`bma-account-linking` web app from the PR checkout (Appflow app ID `c69c8644`), then
point Gradle at its **built** directory containing `index.html`. Use the INT build
for this reproducer (`VITE_BE_URL=https://mwa-api.int.api.egym.com`); a token from
the setup guide's staging example will not work against INT:

```bash
npm ci
./gradlew :app:assembleDebug -PaccountLinkingBundleDir=/absolute/path/to/PR/apps/bma-account-linking/dist
```

This embeds that directory as `account-linking-pr` in the APK and disables Live Updates
**for account linking only**, so a reference-channel release cannot replace the PR
build. It also skips the unrelated BioAge Appflow download for this build; the existing
BioAge, Workouts and NFC launch buttons remain available, but need their own preloaded
assets to render offline. Without `accountLinkingBundleDir`, the account-linking button
is disabled and the existing reference-channel preload still runs.

Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`, open the app
on the device/emulator, enter the email of a half-baked **unlinked** INT EGYM account
and paste a **fresh INT emailJWT** for that exerciser into the password field.
Also enter a valid Ionic Portals key at runtime (the
reference app's original key placeholder is blank), then tap **Open account linking
PR build**. Credentials are entered only at runtime, never stored in Gradle,
assets, logs or saved form state.
The harness supplies `/account-linking/intro`, `sourceFeatureType=advancedWorkouts`,
`linking.status=unlinked`, `clientId=egym` and a fresh `instanceId`. The PR build's
`VITE_BE_URL` must target the intended MWA API environment: the native `/mwa/api`
requests use the URL compiled into the web bundle, **not** the Android context URL.
Confirm the read-only `POST /mwa/api/egym-accounts/bma/v2.0/user-status` returns HTTP
204; the intro should show creation copy and Continue should open consent at
`/account-linking/consent?flow=complete`. Do not run the setup guide's settings/MMS
write calls without approval. Do not paste JWTs into source, `local.properties`,
Gradle properties, shell arguments, or bug reports.
