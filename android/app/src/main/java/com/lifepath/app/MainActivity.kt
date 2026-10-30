package com.lifepath.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import com.lifepath.app.auth.ApiClient
import com.lifepath.app.auth.AuthStore
import com.lifepath.app.notifications.NotificationHelper
import com.lifepath.app.notifications.NotificationScheduler
import com.lifepath.app.widgets.WidgetRefreshWorker

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private var initialLoadUrl: String = ""

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
        private const val FALLBACK_SYSTEM_BAR_COLOR = "#F8F6F2"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serverUrl = AuthStore.getWebUrl(this)
        if (serverUrl.isNullOrBlank() || !AuthStore.isAuthenticated(this)) {
            openAuthScreen()
            return
        }

        applySystemBarsColor(Color.parseColor(FALLBACK_SYSTEM_BAR_COLOR))

        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progressBar)
        webView = findViewById(R.id.webView)

        setupWebView()

        NotificationHelper.createChannels(this)
        requestNotificationPermission()
        NotificationScheduler.scheduleNotifications(this)

        val deepLink = intent?.data?.path
        initialLoadUrl = if (!deepLink.isNullOrBlank()) {
            "$serverUrl$deepLink"
        } else {
            serverUrl
        }
        webView.loadUrl(initialLoadUrl)

        try {
            WidgetRefreshWorker.enqueue(this)
            WidgetRefreshWorker.refreshNow(this)
        } catch (_: Exception) {
            // WorkManager may fail on some devices; non-fatal
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = false
            }
        }

        webView.setBackgroundColor(Color.parseColor("#F8F6F2"))

        webView.addJavascriptInterface(LifePathJsBridge(this), "LifePathAndroid")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                val serverUrl = AuthStore.getWebUrl(this@MainActivity).orEmpty()
                return if (url.startsWith(serverUrl) || url.startsWith("lifepath://")) {
                    false
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                installWebBridgeHooks()
                reconcileTokensWithWeb()
                requestThemeColorFromWeb()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun syncTokensToWeb() {
        val accessToken = AuthStore.getAccessToken(this).orEmpty()
        val refreshToken = AuthStore.getRefreshToken(this).orEmpty()
        if (accessToken.isBlank()) return
        val js = """
            (function() {
                localStorage.setItem('access_token', '${jsEscape(accessToken)}');
                if ('${jsEscape(refreshToken)}'.length > 0) {
                    localStorage.setItem('refresh_token', '${jsEscape(refreshToken)}');
                }
                return 'ok';
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun reconcileTokensWithWeb() {
        webView.evaluateJavascript(
            "(function() { var logout=sessionStorage.getItem('lifepath:logout'); return JSON.stringify({access: localStorage.getItem('access_token') || '', refresh: localStorage.getItem('refresh_token') || '', logout: logout||''}); })()"
        ) { value ->
            val jsonString = value?.trim('"')?.replace("\\\\", "\\")?.replace("\\\"", "\"") ?: ""
            if (jsonString.isBlank()) return@evaluateJavascript
            try {
                val json = org.json.JSONObject(jsonString)
                val webAccess = json.optString("access", "")
                val webRefresh = json.optString("refresh", "")
                val logoutFlag = json.optString("logout", "").isNotBlank()
                val nativeAccess = AuthStore.getAccessToken(this).orEmpty()

                when {
                    webAccess.isBlank() && logoutFlag -> {
                        // Web cleared tokens (logout in progress). Do NOT push native — that would undo logout.
                        // Clear the flag so future reconciles behave normally.
                        webView.evaluateJavascript("(function(){ try{ sessionStorage.removeItem('lifepath:logout'); }catch(e){} })();", null)
                    }
                    webAccess.isBlank() -> {
                        // WebView has no tokens (fresh load, storage cleared) — push native if any
                        syncTokensToWeb()
                    }
                    nativeAccess.isBlank() -> {
                        // Native has no tokens (e.g. after logout). Do NOT restore from web — that
                        // would undo a deliberate logout due to an async callback race. Push blank
                        // to web to keep them in sync.
                        syncTokensToWeb()
                    }
                    nativeAccess != webAccess -> {
                        // Native has a different token, meaning a background refresh (e.g. widget
                        // worker) updated SharedPreferences while the WebView still holds the old
                        // token. Trust native and push it to the WebView so the already-rotated
                        // refresh token in localStorage is never used again.
                        syncTokensToWeb()
                    }
                    else -> {
                        // Both have tokens and they match — persist web's to native (handles first
                        // load when web got tokens from bridge before native was read).
                        AuthStore.saveTokens(this, webAccess, webRefresh.ifBlank { null })
                        NotificationScheduler.scheduleNotifications(this)
                        WidgetRefreshWorker.refreshNow(this)
                    }
                }
            } catch (_: Exception) {
                syncTokensToWeb()
            }
        }
    }

    private fun jsEscape(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val deepLink = intent.data?.path
        if (::webView.isInitialized && !deepLink.isNullOrBlank()) {
            val serverUrl = AuthStore.getWebUrl(this).orEmpty()
            webView.loadUrl("$serverUrl$deepLink")
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
            installWebBridgeHooks()
            reconcileTokensWithWeb()
            requestThemeColorFromWeb()
            NotificationScheduler.showObjectiveNow(this)
            WidgetRefreshWorker.refreshNow(this)
        }
    }

    override fun onPause() {
        if (::webView.isInitialized) {
            webView.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }
        super.onDestroy()
    }

    internal fun onWebAuthTokensChanged(access: String?, refresh: String?) {
        val cleanAccess = access?.trim().orEmpty()
        if (cleanAccess.isBlank()) return
        AuthStore.saveTokens(this, cleanAccess, refresh?.trim().takeUnless { it.isNullOrBlank() })
        NotificationScheduler.scheduleNotifications(this)
        WidgetRefreshWorker.refreshNow(this)
    }

    internal fun onWebLogout(manual: Boolean = false) {
        AuthStore.clearTokens(this)
        if (manual) {
            runOnUiThread { openAuthScreen() }
            return
        }
        val creds = AuthStore.getSavedCredentials(this)
        if (creds != null && AuthStore.isConfigured(this)) {
            kotlin.concurrent.thread {
                try {
                    val login = ApiClient.login(this@MainActivity, creds.email, creds.password)
                    AuthStore.saveTokens(this@MainActivity, login.accessToken, login.refreshToken)
                    NotificationScheduler.scheduleNotifications(this@MainActivity)
                    WidgetRefreshWorker.refreshNow(this@MainActivity)
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            webView.reload()
                        }
                    }
                } catch (_: Exception) {
                    runOnUiThread { openAuthScreen() }
                }
            }
        } else {
            runOnUiThread { openAuthScreen() }
        }
    }

    internal fun onWebThemeColorChanged(color: String?) {
        val parsed = parseCssColor(color) ?: return
        runOnUiThread { applySystemBarsColor(parsed) }
    }

    private fun openAuthScreen() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }

    private fun installWebBridgeHooks() {
        val js = """
            (function() {
              if (window.__lifepathAndroidBridgeInstalled) return 'installed';
              window.__lifepathAndroidBridgeInstalled = true;
              function notifyTokens() {
                try {
                  if (!window.LifePathAndroid || !window.LifePathAndroid.onAuthTokensChanged) return;
                  var access = localStorage.getItem('access_token') || '';
                  var refresh = localStorage.getItem('refresh_token') || '';
                  window.LifePathAndroid.onAuthTokensChanged(access, refresh);
                } catch (e) {}
              }
              function notifyTheme() {
                try {
                  if (!window.LifePathAndroid || !window.LifePathAndroid.onThemeColorChanged) return;
                  var metaTheme = document.querySelector('meta[name="theme-color"]');
                  var color = (metaTheme && metaTheme.getAttribute('content')) || '';
                  if (!color) {
                    var root = document.querySelector('main') || document.body || document.documentElement;
                    var rootStyle = getComputedStyle(root);
                    color = rootStyle.backgroundColor || '';
                    if (!color || color === 'transparent' || color === 'rgba(0, 0, 0, 0)') {
                      color = rootStyle.getPropertyValue('--color-background').trim();
                    }
                    if (!color || color === 'transparent' || color === 'rgba(0, 0, 0, 0)') {
                      color = getComputedStyle(document.body).backgroundColor || '';
                    }
                  }
                  window.LifePathAndroid.onThemeColorChanged(color);
                } catch (e) {}
              }
              function wrapHistory(fnName) {
                try {
                  var original = history[fnName];
                  history[fnName] = function() {
                    var out = original.apply(this, arguments);
                    notifyTheme();
                    return out;
                  };
                } catch (e) {}
              }
              var originalSet = localStorage.setItem.bind(localStorage);
              localStorage.setItem = function(key, value) {
                originalSet(key, value);
                if (key === 'access_token' || key === 'refresh_token') notifyTokens();
              };
              var originalRemove = localStorage.removeItem.bind(localStorage);
              localStorage.removeItem = function(key) {
                originalRemove(key);
                if (key === 'access_token' || key === 'refresh_token') notifyTokens();
              };
              window.addEventListener('auth:logout', function(ev) {
                try {
                  if (window.LifePathAndroid && window.LifePathAndroid.onWebLogout) {
                    var manual = ev && ev.detail && ev.detail.manual === true;
                    window.LifePathAndroid.onWebLogout(manual);
                  }
                } catch (e) {}
              });
              window.addEventListener('popstate', notifyTheme);
              window.addEventListener('hashchange', notifyTheme);
              window.addEventListener('load', notifyTheme);
              wrapHistory('pushState');
              wrapHistory('replaceState');
              try {
                var observer = new MutationObserver(function() { notifyTheme(); });
                observer.observe(document.documentElement, {
                  attributes: true,
                  attributeFilter: ['class', 'style'],
                  childList: true,
                  subtree: true
                });
              } catch (e) {}
              setInterval(notifyTheme, 1500);
              notifyTokens();
              notifyTheme();
              return 'ok';
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun requestThemeColorFromWeb() {
        webView.evaluateJavascript(
            "(function(){ var meta=document.querySelector('meta[name=\"theme-color\"]'); if(meta&&meta.getAttribute('content')) return meta.getAttribute('content'); var root=document.querySelector('main')||document.body||document.documentElement; var style=getComputedStyle(root); var c=(style.backgroundColor||''); if(!c||c==='transparent'||c==='rgba(0, 0, 0, 0)'){ c=(style.getPropertyValue('--color-background')||'').trim(); } if(!c||c==='transparent'||c==='rgba(0, 0, 0, 0)'){ c=getComputedStyle(document.body).backgroundColor||''; } return c; })();"
        ) { color ->
            val clean = color?.trim('"')
            val parsed = parseCssColor(clean) ?: Color.parseColor(FALLBACK_SYSTEM_BAR_COLOR)
            applySystemBarsColor(parsed)
        }
    }

    private fun parseCssColor(cssColor: String?): Int? {
        val value = cssColor?.trim().orEmpty()
        if (value.isBlank()) return null
        return try {
            when {
                value == "transparent" -> null
                value == "rgba(0, 0, 0, 0)" -> null
                value.startsWith("#") -> Color.parseColor(value)
                value.startsWith("rgb") -> {
                    val numbers = value.substringAfter("(").substringBefore(")").split(",")
                    if (numbers.size < 3) return null
                    val r = numbers[0].trim().toInt().coerceIn(0, 255)
                    val g = numbers[1].trim().toInt().coerceIn(0, 255)
                    val b = numbers[2].trim().toInt().coerceIn(0, 255)
                    Color.rgb(r, g, b)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun applySystemBarsColor(color: Int) {
        window.statusBarColor = color
        window.navigationBarColor = color
        val lightBg = ColorUtils.calculateLuminance(color) > 0.5
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = lightBg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = lightBg
        }
    }
}

class LifePathJsBridge(private val activity: MainActivity) {
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun getAppVersion(): String {
        return try {
            val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
            info.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    @JavascriptInterface
    fun isAndroidApp(): Boolean = true

    @JavascriptInterface
    fun getStoredTokens(): String {
        val access = AuthStore.getAccessToken(activity) ?: ""
        val refresh = AuthStore.getRefreshToken(activity) ?: ""
        return if (access.isBlank()) "{}"
        else org.json.JSONObject().apply {
            put("access_token", access)
            put("refresh_token", refresh)
        }.toString()
    }

    @JavascriptInterface
    fun onDataChanged() {
        WidgetRefreshWorker.refreshNow(activity)
    }

    @JavascriptInterface
    fun onAuthTokensChanged(access: String?, refresh: String?) {
        activity.onWebAuthTokensChanged(access, refresh)
    }

    @JavascriptInterface
    fun onWebLogout(manual: Boolean?) {
        activity.onWebLogout(manual == true)
    }

    @JavascriptInterface
    fun onThemeColorChanged(color: String?) {
        activity.onWebThemeColorChanged(color)
    }
}
