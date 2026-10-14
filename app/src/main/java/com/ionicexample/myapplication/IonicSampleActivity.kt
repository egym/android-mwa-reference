package com.ionicexample.myapplication

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.egym.capacitor.nfcpasswallet.CapacitorNFCPassWalletPlugin
import com.capacitorjs.plugins.preferences.PreferencesPlugin
import com.getcapacitor.CapConfig
import com.getcapacitor.community.database.sqlite.CapacitorSQLitePlugin
import io.ionic.portals.Portal
import io.ionic.portals.PortalBuilder
import io.ionic.portals.PortalView
import io.ionic.portals.PortalsPubSub
import io.ionic.portals.SubscriptionResult
import org.json.JSONObject

val appToIidMap = mapOf(
    "bioage" to "068a3720",
    "workouts" to "851e0894",
    "nfc" to "dcbe378a",
)

class IonicSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = intent.getStringExtra("app") ?: throw IllegalArgumentException("Missing app extra")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ionic_sample)
        addSystemBarsPadding()
        val appId = appToIidMap[app] ?: throw IllegalArgumentException("Invalid app: $app")
        val channelName = "reference"

        val portal: Portal = PortalBuilder("/$app/home")
            .setPlugins(mutableListOf(
                PreferencesPlugin::class.java,
                CapacitorSQLitePlugin::class.java,
                CapacitorNFCPassWalletPlugin::class.java,
                // and other plugins if needed
            ))
            .setInitialContext(getInitialContext(app))
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
        portalPubSub.subscribe("subscription") { result: SubscriptionResult ->
            val json = result.toJSObject().getJSONObject("data")
            val eventType = json.getString("type")
            when (eventType) {
                "dismiss" -> {
                    finish()
                }
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

    private fun addSystemBarsPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val insetTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            val systemBarsInsets = insets.getInsets(insetTypes)
            view.setPadding(
                systemBarsInsets.left,
                systemBarsInsets.top,
                systemBarsInsets.right,
                systemBarsInsets.bottom
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