package com.visang.mathalive.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.AppOpsManager
import androidx.lifecycle.LifecycleRegistry
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.*
import android.os.*
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.text.SpannableStringBuilder
import android.util.Base64
import android.view.Display
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import com.visang.mathalive.BuildConfig
import com.visang.mathalive.MainApplication
import com.visang.mathalive.databinding.ActivityMainBinding
import com.visang.mathalive.eventbus.*
import com.visang.mathalive.handler.WifiHandler
import com.visang.mathalive.media.ScreenToVideoRecorder
import com.visang.mathalive.service.MyService
import com.visang.mathalive.util.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.jetbrains.anko.inputMethodManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule
import kotlin.coroutines.CoroutineContext

/**
 * Main activity
 *
 * Main activity class.
 */

class MainActivity : BaseActivity(activityId), CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private var contentUpdateJob: Job? = null

    private var hideKeyboardDisposable: Disposable? = null
    private val compositeDisposable = CompositeDisposable()
    private var foregroundServiceIntent : Intent? = null

    private var backPressedToExitOnce = false

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        //  사용자가 현재 진행 중이던 상태를 Bundle에 저장한다.
//        savedInstanceState.putInt(STATE_SCORE, mCurrentScore);
//        savedInstanceState.putInt(STATE_LEVEL, mCurrentLevel);

        // *항상 super를 호출해줘야 안드로이드에서 View의 상태들도 저장할 것이다.
        super.onSaveInstanceState(savedInstanceState);
    }

    private var screenToVideoRecorder: ScreenToVideoRecorder? = null
    private var mProjectionManager: MediaProjectionManager? = null
    private var mImageReader: ImageReader? = null
    private var mHandler: Handler? = null
    private var mDisplay: Display? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mDensity = 0
    private var mWidth = 0
    private var mHeight = 0
    private var mRotation = 0
    private val REQUEST_CODE_CAPTURE = 100
    private val REQUEST_CODE_RECORD = 101
    private val MODIFY_AUDIO_SETTINGS = 102
    private var STORE_DIRECTORY: String? = null
    private var IMAGES_PRODUCED = 0
    private val SCREENCAP_NAME = "screencap"
    private val VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    private var sMediaProjectionCapture: MediaProjection? = null
    private var SCREENCAP_LAST: Long = 0
    private lateinit var SCREENCAP_DATA: JsonObject
    private var hasMediaProtectionPermission = false
    private var isCapturing = false
    private var isRecording = false

    private val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1001
    private val USAGE_STATS_PERMISSION_REQUEST_CODE = 1002

    private var bQuitService = true

    /**
     * Called when the activity is starting.
     */
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LifeCycle", "onCreate")

        if (savedInstanceState != null) {
        } else {
            Logger.initialize()
        }

        //App update check
//        AppUpdateCheck()

        val token = PrefManager.getPushToken(applicationContext)
        if(token.isNullOrEmpty()) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FirebaseMessaging", "Fetching FCM registration token failed" + task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                // Log and toast
                token?.let { PrefManager.savePushToken(applicationContext, it) }
