package com.ionicexample.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ionicexample.myapplication.databinding.ActivityBmaLoginBinding
import kotlinx.coroutines.launch
import java.io.IOException

/** Logs in as a BMA (Galaxy) user before the MWA buttons show. Only started when [BmaLogin.isEnabled]. */
class BmaLoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBmaLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBmaLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val padding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.btnLogIn.setOnClickListener { logIn() }
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            (actionId == EditorInfo.IME_ACTION_DONE).also { done -> if (done) logIn() }
        }
    }

    private fun logIn() {
        if (binding.loginProgress.isVisible) return
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.bma_login_missing_fields))
            return
        }

        showProgress(true)
        lifecycleScope.launch {
            try {
                BmaLogin.logIn(email, password)
                startActivity(Intent(this@BmaLoginActivity, MainActivity::class.java))
                finish()
            } catch (e: GalaxyHttpException) {
                Log.w(BmaLogin.LOG_TAG, "Login failed with HTTP ${e.status}")
                showError(getString(R.string.bma_login_failed_status, e.status))
            } catch (e: IOException) {
                Log.w(BmaLogin.LOG_TAG, "Login failed", e)
                showError(getString(R.string.bma_login_failed))
            }
        }
    }

    private fun showProgress(inProgress: Boolean) {
        binding.loginProgress.isVisible = inProgress
        binding.btnLogIn.isEnabled = !inProgress
        if (inProgress) binding.loginError.isVisible = false
    }

    private fun showError(message: String) {
        showProgress(false)
        binding.loginError.text = message
        binding.loginError.isVisible = true
    }
}
