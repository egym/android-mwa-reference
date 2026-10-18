package com.ionicexample.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.egym.capacitor.mwanavigation.EgymMwa
import com.egym.capacitor.mwanavigation.EgymMwaOpenOptions
import com.egym.capacitor.mwanavigation.EgymMwaOpenStatus

private val appIds = mapOf(
    "bioage" to "068a3720",
    "workouts" to "851e0894",
    "nfc" to "dcbe378a",
)

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BmaLogin.isEnabled && BmaLogin.session == null) {
            startActivity(Intent(this, BmaLoginActivity::class.java))
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)
        HostMessages.start(this)

        findViewById<Button>(R.id.btnOpenBioage).setOnClickListener { openMwa("bioage") }
        findViewById<Button>(R.id.btnOpenWorkouts).setOnClickListener { openMwa("workouts") }
        findViewById<Button>(R.id.btnOpenNfc).setOnClickListener { openMwa("nfc") }
    }

    /** The package shows the MWA full screen, loads its live update first and closes it on dismiss or back. */
    private fun openMwa(app: String) {
        val appId = appIds.getValue(app)
        val bmaSession = if (BmaLogin.runsLoggedIn(app)) BmaLogin.session else null
        val channel = if (bmaSession != null) BuildConfig.BMA_LIVE_UPDATE_CHANNEL else "reference"
        val options = EgymMwaOpenOptions(
            appId = appId,
            channel = channel,
            startingRoute = "/$app/home",
            initialContext = bmaSession?.initialContext(app) ?: sampleContext(app),
            // app/build.gradle can bundle a build from Appflow into assets/<appId>-<channel>.
            startDir = "$appId-$channel",
        )
        val status = EgymMwa.open(this, options) { reason -> Log.i(TAG, "$app closed by ${reason.value}") }
        if (status != EgymMwaOpenStatus.OPENED) Log.w(TAG, "$app did not open: $status")
    }

    private fun sampleContext(app: String): Map<String, String> = mapOf(
        "startingRoute" to "/$app/home",
        "email" to "email@example.com",
        "firstName" to "Oleksandr",
        "lastName" to "Usyk",
        "gymLocation" to "10001", // external gym id
        "gender" to "FEMALE", // MALE, FEMALE
        "measurementSystem" to "METRIC", // METRIC, IMPERIAL
        "dateOfBirth" to "1968-09-09",
        "language" to "de-DE",
    )

    private companion object {
        const val TAG = "MainActivity"
    }
}
