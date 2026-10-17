package com.ionicexample.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.CookieManager
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URLEncoder
import java.util.UUID

/** The optional "Log in as a BMA user" for EGYM testing, on only when bma-login.properties exists (README). */
object BmaLogin {
    const val LOG_TAG = "BmaLogin"

    private val loggedInApps = setOf("workouts", "nfc")

    val isEnabled: Boolean
        get() = BuildConfig.BMA_GALAXY_URL.isNotEmpty()

    /** In memory only, so the token never reaches the disk and a new process starts logged out. */
    @Volatile
    var session: BmaSession? = null
        private set

    /** Workouts and NFC run as the logged-in user on the channel from the file; Bioage keeps the sample. */
    fun runsLoggedIn(app: String): Boolean = isEnabled && app in loggedInApps

    /** Logs in at Galaxy and gets the MWA token. The session starts only if the calling coroutine is still active. */
    suspend fun logIn(email: String, password: String) {
        session = withContext(Dispatchers.IO) {
            val galaxy = GalaxyClient(BuildConfig.BMA_GALAXY_URL)
            val account = galaxy.logIn(email, password)
            val exerciserUuid = account.requireString("uuid")
            BmaSession(
                galaxy = galaxy,
                exerciserUuid = exerciserUuid,
                homeClubUuid = account.requireString("homeClubUuid"),
                email = if (account.isNull("email")) email else account.getString("email"),
                accessToken = galaxy.fetchMwaToken(exerciserUuid),
            )
        }
    }
}

class BmaSession(
    private val galaxy: GalaxyClient,
    private val exerciserUuid: String,
    private val homeClubUuid: String,
    private val email: String,
    accessToken: String,
) {
    @Volatile
    private var accessToken = accessToken

    /** Gets a new MWA token from Galaxy, as the BMA apps do when an MWA asks for one. */
    suspend fun renewAccessToken(): String =
        withContext(Dispatchers.IO) { galaxy.fetchMwaToken(exerciserUuid) }.also { accessToken = it }

    // No emailJWT key (it turns on the consent guard) and no mwaProtocolVersion (this host drops trackEvent).
    fun initialContext(app: String): Map<String, Any> = mapOf(
        "startingRoute" to "/$app/home",
        "clientId" to "egym",
        "authToken" to accessToken,
        "url" to BuildConfig.BMA_MWA_API_URL,
        "gymLocation" to homeClubUuid,
        "email" to email,
        "language" to "en-US",
        "preferences" to emptyMap<String, Any>(),
        "linking" to mapOf("status" to "linked", "egymEmail" to email),
    )
}

/** Galaxy's mobile login and MWA token calls. Blocking; its cookie jar keeps the login session for the token call. */
class GalaxyClient(galaxyUrl: String) {
    private val baseUrl = galaxyUrl.trimEnd('/')
    private val cookies = CookieManager()
    private val userAgent = "clientType=MOBILE_DEVICE; devicePlatform=ANDROID; deviceUid=${UUID.randomUUID()}; " +
        "applicationName=Team; applicationVersion=3.64; applicationVersionCode=501;"

    fun logIn(email: String, password: String): JSONObject =
        send("/np/login", form = "username=${email.formEncoded()}&password=${password.formEncoded()}&club_portal=true")

    fun fetchMwaToken(exerciserUuid: String): String =
        send("/np/micro-web-app/v1.0/exercisers/$exerciserUuid/tokens/FLS").requireString("accessToken")

    private fun send(path: String, form: String? = null): JSONObject {
        val uri = try {
            URI(baseUrl + path)
        } catch (e: URISyntaxException) {
            throw IOException("Invalid Galaxy URL", e)
        }
        val connection = uri.toURL().openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MILLIS
            connection.readTimeout = TIMEOUT_MILLIS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-NP-User-Agent", userAgent)
            cookies.get(uri, emptyMap())["Cookie"]?.takeIf { it.isNotEmpty() }?.let {
                connection.setRequestProperty("Cookie", it.joinToString("; "))
            }
            if (form != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                connection.outputStream.use { it.write(form.toByteArray()) }
            }
            val status = connection.responseCode
            cookies.put(uri, connection.headerFields)
            if (status !in 200..299) throw GalaxyHttpException(status)
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            return try {
                JSONObject(body)
            } catch (e: JSONException) {
                // Not chained: the parser's message quotes the body, which can hold a token.
                throw IOException("Galaxy did not answer with JSON")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun String.formEncoded(): String = URLEncoder.encode(this, "UTF-8")

    private companion object {
        const val TIMEOUT_MILLIS = 20_000
    }
}

class GalaxyHttpException(val status: Int) : IOException("Galaxy answered HTTP $status")

private fun JSONObject.requireString(name: String): String =
    if (isNull(name)) throw IOException("Galaxy answered without $name") else getString(name)
