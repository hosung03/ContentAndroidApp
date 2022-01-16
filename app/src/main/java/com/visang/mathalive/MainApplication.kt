package com.visang.test

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import androidx.multidex.MultiDexApplication
import com.facebook.stetho.Stetho
import com.github.pwittchen.reactivewifi.ReactiveWifi
import com.visang.mathalive.eventbus.DeviceDisplayChanged
import com.visang.mathalive.eventbus.EventBus
import com.visang.mathalive.util.*
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.SimpleWebServer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.displayManager
import org.jetbrains.anko.toast
import java.io.IOException

/**
 * Main application
 *
 * Main application class.
 */
class MainApplication : MultiDexApplication() {

    private val logCatManager = LogCatManager()
    private val crashlyticsLogWaitTimeMs = 5000L

    /**
     * Application이 시작할 때 호출됨.
     */
    override fun onCreate() {
        super.onCreate()

        MainApplication._appContext = applicationContext

        Logger.initialize()
        initializeStetho()
        initializeRxJava()
        PathUtils.initialize()
        initializeWifiStateChangeHandler()
//        initializeLocalWebServer()
        initializeDisplayListener()
        initializeUncaughtExceptionHandler()
    }

    /**
     * Stetho를 초기화한다.
     */
    private fun initializeStetho() {
        Stetho.initializeWithDefaults(this)
    }

    private fun initializeRxJava() {
        RxJavaPlugins.setErrorHandler {
            if (it is ForcedException) {
                throw it
            } else {
                it.cause?.let {
                    if (it is ForcedException) {
                        throw it
                    }
                }
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun initializeWifiStateChangeHandler() {
        ReactiveWifi.observeWifiSignalLevel(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("stream-wifi", "WifiSignalLevel : $it")
                })

        ReactiveWifi.observeWifiAccessPointChanges(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("stream-wifi", "WifiAccessPointChanges : $it")
                })

        ReactiveWifi.observeSupplicantState(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("stream-wifi", "SupplicantState : $it")
                })

        ReactiveWifi.observeWifiStateChange(applicationContext)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d("stream-wifi", "WifiStateChange : $it")
                })
    }

    private fun initializeLocalWebServer() {
        val server = SimpleWebServer("127.0.0.1", Constants.LOCAL_WEB_SERVER_PORT, PathUtils.getLocalWebServerHome(), true)
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        } catch (e: IOException) {
            toast("Server creation failed")
        }
    }

    private fun initializeDisplayListener() {
        MainApplication.appContext.displayManager.registerDisplayListener(object : DisplayManager.DisplayListener {
            override fun onDisplayChanged(displayId: Int) {
                EventBus.send(DeviceDisplayChanged(displayId))
            }
            override fun onDisplayAdded(displayId: Int) {
            }
            override fun onDisplayRemoved(displayId: Int) {
            }
        }, null)
    }

    private fun initializeUncaughtExceptionHandler() {
        val origUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            val logs = logCatManager.getLog()

            logs.forEach {
//                Crashlytics.log(it)
            }

//            Crashlytics.log(Log.DEBUG, "mathalive-crash", "${logs.size} logs were reported")

            try {
                Thread.sleep(crashlyticsLogWaitTimeMs)
            } catch (t: Throwable) {
                Log.e("mathalive-crash", "Log reporting was interrupted")
            }

            origUncaughtExceptionHandler.uncaughtException(t, e)
        }
    }

    companion object {

        private var _appContext: Context? = null

        val appContext: Context
            get() = _appContext!!

    }
}
