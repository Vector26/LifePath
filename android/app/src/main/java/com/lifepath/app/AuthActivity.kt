package com.lifepath.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lifepath.app.auth.ApiClient
import com.lifepath.app.auth.AuthStore
import com.lifepath.app.notifications.NotificationScheduler
import com.lifepath.app.widgets.WidgetRefreshWorker
import kotlin.concurrent.thread

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        if (AuthStore.isConfigured(this) && AuthStore.isAuthenticated(this)) {
            startMain()
            return
        }

        val changeServerLink = findViewById<View>(R.id.changeServerLink)
        val urlFieldsContainer = findViewById<LinearLayout>(R.id.urlFieldsContainer)
        val webUrlInput = findViewById<EditText>(R.id.webUrlInput)
        val apiUrlInput = findViewById<EditText>(R.id.apiUrlInput)
        val saveCredentialsCheck = findViewById<CheckBox>(R.id.saveCredentialsCheck)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val loginProgress = findViewById<ProgressBar>(R.id.loginProgress)

        val webUrl = AuthStore.getWebUrl(this)
        val apiUrl = AuthStore.getApiUrl(this)
        var urlsConfigured = !webUrl.isNullOrBlank()
        if (urlsConfigured) {
            webUrlInput.setText(webUrl)
            if (!apiUrl.isNullOrBlank() && apiUrl != webUrl) apiUrlInput.setText(apiUrl)
            urlFieldsContainer.visibility = View.GONE
            changeServerLink.visibility = View.VISIBLE
        } else {
            urlFieldsContainer.visibility = View.VISIBLE
            changeServerLink.visibility = View.GONE
        }

        changeServerLink.setOnClickListener {
            urlFieldsContainer.visibility = View.VISIBLE
            changeServerLink.visibility = View.GONE
            urlsConfigured = false
        }

        AuthStore.getSavedCredentials(this)?.let { creds ->
            emailInput.setText(creds.email)
        }

        loginButton.setOnClickListener {
            val webUrlVal = webUrlInput.text.toString().trim().trimEnd('/')
            val apiUrlOverride = apiUrlInput.text.toString().trim().trimEnd('/')
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            val webUrlToUse = if (urlsConfigured) webUrl!! else webUrlVal
            val apiUrlToUse = if (urlsConfigured) apiUrl else apiUrlOverride.ifBlank { null }

            if (!urlsConfigured) {
                if (!webUrlVal.startsWith("http://") && !webUrlVal.startsWith("https://")) {
                    Toast.makeText(this, "Enter a valid Web URL", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (apiUrlOverride.isNotBlank() && !apiUrlOverride.startsWith("http")) {
                    Toast.makeText(this, "Enter a valid API URL", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            setLoading(loginButton, loginProgress, true)
            thread {
                try {
                    AuthStore.saveUrls(this, webUrlToUse, apiUrlToUse)
                    val login = ApiClient.login(this, email, password)
                    AuthStore.saveTokens(this, login.accessToken, login.refreshToken)
                    if (saveCredentialsCheck.isChecked) {
                        AuthStore.saveCredentials(this, email, password)
                    } else {
                        AuthStore.clearSavedCredentials(this)
                    }
                    NotificationScheduler.scheduleNotifications(this)
                    WidgetRefreshWorker.enqueue(this)
                    WidgetRefreshWorker.refreshNow(this)
                    runOnUiThread { startMain() }
                } catch (e: Exception) {
                    runOnUiThread {
                        setLoading(loginButton, loginProgress, false)
                        Toast.makeText(
                            this,
                            e.message ?: "Login failed. Check URL and credentials.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun setLoading(button: Button, progressBar: ProgressBar, isLoading: Boolean) {
        button.isEnabled = !isLoading
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
