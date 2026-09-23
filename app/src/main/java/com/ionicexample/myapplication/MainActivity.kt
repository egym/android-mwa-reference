package com.ionicexample.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContentView(R.layout.main_activity)

        findViewById<Button>(R.id.btnOpenBioage).setOnClickListener {
            openApp("bioage")
        }

        findViewById<Button>(R.id.btnOpenWorkouts).setOnClickListener {
            openApp("workouts")
        }

        findViewById<Button>(R.id.btnOpenNfc).setOnClickListener {
            openApp("nfc")
        }

        val emailInput = findViewById<EditText>(R.id.accountLinkingEmail)
        val jwtInput = findViewById<EditText>(R.id.accountLinkingEmailJwt)
        val portalKeyInput = findViewById<EditText>(R.id.portalKey)
        findViewById<Button>(R.id.btnOpenAccountLinking).apply {
            isEnabled = BuildConfig.HAS_ACCOUNT_LINKING_BUNDLE
            setOnClickListener {
                val email = emailInput.text.toString().trim()
                val emailJwt = jwtInput.text.toString().trim()
                val portalKey = portalKeyInput.text.toString().trim()
                if (email.isEmpty()) {
                    emailInput.error = getString(R.string.account_linking_email_required)
                    return@setOnClickListener
                }
                if (emailJwt.isEmpty()) {
                    jwtInput.error = getString(R.string.account_linking_jwt_required)
                    return@setOnClickListener
                }
                if (portalKey.isEmpty()) {
                    portalKeyInput.error = getString(R.string.portal_key_required)
                    return@setOnClickListener
                }
                emailInput.text.clear()
                jwtInput.text.clear()
                portalKeyInput.text.clear()
                startActivity(Intent(this@MainActivity, IonicSampleActivity::class.java).apply {
                    putExtra("app", "account-linking")
                    putExtra("accountLinkingEmail", email)
                    putExtra("emailJWT", emailJwt)
                    putExtra("portalKey", portalKey)
                })
            }
        }
    }

    private fun openApp(app: String) {
        startActivity(Intent(this, IonicSampleActivity::class.java).putExtra("app", app))
    }
}
