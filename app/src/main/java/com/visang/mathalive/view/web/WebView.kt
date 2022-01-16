package com.visang.mathalive.view.web

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * WebView
 *
 * 이 앱에서 사용할 WebView들의 공통 사항들 적용
 */
@SuppressLint("SetJavaScriptEnabled")
open class WebView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = android.R.attr.webViewStyle): WebView(context, attrs, defStyleAttr) {

    init {
        settings.apply {
            javaScriptEnabled = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            setAppCacheEnabled(false)
            textZoom = 100
        }

        setBackgroundColor(0)
        webChromeClient = WebChromeClient()

        addJavascriptInterface(JSInterface(), "MathALiveAndroidBridge")

        setWebContentsDebuggingEnabled(true)
    }
}
