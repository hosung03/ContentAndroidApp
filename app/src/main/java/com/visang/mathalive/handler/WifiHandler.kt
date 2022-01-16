package com.visang.mathalive.handler

import android.Manifest
import androidx.lifecycle.Lifecycle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.SupplicantState
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.visang.mathalive.MainApplication
import com.visang.mathalive.activity.MainActivity
import com.visang.mathalive.eventbus.*
import com.trello.rxlifecycle2.LifecycleProvider
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import com.trello.rxlifecycle2.kotlin.bindUntilEvent
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import org.jetbrains.anko.wifiManager

class WifiHandler(val lifecycleProvider: LifecycleProvider<Lifecycle.Event>) {

    init {
        EventBus.observable
                .bindToLifecycle(lifecycleProvider)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is GetActiveWifiList -> getActiveWifiList()
                        is TryConnectWifi -> tryConnectWifi(it.ssid, it.password)
                    }
                }
    }

    private fun getActiveWifiList() {
        getWifiScanResults()
                .bindUntilEvent(lifecycleProvider, Lifecycle.Event.ON_DESTROY)
                .subscribe({
                    EventBus.sendJSResult(NotifyGetActiveWifiListResult(true, it))
                }, {
                    EventBus.sendJSResult(NotifyGetActiveWifiListResult(false))
                })
    }

    private fun createWifiConfiguration(ssid: String, capabilities: String, password: String) =
        WifiConfiguration().apply {
            SSID = "\"$ssid\""

            when {
                capabilities.contains("WEP") -> {
                    wepKeys[0] = "\"$password\""
                    wepTxKeyIndex = 0
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
                }
                capabilities.contains("WPA") -> {
                    preSharedKey = "\"$password\""
                }
                else -> {
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
            }
        }

    private fun disconnect(): Completable =
            Completable.create { emitter ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.getParcelableExtra<SupplicantState>(WifiManager.EXTRA_NEW_STATE) == SupplicantState.DISCONNECTED) {
                            emitter.onComplete()
                        }
                    }
                }

                MainApplication.appContext.registerReceiver(receiver, IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))
                MainApplication.appContext.wifiManager.disconnect()

                emitter.setCancellable {
                    MainApplication.appContext.unregisterReceiver(receiver)
                }
            }

    private fun disconnectIfAny(): Completable {
        val wifiManager = MainApplication.appContext.wifiManager

        val connectionInfo = wifiManager.connectionInfo

        if (connectionInfo.supplicantState != SupplicantState.COMPLETED) {
            return Completable.complete()
        }

        return disconnect()
    }

    private fun connect(ssid: String, capabilities: String, password: String): Completable =
            Completable.create { emitter ->
                val wifiManager = MainApplication.appContext.wifiManager

                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.getParcelableExtra<SupplicantState>(WifiManager.EXTRA_NEW_STATE) == SupplicantState.COMPLETED) {
                            if (wifiManager.connectionInfo.ssid.replace(Regex("^\"(.*)\"$"), "$1").equals(ssid)) {
                                emitter.onComplete()
                            } else {
                                if (!emitter.isDisposed) emitter.onError(Throwable("ERROR"))
                            }
                        } else if (intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1) == WifiManager.ERROR_AUTHENTICATING) {
                            if (!emitter.isDisposed) emitter.onError(Throwable("ERROR_AUTHENTICATING"))
                        }
                    }
                }

                MainApplication.appContext.registerReceiver(receiver, IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION))

                val wifiConfiguration = createWifiConfiguration(ssid, capabilities, password)
                val networkId = wifiManager.addNetwork(wifiConfiguration)

                wifiManager.enableNetwork(networkId, true)
                wifiManager.reconnect()

                emitter.setCancellable {
                    MainApplication.appContext.unregisterReceiver(receiver)
                }
            }

    private fun connectWifi(ssid: String, capabilities: String, password: String): Completable =
            disconnectIfAny()
                    .andThen(connect(ssid, capabilities, password))

    private fun tryConnectWifi(ssid: String, password: String) {
        getWifiScanResults()
                .bindUntilEvent(lifecycleProvider, Lifecycle.Event.ON_DESTROY)
                .flatMapCompletable {
                    val wifiScanResult = it.find { it.SSID.equals(ssid) }
                    connectWifi(ssid, wifiScanResult!!.capabilities, password)
                }
                .subscribe({
                    EventBus.sendJSResult(NotifyTryConnectWifiResult(true))
                }, {
                    EventBus.sendJSResult(NotifyTryConnectWifiResult(false))
                })
    }

    private fun getWifiScanResults(): Maybe<List<ScanResult>> =
            MainActivity.checkPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
                    .andThen(
                            Maybe.create { emitter ->
                                val receiver = object : BroadcastReceiver() {
                                    override fun onReceive(context: Context, intent: Intent) {
                                        when (intent.action) {
                                            WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> emitter.onSuccess(MainApplication.appContext.wifiManager.scanResults)
                                        }
                                    }
                                }

                                MainApplication.appContext.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))

                                emitter.setCancellable {
                                    MainApplication.appContext.unregisterReceiver(receiver)
                                }

                                MainApplication.appContext.wifiManager.startScan()
                            }
                    )

}
