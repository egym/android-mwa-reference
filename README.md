# 📱 Android Ionic MWA Example

This project contains an example of integrating an **Ionic Micro Web App (MWA)** into an Android application.

## 🔑 What’s included

### 0. Install
1. In the root folder, run `npm install @capacitor-community/sqlite` to install the plugin.
2. In the app/build.gradle file, replace the placeholder in def ionicToken = "" with your actual token.

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
