package com.ionicexample.myapplication

import android.os.Bundle
import android.view.WindowManager
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
import org.json.JSONObject
import java.util.UUID

private const val PORTAL_KEY = ""
private const val ACCOUNT_LINKING_APP = "account-linking"

val appToIidMap = mapOf(
    "bioage" to "068a3720",
    "workouts" to "851e0894",
    "nfc" to "dcbe378a",
    ACCOUNT_LINKING_APP to "c69c8644",
)

class IonicSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = intent.getStringExtra("app") ?: throw IllegalArgumentException("Missing app extra")

        super.onCreate(savedInstanceState)
        if (app == ACCOUNT_LINKING_APP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContentView(R.layout.activity_ionic_sample)
        addStatusBarPadding()
        if (!PortalManager.isRegistered()) {
            val portalKey = if (app == ACCOUNT_LINKING_APP) {
                intent.getStringExtra("portalKey")?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("Missing Ionic Portals key")
            } else PORTAL_KEY
            PortalManager.register(portalKey)
        }
        val appId = appToIidMap[app] ?: throw IllegalArgumentException("Invalid app: $app")
        val isAccountLinking = app == ACCOUNT_LINKING_APP
        val channelName = "reference"
        val startRoute = if (isAccountLinking) "/account-linking/intro" else "/$app/home"
        val email = if (isAccountLinking) {
            intent.getStringExtra("accountLinkingEmail")?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Missing account-linking email")
        } else null
        val emailJwt = if (isAccountLinking) {
            intent.getStringExtra("emailJWT")?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Missing emailJWT")
        } else null

        val builder = PortalBuilder(startRoute)
            .setPlugins(mutableListOf(
                PreferencesPlugin::class.java,
                CapacitorSQLitePlugin::class.java,
                CapacitorNFCPassWalletPlugin::class.java,
                // and other plugins if needed
            ))
            .setInitialContext(getInitialContext(app, email, emailJwt))
            .setStartDir(if (isAccountLinking) "account-linking-pr" else "$appId-$channelName")

        if (!isAccountLinking) {
            builder.setLiveUpdateConfig(
                context = this@IonicSampleActivity,
                liveUpdateConfig = io.ionic.liveupdates.LiveUpdate(
                    appId = appId,
                    channelName = channelName,
                ),
                true
            )
        }
        val portal: Portal = builder.create()

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

    private fun getInitialContext(app: String, email: String?, emailJwt: String?): Map<String, String> {
        val context = mutableMapOf(
            "startingRoute" to (if (app == ACCOUNT_LINKING_APP) "/account-linking/intro" else "/$app/home"),
            "email" to "email@example.com",
            "firstName" to "Oleksandr",
            "lastName" to "Usyk",
            "gymLocation" to "10001", // external gym id
            "gender" to "FEMALE", // MALE, FEMALE
            "measurementSystem" to "METRIC", // METRIC, IMPERIAL
            "dateOfBirth" to "1968-09-09",
            "language" to "de-DE",
        )
        if (app == ACCOUNT_LINKING_APP) {
            context.putAll(mapOf(
                "app" to ACCOUNT_LINKING_APP,
                "clientId" to "egym",
                "instanceId" to UUID.randomUUID().toString(),
                "email" to requireNotNull(email),
                "emailJWT" to requireNotNull(emailJwt),
                "sourceFeatureType" to "advancedWorkouts",
                "linking" to JSONObject()
                    .put("status", "unlinked")
                    .put("egymEmail", email)
                    .toString(),
            ))
        }
        return context
    }
}