package com.visang.mathalive.view.web

import android.content.Context
import android.net.http.SslError
import android.util.AttributeSet
import android.util.EventLog
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.visang.fel.util.plusAssign
import com.visang.mathalive.eventbus.*
import com.visang.mathalive.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONObject


/**
 * Launcher WebView
 */
class LauncherWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
): WebView(context, attrs, defStyleAttr) {

    private val gson = Gson()
    private val compositeDisposable = CompositeDisposable()
    init {
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        compositeDisposable += EventBus.observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is NotifyJSResult -> {
                            var functionName = it.data.javaClass.simpleName
                            val jsFunctionName =
                                "${Character.toLowerCase(functionName.toCharArray()[0])}${
                                    functionName.substring(
                                        1
                                    )
                                }"
                            Log.d(
                                "LauncherWeb", "evaluateJavascript ${jsFunctionName}(${
                                    gson.toJson(
                                        it.data
                                    )
                                })"
                            )
                            evaluateJavascript("$jsFunctionName(${gson.toJson(it.data)})", null)
                        }
                        is NotifyCaptureScreenResult -> {
                            it.cbFunc?.let { cbFunc ->
                                var functionName = cbFunc
                                val jsFunctionName =
                                    "${Character.toLowerCase(functionName.toCharArray()[0])}${
                                        functionName.substring(
                                            1
                                        )
                                    }"
                                var obj = JSONObject()
                                obj.put("imgtype", it.imgType);
                                obj.put("data", it.base64Img);
                                Log.d("NotifyCaptureScreenResult", it.base64Img!!)
                                Log.d(
                                    "NotifyCaptureScreenResult",
                                    "evaluateJavascript ${jsFunctionName}(${
                                        gson.toJson(
                                            obj.toString()
                                        )
                                    })"
                                )
                                evaluateJavascript(
                                    "$jsFunctionName(${gson.toJson(obj.toString())})",
                                    null
                                )
                            }
                        }
                        is NotifyRecordScreenResult -> {
                            it.cbFunc?.let { cbFunc ->
                                var functionName = cbFunc
                                val jsFunctionName =
                                    "${Character.toLowerCase(functionName.toCharArray()[0])}${
                                        functionName.substring(
                                            1
                                        )
                                    }"
                                var obj = JSONObject()
                                obj.put("url", it.url);
                                Log.d(
                                    "NotifyRecordScreenResult",
                                    "evaluateJavascript ${jsFunctionName}(${
                                        gson.toJson(
                                            obj.toString()
                                        )
                                    })"
                                )
                                evaluateJavascript(
                                    "$jsFunctionName(${gson.toJson(obj.toString())})",
                                    null
                                )
                            }
                        }
                        is NotifyError -> {
                            var functionName = "window.top.error"
                            val jsFunctionName =
                                "${Character.toLowerCase(functionName.toCharArray()[0])}${functionName.substring(1)}"
                            Log.d("NotifyError", "evaluateJavascript ${jsFunctionName}(${it.code})")
                            evaluateJavascript(
                                "$jsFunctionName(${it.code})",
                                null
                            )
                        }
                        is NotifyDestroy -> {
                            var functionName = "window.top.destroy"
                            val jsFunctionName =
                                "${Character.toLowerCase(functionName.toCharArray()[0])}${functionName.substring(1)}"
                            Log.d("NotifyError", "evaluateJavascript ${jsFunctionName}()")
                            evaluateJavascript(
                                "$jsFunctionName()",
                                null
                            )
                        }
                        is Quit -> {
                            compositeDisposable.clear()
                        }
                        is ClearWebViewCache -> {
                            clearCache(true)
                        }
                        is NotifyDownloadProgress -> {
                            it.cbFunc?.let { cbFunc ->
                                var functionName = "window.top.content." + cbFunc
                                val jsFunctionName =
                                        "${Character.toLowerCase(functionName.toCharArray()[0])}${
                                            functionName.substring(
                                                    1
                                            )
                                        }"
                                var obj = JSONObject()
                                obj.put("target", it.target);
                                obj.put("progress", it.progress);
                                obj.put("filesize", it.filesize);
                                obj.put("downloadedfilesize", it.downloadedfilesize);
                                Log.d(
                                        "NotifyDownloadProgress",
                                        "evaluateJavascript $jsFunctionName(${gson.toJson(obj.toString())})"
                                )
                                evaluateJavascript("$jsFunctionName(${gson.toJson(obj.toString())})", null)
                            }
                        }
                    }
                }

        setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
//                pageFinishedCompletable.onComplete()
//                EventBus.send(HideSplash())
            }

            override fun onReceivedSslError(view: android.webkit.WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed() // Ignore SSL certificate errors
            }
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }
}