//                Log.e("Token", token!!)
//                Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()
            })
        }

        Firebase.messaging.subscribeToTopic(BuildConfig.MESSAGECODE)
                .addOnCompleteListener { task ->
                    var msg = "subscribed"
                    if (!task.isSuccessful) {
                        msg = "subscribe_failed"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    }
//                    Log.d("subscribeToTopic", msg)
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        setContentView(R.layout.activity_main)

        MainActivity.context = this
        if(!isTaskRoot) {
            val intent = getIntent()
            if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
                finish()
                return
            }
        }

        MainActivity.lifecycleRegistry = lifecycleRegistry
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        AndroidBug5497Workaround.assistActivity(this)

        binding.viewWeb.loadUrl(BuildConfig.TARGETSITE)
        binding.reloadWeb.loadUrl(Constants.RELOAD_WEB_URL)

        bConnected = isNetworkAvailable(context)

        if(bConnected) {
            reloadWeb.visibility = View.INVISIBLE
        } else {
            reloadWeb.visibility = View.VISIBLE
        }

        dimView.visibility = View.INVISIBLE
        progressbarView.visibility = View.INVISIBLE

        initializeImmersiveStickyMode()
        initializeHandlers()

        foregroundServiceIntent = Intent(this, MyService::class.java)

        mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // start capture handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()

        checkPermission()

        Timer("HideSplash", false).schedule(10 * 1000) {
            EventBus.send(HideSplash())
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) + ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )) {
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                    ), 9999
                )
            }
        } else {
            // write your logic code if permission already granted
        }
        registerNetworkCallback()
    }

    override fun onStart() {
        super.onStart()
        Log.d("LifeCycle", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("LifeCycle", "onResume")
        setImmersiveStickyMode()
        unregisterNetworkCallback()
        registerNetworkCallback()
    }

    override fun onStop() {
        super.onStop()
        Log.d("LifeCycle", "onStop")
        unregisterNetworkCallback()
    }

    override fun onPause() {
        super.onPause()
        Log.d("LifeCycle", "onPause")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("LifeCycle", "onRestart")
    }

    /**
     * The final call you receive before your activity is destroyed. This can happen either because the activity is finishing (someone called finish() on it, or because the system is temporarily destroying this instance of the activity to save space. You can distinguish between these two scenarios with the isFinishing() method.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d("LifeCycle", "onDestroy")

        hideKeyboardDisposable?.let {
            it.dispose()
            hideKeyboardDisposable = null
        }

        EventBus.sendJSNotifyDestroy()

        compositeDisposable.clear()
        MainActivity.lifecycleRegistry = null

        if(isRecording) {
            stopScreenToVideoRecorder()
        }
        if(isCapturing) {
            stopProjection()
        }

        // Loading a blank page is optional, but will ensure that the WebView isn't doing anything when you destroy it.
        //viewWeb.loadUrl("about:blank");
        // Make sure you remove the WebView from its parent view before doing anything.
        viewRoot.removeAllViews();
        if(viewWeb != null) {
            viewWeb.clearHistory();
            // NOTE: clears RAM cache, if you pass true, it will also clear the disk cache.
            // Probably not a great idea to pass true if you have other WebViews still alive.
            viewWeb.clearCache(true);
            viewWeb.onPause();
            viewWeb.removeAllViews();
//            viewWeb.destroyDrawingCache();
            // NOTE: This pauses JavaScript execution for ALL WebViews,
            // do not use if you have other WebViews still alive.
            // If you create another WebView after calling this,
            // make sure to call mWebView.resumeTimers().
//            viewWeb.pauseTimers();
            // NOTE: This can occasionally cause a segfault below API 17 (4.2)
            viewWeb.destroy();
        }
        if(reloadWeb !== null) {
            reloadWeb.clearHistory();
            reloadWeb.clearCache(true);
            reloadWeb.destroy();
        }

        sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
            putExtra("CMD", "onDestroy")
            putExtra("Quit", bQuitService)
        })

//        if(foregroundServiceIntent != null) {
//            stopService(foregroundServiceIntent)
//            foregroundServiceIntent = null
//        }
    }

    fun startService() {
        if(isServiceRunning("com.visang.mathalive.service.MyService")) {
        } else {
            // 안드로이드 O 이상에서는 startForegroundService()로 호출해야합니다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(foregroundServiceIntent)
            } else {
                startService(foregroundServiceIntent)
            }
        }
    }

    fun isServiceRunning(serviceClassName: String?): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val services: List<ActivityManager.RunningServiceInfo> = activityManager.getRunningServices(
            Int.MAX_VALUE
        )
        for (runningServiceInfo in services) {
            if (runningServiceInfo.service.getClassName().equals(serviceClassName)) {
                return true
            }
        }
        return false
    }

    fun AppUpdateCheck(url: String) {
        try {
            MainActivity.checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnComplete {
//                    UpdateAppTask(MainApplication.appContext).execute()
                    sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                        putExtra("CMD", "updateApp")
                        putExtra("target", "me")
                        putExtra("url", url)
                        putExtra("version", "")
                    })
                }
                .subscribe({
                    //UpdateAppTask(MainApplication.appContext).execute("http://" + GRPC.serverAddress + ":8080/app-release.apk")
                }, {
                    //UpdateAppTask(MainApplication.appContext).execute("http://" + GRPC.serverAddress + ":8080/app-release.apk")
                })
        } catch (e: Exception) {
            Log.e("UpdateAPP", "Update error! " + e.message)
        }
    }
    fun AppUpdateCheckAll(url_me: String, url_ccss: String, url_ggp: String) {
        try {
            MainActivity.checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnComplete {
//                    UpdateAppTask(MainApplication.appContext).execute()
                    if(url_me != "") {
                        sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                            putExtra("CMD", "updateApp")
                            putExtra("target", "me")
                            putExtra("url", url_me)
                            putExtra("version", "")
                        })
                    } else if(url_ccss != "") {
                        sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                            putExtra("CMD", "updateApp")
                            putExtra("target", "ccss")
                            putExtra("url", url_ccss)
                            putExtra("version", "")
                        })
                    } else if(url_ggp != "") {
                        sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                            putExtra("CMD", "updateApp")
                            putExtra("target", "ggp")
                            putExtra("url", url_ggp)
                            putExtra("version", "")
                        })
                    }
                }
                .subscribe({
                    //UpdateAppTask(MainApplication.appContext).execute("http://" + GRPC.serverAddress + ":8080/app-release.apk")
                }, {
                    //UpdateAppTask(MainApplication.appContext).execute("http://" + GRPC.serverAddress + ":8080/app-release.apk")
                })
        } catch (e: Exception) {
            Log.e("UpdateAPP", "Update error! " + e.message)
        }
    }

//    inner class UpdateAppTask(context: Context) : AsyncTask<Void, Void, Void>() {
//        var context: Context? = context
//        var bUpdate = false
//        var version = "0.0.0"
//        var urlApk: String? = null
//
//        override fun onPreExecute() {
//        }
//
//        override fun doInBackground(vararg params: Void?): Void? {
//            try {
//                val url = URL(Constants.APP_UPDATE_CHECK_URL)
//                val c = url.openConnection() as HttpURLConnection
//                c.requestMethod = "GET"
//                c.connectTimeout = 3000
//                c.connect()
//
//                val ist = c.inputStream
//                val info = InputStreamReader(ist).readText()
//                ist.close()
//
//                bUpdate = false
//
//                val jsonData = Gson().fromJson(info, JsonObject::class.java)
//                val jsonStudent = if(!jsonData.has("student")) null else jsonData.get("student").asJsonObject
//                jsonStudent?.let {
//                    version = if(!it.has("version")) "0.0.0" else it.get("version").asString
//                    urlApk = if(!it.has("url")) null else it.get("url").asString
//
//                    if(version > getVersion() && urlApk != null) {
//                        bUpdate = true
//                    }
//                }
//
//            } catch (e: Exception) {
//                bUpdate = false
//                Log.e("UpdateAPP", "Update error! " + e.message)
//            }
//
//            return null
//        }
//
//        override fun onPostExecute(result: Void?) {
//            super.onPostExecute(result)
//
//            if(bUpdate) {
//                AlertDialog.Builder(MainActivity.context)
//                    .setTitle("업데이트 알림")
//                    .setMessage("새로운 업데이트가 있습니다. \n업데이트를 진행합니다.")
//                    .setPositiveButton("확인") { _, _ ->
//                        sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
//                            putExtra("CMD", "updateApp")
//                            putExtra("url", urlApk)
//                            putExtra("version", version)
//                        })
//                    }
////                .setNegativeButton("취소", { _, _ ->
////                })
//                    .show()
//
////                MainActivity.showToast("새로운 버전을 업데이트 합니다.", 3000)
////                Timer("UpdateApp", false).schedule(1 * 1000) {
////                    sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
////                        putExtra("CMD", "updateApp")
////                        putExtra("url", urlApk)
////                        putExtra("version", version)
////                    })
////                }
//
////                if(must == "1") {
////
////                } else {
////                    showUpdateDialog()
////                }
//
//            } else {
//            }
//        }
//
////        private fun showUpdateDialog() {
////            AlertDialog.Builder(MainActivity.context)
////                .setTitle("업데이트")
////                .setMessage("새로운 업데이트가 있습니다./n업데이트 하시겠습니까?")
////                .setPositiveButton("업데이트") { _, _ ->
////
////                    sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
////                        putExtra("CMD", "updateApp")
////                        putExtra("url", urlApk)
////                        putExtra("version", version)
////                    })
////                }
////                .setNegativeButton("취소", { _, _ ->
////                })
////                .show()
////        }
//
//    }

//    fun getVersion(): String {
//        val info = MainApplication.appContext.packageManager.getPackageInfo(
//            MainApplication.appContext.getPackageName(),
//            0
//        )
//        return info.versionName
//    }

    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {           // Andoid 10 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 다른앱 위에 그리기 체크
                val uri: Uri = Uri.fromParts("package", packageName, null)
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                startService()
            }
        } else {
            startService()
        }

        var granted = false
        val appOps: AppOpsManager = applicationContext.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode: Int = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), applicationContext.packageName)

        granted =
            if (mode == AppOpsManager.MODE_DEFAULT) {
                applicationContext.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
            } else {
               mode == AppOpsManager.MODE_ALLOWED
           }

        if(!granted) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    @SuppressLint("CheckResult")
    private fun initializeHandlers() {

        WifiHandler(lifecycleProvider)

        EventBus.observable
            .bindToLifecycle(lifecycleProvider)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                when (it) {
                    is EnableSoftwareKeyboard -> enableSoftwareKeyboard(it.enable)
                    is UpdateApp -> {
                        AppUpdateCheck(it.url)
                    }
                    is UpdateAppAll -> {
                        AppUpdateCheckAll(it.url_me, it.url_ccss, it.url_ggp)
                    }
                    is TestProgress -> {
                        EventBus.sendJSNotifyDownloadProgress(true, it.cbfunc, "target", 50, 10000000, 1000)
                    }
                    is HideReload -> {
                        reloadWeb.visibility = View.INVISIBLE
                    }
                    is HideSplash -> {
                        val animation1 = AlphaAnimation(1.0f, 0.0f)
                        animation1.setDuration(1000)
                        animation1.setStartOffset(1000)
                        animation1.setFillAfter(true)
                        viewSplash.startAnimation(animation1)
                    }
                    is LaunchApp -> {
                        it.data
                        sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                            putExtra("CMD", "LaunchApp")
                            putExtra("DATA", it.data)
                        })
                    }
                    is CloseApp -> {
                        sendBroadcast(Intent("INTENT_FILTER_ACCESS_SERVICE").apply {
                            putExtra("CMD", "CloseApp")
                        })
                    }
                    is CaptureScreen -> {
                        SCREENCAP_DATA = Gson().fromJson(it.data, JsonObject::class.java)
                        getPermission(REQUEST_CODE_CAPTURE)

//                        when {
//                            ContextCompat.checkSelfPermission(
//                                context,
//                                Manifest.permission.RECORD_AUDIO
//                            ) == PackageManager.PERMISSION_GRANTED -> {
//                                // You can use the API that requires the permission.
//                                getPermission(REQUEST_CODE_CAPTURE)
//                            }
//                            else -> {
//                                // You can directly ask for the permission.
//                                ActivityCompat.requestPermissions(
//                                    this,
//                                    arrayOf(Manifest.permission.RECORD_AUDIO),
//                                    REQUEST_CODE_CAPTURE
//                                )
//                            }
//                        }
                    }
                    is StopCaptureScreen -> {
                        stopProjection()
                    }
                    is StartScreenToVideoRecord -> {
                        SCREENCAP_DATA = Gson().fromJson(it.data, JsonObject::class.java)
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                // You can use the API that requires the permission.
//                                startScreenToVideoRecorder()
                                getPermission(REQUEST_CODE_RECORD)
                            }
                            else -> {
                                // You can directly ask for the permission.
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(Manifest.permission.RECORD_AUDIO),
                                    REQUEST_CODE_RECORD
                                )
                            }
                        }
                    }
                    is StopScreenToVideoRecord -> {
                        stopScreenToVideoRecorder()
                    }
                    is QuitForUpdate -> {
                        bQuitService = false
                        finish()
                    }
                    is ReloadNetwork -> {
                        bConnected = isNetworkAvailable(context)

                        if (bConnected) {
                            viewWeb.reload()
                            Timer("HideReload", false).schedule(3 * 1000) {
                                EventBus.send(HideReload())
                            }
                        } else {
                            reloadWeb.visibility = View.VISIBLE
                        }
                    }
                    is ShowProgress -> {
                        var target = MainActivity.downloadTarget
                        var progress = MainActivity.downloadProgress
                        var filesize = MainActivity.downloadTotalSize
                        var downloadedfilesize = MainActivity.downloadedSize

                        dimView.visibility = View.VISIBLE
                        progressbarView.visibility = View.VISIBLE
                        progressbar.progress = progress
                    }
                    is HideProgress -> {
                        dimView.visibility = View.INVISIBLE
                        progressbarView.visibility = View.INVISIBLE
                        progressbar.progress = 0
                    }
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_RECORD -> {
                if (grantResults.isEmpty()) {
                    throw RuntimeException("Empty permission result")
                }
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(
                        mProjectionManager?.createScreenCaptureIntent(),
                        REQUEST_CODE_RECORD
                    )
                } else {
                }
            }
        }
    }

    private var mediaProtectionPermission: Intent? = null

    protected fun getPermission(requestCode: Int) {
        try {
            if(isCapturing) {
//                MainActivity.showToast("화면 캡쳐중입니다.", 1000)
            } else if(isRecording) {
//                MainActivity.showToast("화면 녹화중입니다.", 1000)
            } else {
                if (hasMediaProtectionPermission) {
                    if(requestCode == REQUEST_CODE_CAPTURE) {
                        startProjection()
                    } else if(requestCode == REQUEST_CODE_RECORD) {
                        startScreenToVideoRecorder()
                    }
                } else {
                    openScreenshotPermissionRequester(requestCode)
                }
            }
        } catch (ignored: java.lang.RuntimeException) {
//            openScreenshotPermissionRequester(requestCode)
        }
    }

    protected fun openScreenshotPermissionRequester(requestCode: Int) {
        startActivityForResult(mProjectionManager?.createScreenCaptureIntent(), requestCode)
    }

    protected fun setScreenshotPermission(permissionIntent: Intent?, requestCode: Int) {
        mediaProtectionPermission = permissionIntent
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
                if (!Settings.canDrawOverlays(this)) {
                    MainActivity.showToast("'다른 앱 위에 표시 허용' 권한이 필요합니다.", 2000)
                    Handler(Looper.getMainLooper()).postDelayed({
                        checkPermission()
                    }, 2000)
                } else {
                    startService()
                }
            }
        } else {
            if(Activity.RESULT_OK == resultCode) {
                hasMediaProtectionPermission = true
                setScreenshotPermission(data?.clone() as Intent, requestCode)
                getPermission(requestCode)
            } else {
                hasMediaProtectionPermission = false
            }
        }
    }

    private fun startScreenToVideoRecorder() {
        if(!hasMediaProtectionPermission) {
            getPermission(REQUEST_CODE_RECORD)
        } else {
            try {
                val sMediaProjection = mProjectionManager?.getMediaProjection(
                    Activity.RESULT_OK,
                    mediaProtectionPermission?.clone() as Intent
                )
                sMediaProjection?.let {
                    try {
                        screenToVideoRecorder?.stop()
                    } catch (t: Throwable) {
                    }
                    screenToVideoRecorder = null
                    screenToVideoRecorder = ScreenToVideoRecorder(it)

                    if (screenToVideoRecorder  != null) {
                        val filePath = screenToVideoRecorder?.filePath
                        val outputUrl = File(filePath).toURI().toURL().toString()
                        val cbFunc = if(!SCREENCAP_DATA.has("cbfunc")) "" else SCREENCAP_DATA.get("cbfunc").asString
                        EventBus.sendJSRecordScreenResult(true, cbFunc, outputUrl)
                    } else {
                        EventBus.sendJSRecordScreenResult(false)
                    }

                    isRecording = true
                }
            } catch (ignored: java.lang.RuntimeException) {
                EventBus.sendJSNotifyError(1002)
//                MainActivity.showToast(ignored.localizedMessage, 1000)
            }
        }
    }

    private fun stopScreenToVideoRecorder() {
        if(isRecording) {
            screenToVideoRecorder.let { screenToVideoRecorder ->
                try {
                    if (screenToVideoRecorder  != null) {
                        var outputUrl= try {
                            screenToVideoRecorder.stop()
                        } catch (e: Exception) {
                            null
                        }
                        val cbFunc = if(!SCREENCAP_DATA.has("cbfunc")) "" else SCREENCAP_DATA.get("cbfunc").asString
                        this.screenToVideoRecorder = null
                        EventBus.sendJSRecordScreenResult(true, cbFunc, outputUrl)

//                        viewWeb.loadUrl(outputUrl)

                    } else {
                        EventBus.sendJSRecordScreenResult(false)
                    }

                    isRecording = false
                } catch (ignored: java.lang.RuntimeException) {
                    EventBus.sendJSNotifyError(1003)
                }
            }
        } else {
//            MainActivity.showToast("화면 녹화중이 아닙니다.", 1000)
        }
    }

    private inner class ImageAvailableListener : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader) {

            val rotation = getScreenOrientation(context)
            if (rotation != mRotation) {
                mRotation = rotation
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay?.release()
                    if (mImageReader != null) mImageReader?.setOnImageAvailableListener(null, null)

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay()
                    return

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            var image: Image? = null
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null

            try {
                image = reader.acquireLatestImage()

                val interval = if(!SCREENCAP_DATA.has("interval")) 1.0 else SCREENCAP_DATA.get("interval").asDouble
                var left = if(!SCREENCAP_DATA.has("left")) 0 else SCREENCAP_DATA.get("left").asInt
                var top = if(!SCREENCAP_DATA.has("top")) 0 else SCREENCAP_DATA.get("top").asInt
                var width = if(!SCREENCAP_DATA.has("width")) 0 else SCREENCAP_DATA.get("width").asInt
                var height = if(!SCREENCAP_DATA.has("height")) 0 else SCREENCAP_DATA.get("height").asInt

                var resizeWidth = if(!SCREENCAP_DATA.has("resizeWidth")) 0 else SCREENCAP_DATA.get("resizeWidth").asInt
                var resizeHeight = if(!SCREENCAP_DATA.has("resizeHeight")) 0 else SCREENCAP_DATA.get(
                    "resizeHeight"
                ).asInt
                val cbFunc = if(!SCREENCAP_DATA.has("cbfunc")) "" else SCREENCAP_DATA.get("cbfunc").asString
                val imgType = if(!SCREENCAP_DATA.has("imgtype")) "jpg" else SCREENCAP_DATA.get("imgtype").asString

                val unixTime = System.currentTimeMillis()
                if(unixTime < SCREENCAP_LAST + (interval*1000)) {
                    Log.d(
                        "",
                        "returned image: " + IMAGES_PRODUCED + " ${SCREENCAP_LAST} ${unixTime} ${interval}"
                    )
                    return
                }

                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * mWidth

                    // create bitmap
                    bitmap = Bitmap.createBitmap(
                        mWidth + rowPadding / pixelStride,
                        mHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    //image cropppig...
                    if(width > 0 && height > 0) {
                        if(left+width > mWidth) {
                            width = mWidth-left
                        }
                        if(top+height > mHeight) {
                            height = mHeight-top
                        }
                        bitmap = ImageUtils.getCroppedBitmap(bitmap, left, top, width, height)
                    }

                    //image resizing...
                    if(resizeWidth != 0 && resizeWidth != mWidth) {
                        resizeHeight = (resizeWidth * mHeight) / mWidth
                        bitmap = Bitmap.createScaledBitmap(
                            bitmap!!,
                            resizeWidth,
                            resizeHeight,
                            true
                        )
                    }

                    IMAGES_PRODUCED++
                    val byteOutputStream = ByteArrayOutputStream()
                    if(imgType.equals("jpg")) {
                        fos = FileOutputStream(STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".${imgType}")
                        // write bitmap to a file
                        Log.d("", "captured image: " + IMAGES_PRODUCED + " ${SCREENCAP_LAST}")
                        bitmap?.compress(Bitmap.CompressFormat.JPEG, Constants.JPEG_QUALITY, fos)
                        bitmap?.compress(
                            Bitmap.CompressFormat.JPEG,
                            Constants.JPEG_QUALITY,
                            byteOutputStream
                        )
                    } else {
                        fos = FileOutputStream(STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".${imgType}")
                        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, byteOutputStream)

//                        val fos1 = FileOutputStream(STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + "_${resizeWidth}_${resizeHeight}.png")
//                        resizedBM?.let {
//                            resizedBM.compress(Bitmap.CompressFormat.PNG, 100, fos1)
//                        }
//                        fos1.close()
                    }

                    val b = byteOutputStream.toByteArray()
                    val encImage: String = Base64.encodeToString(b, Base64.DEFAULT)

//                    Log.d("captured Screen bytes", encImage)
                    EventBus.sendJSCaptureScreenResult(true, cbFunc, imgType, encImage)
                    SCREENCAP_LAST = unixTime
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos?.let {
                    try {
                        it.close()
                    } catch (ioe: IOException) {
                        ioe.printStackTrace()
                    }
                }
                bitmap?.recycle()
                image?.close()
            }
        }
    }

    fun getScreenOrientation(context: Context): Int {
        val screenOrientation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.orientation
        return  screenOrientation
    }

    private inner class MediaProjectionStopCallback : MediaProjection.Callback() {
        override fun onStop() {
            android.util.Log.e("ScreenCapture", "stopping projection.")
            mHandler?.post {
                if (mVirtualDisplay != null) mVirtualDisplay?.release()
                if (mImageReader != null) mImageReader?.setOnImageAvailableListener(null, null)
                sMediaProjectionCapture?.let {
                    it.unregisterCallback(this@MediaProjectionStopCallback)
                }
            }
        }
    }

    /****************************************** UI Widget Callbacks  */
    private fun startProjection() {
        if(!hasMediaProtectionPermission) {
            getPermission(REQUEST_CODE_CAPTURE)
        } else {
            try {
                val sMediaProjection = mProjectionManager?.getMediaProjection(
                    Activity.RESULT_OK,
                    mediaProtectionPermission?.clone() as Intent
                )
                sMediaProjection.let {
                    sMediaProjectionCapture = sMediaProjection
                    val externalFilesDir = getExternalFilesDir(null)
                    if (externalFilesDir != null) {
                        STORE_DIRECTORY = externalFilesDir.absolutePath + "/screenshots/"
                        val storeDirectory = File(STORE_DIRECTORY)
                        if (!storeDirectory.exists()) {
                            val success = storeDirectory.mkdirs()
                            if (!success) {
                                Log.d("", "failed to create file storage directory.")
                                return
                            }
                        }
                    } else {
                        Log.d(
                            "",
                            "failed to create file storage directory, getExternalFilesDir is null."
                        )
                        return
                    }

                    // display metrics
                    val metrics = resources.displayMetrics
                    mDensity = metrics.densityDpi
                    mDisplay = windowManager.defaultDisplay

                    // create virtual display depending on device width / height
                    createVirtualDisplay()

                    // register media projection stop callback
                    sMediaProjectionCapture?.registerCallback(
                        MediaProjectionStopCallback(),
                        mHandler
                    )

                    isCapturing = true
                }
            } catch (ignored: java.lang.RuntimeException) {
                MainActivity.showToast(ignored.localizedMessage, 1000)
                EventBus.sendJSNotifyError(1004)
            }
        }
    }

    private fun stopProjection() {
        if(isCapturing) {
            try {
                IMAGES_PRODUCED = 0
                mHandler?.post {
                    if (sMediaProjectionCapture != null) {
                        sMediaProjectionCapture?.stop()
                        sMediaProjectionCapture = null
                    }
                    isCapturing = false
                }
            } catch (ignored: java.lang.RuntimeException) {
                EventBus.sendJSNotifyError(1005)
            }
        } else {
//            MainActivity.showToast("화면 캡쳐중이 아닙니다.", 1000)
        }
    }

    /****************************************** Factoring Virtual Display creation  */
    private fun createVirtualDisplay() {
        // get width and height
        val size = Point()
        mDisplay?.getSize(size)
        mWidth = size.x
        mHeight = size.y

        val realMetrics = DefaultDisplay.realMetrics
        mWidth = realMetrics.widthPixels
        mHeight = realMetrics.heightPixels
        mDensity = realMetrics.densityDpi

        // start capture reader
        val imageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.FLEX_RGBA_8888, 2)
//        val imageReader = ImageReader.newInstance(mWidth, mHeight, android.graphics.PixelFormat.RGBA_8888, 2)
        mVirtualDisplay = sMediaProjectionCapture?.createVirtualDisplay(
            SCREENCAP_NAME,
            mWidth,
            mHeight,
            mDensity,
            VIRTUAL_DISPLAY_FLAGS,
            imageReader.getSurface(),
            null,
            mHandler
        )

        imageReader.setOnImageAvailableListener(ImageAvailableListener(), mHandler)

        mImageReader = imageReader
    }

    override fun onBackPressed() {
        if (backPressedToExitOnce) {

            if(foregroundServiceIntent != null) {
                stopService(foregroundServiceIntent)
                foregroundServiceIntent = null
            }

            super.onBackPressed()
            finish()

        } else {
            backPressedToExitOnce = true
            MainActivity.showToast("Press again to exit", 1000)
            Handler().postDelayed({ backPressedToExitOnce = false }, 2000)
        }
    }

    private fun initializeImmersiveStickyMode() {
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect();
            window.decorView.getWindowVisibleDisplayFrame(rect)

            val keyboardHeight = window.decorView.height - rect.bottom;
            if (keyboardHeight <= window.decorView.height * 0.15) {
                setImmersiveStickyMode()
            }
        }
    }

    /**
     * Called when the current Window of the activity gains or loses focus.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            setImmersiveStickyMode()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> return true
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun setImmersiveStickyMode() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun enableSoftwareKeyboard(enable: Boolean) {
        if (enable) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            hideKeyboardDisposable?.let {
                it.dispose()
                hideKeyboardDisposable = null
            }
            EventBus.sendJSResult(NotifyShowSoftwareKeyboardResult(true))
        } else {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

            if (hideKeyboardDisposable == null) {
                hideKeyboard()
            }
        }
    }

    private fun hideKeyboard() {
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.getWindowToken(), 0)
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("Test", "wifi available")
            if(!MainActivity.bConnected) {
                MainActivity.bConnected = isNetworkAvailable(context)
                if(MainActivity.bConnected) {
                    EventBus.send(ReloadNetwork())
                }
            }
        }

        override fun onLost(network: Network?) {
            Log.d("Test", "wifi unavailable")
            if(MainActivity.bConnected) {
                MainActivity.bConnected = isNetworkAvailable(context)
                if(!MainActivity.bConnected) {
                    Log.d("Test", "network unavailable")
                    EventBus.send(ReloadNetwork())
                }
            }
        }
    }

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        cm.registerNetworkCallback(wifiNetworkRequest, networkCallback)

        val cellNetworkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        cm.registerNetworkCallback(cellNetworkRequest, networkCallback)
    }

    private fun unregisterNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.unregisterNetworkCallback(networkCallback)
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    companion object : BaseActivityCompanion() {

        var lifecycleRegistry: LifecycleRegistry? = null
        private lateinit var countdownTimer : CountDownTimer
        lateinit var context : Context;
        var bConnected = true
        var downloadTarget = ""
        var downloadProgress = 0
        var downloadTotalSize = 0
        var downloadedSize = 0

        fun popupOneButtonDialog(
            title: String,
            message: String,
            buttonString: String,
            clickListener: DialogInterface.OnClickListener
        ){
            val builder: AlertDialog.Builder = AlertDialog.Builder(context);
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton(buttonString, clickListener)
            val dlg = builder.create()
            dlg.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION)
            dlg.show()
        }

        fun showToast(message: String, lifeTime: Long) {
            var stringBuilder = SpannableStringBuilder();
            stringBuilder.append(message);
            val toast = Toast.makeText(MainApplication.appContext, stringBuilder, Toast.LENGTH_LONG);
            countdownTimer = object : CountDownTimer(lifeTime, 1000) {
                override fun onFinish() {
                    Log.d("result toast", "Finish Toast")
                }
                override fun onTick(millisUntilFinished: Long) {
                    toast.show();
                }
            }.start()
        }
    }
}
