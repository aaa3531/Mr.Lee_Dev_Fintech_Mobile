package com.example.moneymachinemobile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.net.URISyntaxException

class WebViewActivity : AppCompatActivity(){

    var URL = ""
    private var m_context: Context? = null
    private lateinit var m_webView : WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        val ab = supportActionBar!!
        ab.title = "주식인다이아"
        ab.setDisplayShowTitleEnabled(true)

        m_context = applicationContext
        m_webView = findViewById(R.id.webView)
        m_webView.setBackgroundColor(Color.TRANSPARENT)
        m_webView.rootView.setBackgroundColor(Color.WHITE)
        WebView.setWebContentsDebuggingEnabled(true)
        m_webView.webChromeClient = WebChromeClient()


        if(intent.hasExtra("url"))
        {
            URL = intent.getStringExtra("url")
        }

        m_webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {

            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url != null && url.startsWith("intent://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        val existPackage =
                                packageManager.getLaunchIntentForPackage(intent.getPackage()!!)
                        if (existPackage != null) {
                            startActivity(intent)
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW)
                            marketIntent.data =
                                    Uri.parse("market://details?id=" + intent.getPackage()!!)
                            startActivity(marketIntent)
                        }
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } else if (url != null && url.startsWith("market://")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (intent != null) {
                            startActivity(intent)
                        }
                        return true
                    } catch (e: URISyntaxException) {
                        e.printStackTrace()
                    }

                }
                view.loadUrl(url)
                return false
            }
        }

        startWebView()
    }

    override fun onBackPressed() {
        if (m_webView.canGoBack()) {
            m_webView.goBack()
        } else {
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    fun startWebView() {
        m_webView.clearCache(true)

        val settings = m_webView.settings
        settings.javaScriptEnabled = true
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.domStorageEnabled = true
        settings.setAppCacheEnabled(true)
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true


        //웹뷰가 html의 viewport 메타 태그를 지원하게 한다.
        settings.useWideViewPort = true

        //웹뷰가 html 컨텐츠가 웹뷰보다 클 경우 스크린 크기에 맞게 조정되도록 한다.
        settings.loadWithOverviewMode = true

        m_webView.loadUrl(URL)
    }
}