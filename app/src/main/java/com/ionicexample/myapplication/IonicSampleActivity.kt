package com.ionicexample.myapplication

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capacitorjs.plugins.preferences.PreferencesPlugin
import com.getcapacitor.community.database.sqlite.CapacitorSQLitePlugin
import io.ionic.portals.Portal
import io.ionic.portals.PortalBuilder
import io.ionic.portals.PortalManager
import io.ionic.portals.PortalView
import io.ionic.portals.PortalsPubSub
import io.ionic.portals.SubscriptionResult

private const val PORTAL_KEY = ""

val initialContext = mapOf(
    "startingRoute" to "/bioage/home",
    "email" to "email@example.com",
    "firstName" to "Oleksandr",
    "lastName" to "Usyk",
    "gymLocation" to "10001", // external gym id
    "gender" to "FEMALE", // MALE, FEMALE
    "measurementSystem" to "METRIC", // METRIC, IMPERIAL
    "dateOfBirth" to "1968-09-09",
    "language" to "de-DE",
)

class IonicSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = intent.getStringExtra("app")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ionic_sample)
        addStatusBarPadding()
        if (!PortalManager.isRegistered()) {
            PortalManager.register(PORTAL_KEY)
        }
        val appId = if (app == "bioage") "068a3720" else "851e0894"
        val channelName = "sportcitydevelop"

        val portal: Portal = PortalBuilder("/bioage/home")
            .setPlugins(mutableListOf(
                PreferencesPlugin::class.java,
                CapacitorSQLitePlugin::class.java,
                // and other plugins if needed
            ))
            .setInitialContext(initialContext)
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

    private fun addStatusBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                view.paddingLeft,
                systemBarsInsets.top,
                view.paddingRight,
                view.paddingBottom
            )

            insets
        }
    }
}