package com.visang.mathalive.eventbus

import android.graphics.Rect
import android.media.projection.MediaProjection
import android.net.wifi.ScanResult
import com.visang.mathalive.util.ImageUtils
import com.visang.mathalive.util.PathUtils


// Common
fun makeResult(result: Boolean) = if (result) "success" else "fail"
fun makeWebUrl(url: String?) = if (url != null) PathUtils.fileToWebUrl(url) else null
fun makeFilePath(url: String) = PathUtils.webToFilePath(url)

// Activity
class Quit
class QuitForUpdate
class KeepForeground(val keepForeground: Boolean)

class NotifyJSResult(val data: Any)

// 전체 화면 동영상
class StartScreenToVideoRecord(val data: String)
class StopScreenToVideoRecord
class NotifyRecordScreenResult(result: Boolean, cbFunc: String? = null, url: String? = null) {
    val result = makeResult(result)
    val cbFunc = cbFunc
    val url = makeWebUrl(url)
}

// 화면 Screen Shot
class CaptureScreen(val data: String)
class StopCaptureScreen()
class NotifyCaptureScreenResult(result: Boolean, cbFunc: String? = null, type: String? = "jpg", data: String? = null) {
    val result = makeResult(result)
    val cbFunc = cbFunc
    val base64Img = data
    val imgType = type
}

// 디바이스
class DeviceDisplayChanged(val displayId: Int)

// 키보드
class EnableSoftwareKeyboard(val enable: Boolean = true)
class NotifyShowSoftwareKeyboardResult(val result: Boolean)
class NotifyHideSoftwareKeyboardResult(val result: Boolean)

// Wifi
class GetActiveWifiList
class NotifyGetActiveWifiListResult(result: Boolean, val list: List<ScanResult>? = null) {
    val result = makeResult(result)
}
class TryConnectWifi(val ssid: String, val password: String)
class NotifyTryConnectWifiResult(result: Boolean) {
    val result = makeResult(result)
}

// WebView
class ClearWebViewCache

//LaunchApp
class LaunchApp(val data: String)

//CloseApp
class CloseApp

//DestroyApp
class NotifyDestroy

//Notify Error
class NotifyError(val code: Int)

class NotifyDownloadProgress(val result: Boolean, val cbFunc: String? = null, val target:String, val progress: Int, val filesize: Int, val downloadedfilesize: Int)

class UpdateApp(val url: String)

class UpdateAppAll(val url_me: String, val url_ccss: String, val url_ggp: String)

class TestProgress(val cbfunc: String?)

class HideSplash()
class HideReload()

class ReloadNetwork()

class ShowProgress()
class HideProgress()