package com.ionicexample.myapplication

import android.content.Context
import android.content.Intent
import android.util.Log
import io.ionic.portals.PortalsPubSub
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

/**
 * The app's listener on the shared Portals channel, where the package relays the messages of the MWAs it opened.
 * It logs each message type and answers `authToken` for the BMA login. The package closes its screens on `dismiss`.
 */
object HostMessages {
    private const val TAG = "HostMessages"

    private val scope = MainScope()
    private var subscriptionRef: Int? = null

    /** The BMA session of the MWA on screen; null for none or for one with the sample context. */
    @Volatile
    var screenSession: BmaSession? = null

    /** Call on the main thread. Later calls do nothing. */
    fun start(context: Context) {
        if (subscriptionRef != null) return
        val appContext = context.applicationContext
        subscriptionRef = PortalsPubSub.shared.subscribe("subscription") { result ->
            val type = (result.data as? JSONObject)?.optString("type")
            Log.i(TAG, "Message type $type")
            if (type == "authToken") screenSession?.let { publishNewAuthToken(appContext, it) }
        }
    }

    /** Answers the MWA's token request with a new token from Galaxy, as the BMA apps do. */
    private fun publishNewAuthToken(context: Context, session: BmaSession) {
        scope.launch {
            try {
                val token = session.renewAccessToken()
                // A login that ended or was replaced during the renewal must not hand out its token.
                if (BmaLogin.session === session) PortalsPubSub.shared.publish("authToken", token)
            } catch (e: GalaxyHttpException) {
                Log.w(BmaLogin.LOG_TAG, "Galaxy answered the MWA token request with HTTP ${e.status}")
                if (e.rejectsSession) logInAgain(context, session)
            } catch (e: IOException) {
                Log.w(BmaLogin.LOG_TAG, "Could not get a new MWA token", e)
            }
        }
    }

    /** The Galaxy session expired, so the whole app starts over at the login, once per session. */
    private fun logInAgain(context: Context, expired: BmaSession) {
        if (!BmaLogin.endSession(expired)) return
        context.startActivity(
            Intent(context, BmaLoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }
}
