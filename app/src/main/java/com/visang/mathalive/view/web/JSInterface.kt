package com.visang.mathalive.view.web

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Rect
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.visang.mathalive.MainApplication
import com.visang.mathalive.activity.MainActivity
import com.visang.mathalive.eventbus.*
import com.visang.mathalive.util.DefaultDisplay
import com.visang.mathalive.util.Log
import com.visang.mathalive.util.Logger
import com.visang.mathalive.util.Utils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.wifiManager
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule
import kotlin.math.roundToLong


/**
 * Javascript Interface
 *
 * WebView의 JavaScript에서 Native 기능을 사용할 수 있도록 Interface 제공
 */
class JSInterface {
    var progress = 0
    @JavascriptInterface
    fun quit() {
        EventBus.send(Quit())    
    }

    @JavascriptInterface
    fun getAPName(): String {
        return MainApplication.appContext.wifiManager.connectionInfo.ssid
    }

    @JavascriptInterface
    fun getVersion(): String {

        var me = "";
        var ccss = "";
        var ggp = "";

        try {
            val info = MainApplication.appContext.packageManager.getPackageInfo(
                MainApplication.appContext.getPackageName(),
                0
            )
//            val isRunnning = Utils.isAppRunning(MainApplication.appContext.getPackageName())
            me = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
        }
        try {
            val info = MainApplication.appContext.packageManager.getPackageInfo(
                "com.Visang.VisangMath",
                0
            )
            val isRunnning = Utils.isAppRunning("com.Visang.VisangMath")
            if(isRunnning) {
                ggp = "100000.0.0"
            } else {
                ggp = info.versionName
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ggp = "0.0.0"
        }
        try {
            val info = MainApplication.appContext.packageManager.getPackageInfo(
                "com.Visang.VisangMath2",
                0
            )
            val isRunnning = Utils.isAppRunning("com.Visang.VisangMath2")
            if(isRunnning) {
                ccss = "100000.0.0"
            } else {
                ccss = info.versionName
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ccss = "0.0.0"
        }

        var obj = JSONObject()
        obj.put("me", me)
        obj.put("ggp", ggp)
        obj.put("ccss", ccss)

        return obj.toString()
    }

    @JavascriptInterface
    fun updateApp(url: String) {
        EventBus.send(UpdateApp(url))
    }

    @JavascriptInterface
    fun updateAppAll(url_me: String, url_ccss: String, url_ggp: String) {
        EventBus.send(UpdateAppAll(url_me, url_ccss, url_ggp))
    }

    @JavascriptInterface
    fun testProgress(cbfunc: String?): String {
//        EventBus.send(TestProgress(cbfunc))

        if(MainActivity.downloadProgress >= 100) MainActivity.downloadProgress = 0
        MainActivity.downloadProgress += 1
        MainActivity.downloadTarget = "target"
        MainActivity.downloadTotalSize = 100000
        MainActivity.downloadedSize += 1000

        var obj = JSONObject()
        obj.put("target", MainActivity.downloadTarget);
        obj.put("progress", MainActivity.downloadProgress);
        obj.put("filesize", MainActivity.downloadTotalSize);
        obj.put("downloadedfilesize", MainActivity.downloadedSize);
        var result = obj.toString()
        return result
    }

    /**
     * 디바이스의 "natural" orientation 으로부터 몇 도 회전되어 있는지 조회.
     */
    @JavascriptInterface
    fun getDeviceOrientation(): Int {
        return DefaultDisplay.rotationDegree
    }

    @JavascriptInterface
    fun getActiveWifiList() {
        EventBus.send(GetActiveWifiList())
    }

    @JavascriptInterface
    fun showSoftwareKeyboard() {
        EventBus.send(EnableSoftwareKeyboard(true))
    }

    @JavascriptInterface
    fun hideSoftwareKeyboard() {
        EventBus.send(EnableSoftwareKeyboard(false))
    }

    /**
     * Password가 필요없는 AP의 경우 password parameter 값 무시됨.
     */
    @JavascriptInterface
    fun tryConnectWifi(ssid: String, password: String) {
         EventBus.send(TryConnectWifi(ssid, password))
    }

    @JavascriptInterface
    fun keepForeground(keepForeground: Boolean) {
        EventBus.send(KeepForeground(keepForeground))
    }

    @JavascriptInterface
    fun launchApp(data: String) {
        EventBus.send(LaunchApp(data))
    }

    @JavascriptInterface
    fun closeApp() {
        EventBus.send(CloseApp())
    }

    @JavascriptInterface
    fun startScreenRecord(data: String) {
        Log.d("startRecord", data)
        EventBus.send(StartScreenToVideoRecord(data))
    }
    @JavascriptInterface
    fun stopScreenRecord() {
        EventBus.send(StopScreenToVideoRecord())
    }

    @JavascriptInterface
    fun startCapture(data: String) {
        Log.d("startCapture", data)
        EventBus.send(CaptureScreen(data))
    }

    @JavascriptInterface
    fun stopCapture() {
        EventBus.send(StopCaptureScreen())
    }

    @JavascriptInterface
    fun hideSplash() {
        EventBus.send(HideSplash())
    }

    @JavascriptInterface
    fun reloadNetwork() {
        EventBus.send(ReloadNetwork())
    }
}
