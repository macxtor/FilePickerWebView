package com.example.filepickerwebview

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebView(
    webViewClient: WebViewClient,
    webChromeClient: WebChromeClient,
    url: String
) {
    handleCookies()
    AndroidView(factory = {
        android.webkit.WebView(it).apply {
            this.webViewClient = webViewClient
            this.webChromeClient = webChromeClient
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
            }
            loadUrl(url)
        }
    }, modifier = Modifier.fillMaxWidth())
}

private fun handleCookies() {
    CookieManager.getInstance().apply {
        removeAllCookies(null)
        flush()
        setAcceptCookie(true)
        acceptCookie()
    }
}