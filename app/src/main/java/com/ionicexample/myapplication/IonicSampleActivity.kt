package com.ionicexample.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.egym.capacitor.mwanavigation.EgymMwa
import com.getcapacitor.CapConfig
import io.ionic.portals.Portal
import io.ionic.portals.PortalBuilder
import io.ionic.portals.PortalView
import io.ionic.portals.PortalsPubSub
import io.ionic.portals.SubscriptionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

val appToIidMap = mapOf(
    "bioage" to "068a3720",
    "workouts" to "851e0894",
    "nfc" to "dcbe378a",
)

class IonicSampleActivity : AppCompatActivity() {
    private var subscriptionRef: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = intent.getStringExtra("app") ?: throw IllegalArgumentException("Missing app extra")

        super.onCreate(savedInstanceState)
        val runsLoggedIn = BmaLogin.runsLoggedIn(app)
        val bmaSession = if (runsLoggedIn) BmaLogin.session else null
        if (runsLoggedIn && bmaSession == null) {
            // A new process starts logged out, so go back to MainActivity, which shows the login.
            finish()
            return
        }
        setContentView(R.layout.activity_ionic_sample)
        addSystemBarsPadding()
        val appId = appToIidMap[app] ?: throw IllegalArgumentException("Invalid app: $app")
        val channelName = if (bmaSession != null) BuildConfig.BMA_LIVE_UPDATE_CHANNEL else "reference"

        val portal: Portal = PortalBuilder("/$app/home")
            .setPlugins(EgymMwa.portalPlugins.toMutableList())
            .setInitialContext(bmaSession?.initialContext(app) ?: getInitialContext(app))
            .setStartDir("$appId-$channelName") // directory with preloaded web app from assets
            .setLiveUpdateConfig(
                context = this@IonicSampleActivity,
                liveUpdateConfig = io.ionic.liveupdates.LiveUpdate(
                    appId = appId,
                    channelName = channelName,
                ),
                true
            )
            .create()

        val portalPubSub = PortalsPubSub.shared
        subscriptionRef = portalPubSub.subscribe("subscription") { result: SubscriptionResult ->
            val json = result.toJSObject().getJSONObject("data")
            val eventType = json.getString("type")
            when (eventType) {
                "dismiss" -> {
                    finish()
                }
                "authToken" -> bmaSession?.let(::publishNewAuthToken)
            }
        }

        val myPortalView = PortalView(this@IonicSampleActivity, portal)
        // Capacitor 8 pads the window for the system bars; this activity does its own padding.
        myPortalView.getPortalFragment()?.setConfig(
            CapConfig.Builder(this@IonicSampleActivity)
                .setInitialFocus(false)
                .setPluginsConfiguration(JSONObject().put("SystemBars", JSONObject().put("insetsHandling", "disable")))
                .create()
        )
        findViewById<FrameLayout>(R.id.mainContainer).addView(myPortalView)
    }

    override fun onDestroy() {
        subscriptionRef?.let { PortalsPubSub.shared.unsubscribe("subscription", it) }
        super.onDestroy()
    }

    /** Answers the MWA's token request with a new token from Galaxy, as the BMA apps do. */
    private fun publishNewAuthToken(session: BmaSession) {
        lifecycleScope.launch {
            try {
                val token = withContext(Dispatchers.IO) { session.renewAccessToken() }
                PortalsPubSub.shared.publish("authToken", token)
            } catch (e: IOException) {
                Log.w(BmaLogin.LOG_TAG, "Could not get a new MWA token", e)
            }
        }
    }

    private fun addSystemBarsPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val insetTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            val systemBarsInsets = insets.getInsets(insetTypes)
            val keyboardInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                maxOf(systemBarsInsets.bottom, keyboardInsets.bottom)
            )

            // Pass zero bar insets on, so WebView 140+ does not pad again; CONSUMED would stop its safe area updates.
            WindowInsetsCompat.Builder(insets).setInsets(insetTypes, Insets.NONE).build()
        }
    }

    private fun getInitialContext(app: String): Map<String, String> {
        return mapOf(
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
    }
}