package com.ionicexample.myapplication

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.egym.capacitor.nfcpasswallet.CapacitorNFCPassWalletPlugin
import com.capacitorjs.plugins.preferences.PreferencesPlugin
import com.getcapacitor.community.database.sqlite.CapacitorSQLitePlugin
import io.ionic.portals.Portal
import io.ionic.portals.PortalBuilder
import io.ionic.portals.PortalManager
import io.ionic.portals.PortalView
import io.ionic.portals.PortalsPubSub
import io.ionic.portals.SubscriptionResult

private const val PORTAL_KEY = ""

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
        if (!PortalManager.isRegistered()) {
            PortalManager.register(PORTAL_KEY)
        }
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
        findViewById<FrameLayout>(R.id.mainContainer).addView(myPortalView)
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