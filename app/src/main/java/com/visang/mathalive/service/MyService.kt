package com.visang.mathalive.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.widget.Toast
import com.visang.mathalive.R
import com.visang.mathalive.activity.MainActivity
import com.visang.mathalive.eventbus.EventBus
import com.visang.mathalive.eventbus.HideReload
import com.visang.mathalive.eventbus.QuitForUpdate
import com.visang.mathalive.eventbus.ShowProgress
import com.visang.mathalive.util.DownloadController
import com.visang.mathalive.util.Logger
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.schedule


const val CHANNEL_ID = "my_channel_id"
const val CHANNEL_NAME = "My Forground Service"

class MyService : Service() {

    var mReceiver: BroadcastReceiver? = null

    companion object {
        fun newService(context: Context): Intent =
            Intent(context, MyService::class.java)
    }

    // use this as an inner class like here or as a top-level class
    class MyReceiver : BroadcastReceiver() {
        var isDownloading: Boolean = false

        inner class URLExistTask : AsyncTask<String, Void, Boolean>() {
            override fun doInBackground(vararg params: String?): Boolean {
                return try {
                    HttpURLConnection.setFollowRedirects(false)
                    val con: HttpURLConnection = URL(params[0]).openConnection() as HttpURLConnection
                    con.setConnectTimeout(1000)
                    con.setReadTimeout(1000)
                    con.setRequestMethod("HEAD")
                    con.getResponseCode() === HttpURLConnection.HTTP_OK
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
        }

        override fun onReceive(context: Context, intent: Intent) {
            // do something
            val cmd = intent.getStringExtra("CMD")
            Logger.d("BroadcastReceiver : ${cmd}")

            if (cmd.equals("LaunchApp", ignoreCase = true)) {
                val data = intent.getStringExtra("DATA")
                val context = MainActivity.context
                try {
//                    var _jsonData = Gson().fromJson(data, JsonObject::class.java)
//                    var sValue = _jsonData["value"].asString;

                    var packageName = "com.Visang.VisangMath2"
//                    if(data.indexOf("Z.", 0) == 0) {
//                        packageName = "com.Visang.VisangMath"
//                    }
                    Logger.d("_packageName : ${packageName}")

                    try {
//                        packageName = "com.sec.android.gallery3d"

                        var pm: PackageManager = context.getPackageManager()
                        var info = pm.getPackageInfo("" + packageName, PackageManager.GET_META_DATA)
                        Logger.d("Pkg Info : ${info}")

                    } catch (e: PackageManager.NameNotFoundException) {
                    }

                    val launchIntent: Intent? =  context.getPackageManager().getLaunchIntentForPackage(
                        packageName
                    )
                    launchIntent?.let {
                        it.putExtra("toolKey", data)
                        context.startActivity(it)
                    }
                } catch (ignored: java.lang.RuntimeException) {
                    EventBus.sendJSNotifyError(1000)
                }
            } else if(cmd.equals("CloseApp", ignoreCase = true)) {
                try {
                    val intent = Intent(context, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    context.startActivity(intent)
                } catch (ignored: java.lang.RuntimeException) {
                    EventBus.sendJSNotifyError(1001)
                }

//                val url = "mathalive://open"
//                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
//                context.startActivity(intent)

//                var am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//                am.runningAppProcesses.forEach {
//                    Logger.d("BroadcastReceiver : ${it.processName}")
//                    if(it.processName == "com.visang.mathalive") {
//                        val intent = Intent(context, MainActivity::class.java)
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                        context.startActivity(intent)
//                    }
//                }
            } else if(cmd.equals("startRecord", ignoreCase = true)) {
            } else if(cmd.equals("onDestroy", ignoreCase = true)) {
//                Timer("onDestroy", false).schedule(1 * 1000) {
//                    val intent = Intent(context, MainActivity::class.java)
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
////                    intent.addFlags(Intent.FLAG_FROM_BACKGROUND)
//                    context.startActivity(intent)
//                }

                val bQuit = intent.getBooleanExtra("Quit", true)
                if(bQuit) {
                    Timer("onDestroy", false).schedule(1 * 1000) {
                        val serviceIntent = MyService.newService(context)
                        context.stopService(serviceIntent)
                    }
                }
            } else if(cmd.equals("updateApp", ignoreCase = true)) {
                val url = intent.getStringExtra("url")
                val target = intent.getStringExtra("target")
                val version = intent.getStringExtra("version")


                val urlexist: Boolean = URLExistTask().execute(url).get()
                if (!urlexist) {
                    Toast.makeText(context, context.getString(R.string.notfound), Toast.LENGTH_LONG).show()
                    return
                }

                val downloadController = DownloadController(context, url, target, version)
                downloadController.enqueueDownload()

//                if(target == "me") {
//                    EventBus.send(QuitForUpdate())
//                }
            } else if(cmd.equals("CompleteUpdateApp", ignoreCase = true)) {
                val target = intent.getStringExtra("target")
                Logger.d("CompleteUpdateApp : ${target}")
            } else if(cmd.equals("DownloadProgress", ignoreCase = true)) {
                val progress = intent.getIntExtra("progress", 0)
                val target = intent.getStringExtra("target")
                val filesize = intent.getIntExtra("filesize", 0)
                val downloadedfilesize = intent.getIntExtra("downloadedfilesize", 0)

                MainActivity.downloadTarget = target
                MainActivity.downloadProgress = progress
                MainActivity.downloadTotalSize = filesize
                MainActivity.downloadedSize = downloadedfilesize

                EventBus.send(ShowProgress())

//                Logger.d("DownloadProgress : ${target} ${progress}")
//                EventBus.sendJSNotifyDownloadProgress(true, "downloadProgress", target, progress, filesize, downloadedfilesize)
            }
        }
    }

    override fun onCreate() {
        // get an instance of the receiver in your service
//        val filter = IntentFilter()
//        filter.addAction("action")
//        filter.addAction("anotherAction")
        mReceiver = MyReceiver()
        registerReceiver(mReceiver, IntentFilter("INTENT_FILTER_ACCESS_SERVICE"))

//        initWindowLayout(getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
//        windowBinding.initCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)

        EventBus.send(QuitForUpdate())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); // Foreground service 종료
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val builder: NotificationCompat.Builder

        // 안드로이드 O 버전 이상에서부터는 채널 ID를 지정해주어야 합니다.
        builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )

            NotificationCompat.Builder(this, CHANNEL_ID)

        } else {
            NotificationCompat.Builder(this)
        }

        val notification = builder
            .setContentTitle("Title")
            .setContentText("Body")
            .setContentIntent(pendingIntent)
            .build()

        // startForeground()에 지정하는 정수 ID는 0이면 안됩니다.
        startForeground(10001, notification)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}