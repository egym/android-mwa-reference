package com.ionicexample.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import com.egym.capacitor.mwanavigation.EgymMwa
import com.getcapacitor.CapConfig
import io.ionic.portals.Portal
import io.ionic.portals.PortalBuilder
import io.ionic.portals.PortalView
import io.ionic.portals.PortalsPubSub
import io.ionic.portals.SubscriptionResult
import kotlinx.coroutines.launch
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
                false
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

        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnTryAgain).setOnClickListener { loadPortal(portal, appId) }
        loadPortal(portal, appId)
    }

    /** Shows the Portal only after its live update is on the device, so the first open is never blank. */
    private fun loadPortal(portal: Portal, appId: String) {
        showLoadStatus(isLoading = true)
        LiveUpdateLoader.load(this, appId) { hasBuild ->
            if (isFinishing || isDestroyed) return@load
            if (hasBuild) {
                lifecycleScope.launch { lifecycle.withStarted { showPortal(portal) } }
            } else {
                showLoadStatus(isLoading = false)
            }
        }
    }

    private fun showLoadStatus(isLoading: Boolean) {
        findViewById<View>(R.id.mwaLoading).isVisible = isLoading
        findViewById<View>(R.id.mwaLoadFailed).isVisible = !isLoading
        findViewById<View>(R.id.btnTryAgain).isVisible = !isLoading
    }

    private fun showPortal(portal: Portal) {
        findViewById<View>(R.id.mwaStatus).isVisible = false
        val myPortalView = PortalView(this@IonicSampleActivity, portal)
        // Capacitor 8 pads the window for the system bars; this activity does its own padding.
        // Back closes this screen; the App plugin from the package would take it and do nothing.
        myPortalView.getPortalFragment()?.setConfig(
            CapConfig.Builder(this@IonicSampleActivity)
                .setInitialFocus(false)
                .setPluginsConfiguration(
                    JSONObject()
                        .put("SystemBars", JSONObject().put("insetsHandling", "disable"))
                        .put("App", JSONObject().put("disableBackButtonHandler", true))
                )
                .create()
        )
        myPortalView.getPortalFragment()?.addWebViewListener(EgymMwa.zeroCssSafeAreaListener())
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
                PortalsPubSub.shared.publish("authToken", session.renewAccessToken())
            } catch (e: GalaxyHttpException) {
                Log.w(BmaLogin.LOG_TAG, "Galaxy answered the MWA token request with HTTP ${e.status}")
                if (e.rejectsSession) logInAgain(session)
            } catch (e: IOException) {
                Log.w(BmaLogin.LOG_TAG, "Could not get a new MWA token", e)
            }
        }
    }

    /** The Galaxy session expired, so the whole app starts over at the login, once per session. */
    private fun logInAgain(expired: BmaSession) {
        if (!BmaLogin.endSession(expired)) return
        startActivity(
            Intent(this, BmaLoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
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