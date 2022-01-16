package com.visang.mathalive.activity

import android.annotation.SuppressLint
import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleRegistryOwner
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent
import com.visang.mathalive.eventbus.ActivityEvent
import com.visang.mathalive.eventbus.EventBus
import com.visang.mathalive.eventbus.Quit
import com.visang.mathalive.util.Utils
import com.trello.lifecycle2.android.lifecycle.AndroidLifecycle
import com.trello.rxlifecycle2.LifecycleProvider
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicInteger

open class BaseActivity(val activityId: String) : AppCompatActivity(), LifecycleRegistryOwner {

    val lifecycleRegistry = LifecycleRegistry(this)
    val lifecycleProvider: LifecycleProvider<Lifecycle.Event> = AndroidLifecycle.createLifecycleProvider(this)

    /**
     * Called when the activity is starting.
     */
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.observable
                .bindToLifecycle(lifecycleProvider)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    when (it) {
                        is Quit -> quit()
                    }
                }

        eventObservable(activityId)
                .bindToLifecycle(lifecycleProvider)
                .subscribe {
                    when (it) {
                        is InternalEvent.RunWithActivity -> it.func(this)
                    }
                }
    }

    /**
     * Called when the activity is becoming visible to the user.
     * Followed by onResume() if the activity comes to the foreground, or onStop() if it becomes hidden.
     */
    override fun onStart() {
        super.onStart()
        sendEvent(activityId, ActivityEvent.OnStart())
    }

    override fun onResume() {
        super.onResume()
        sendEvent(activityId, ActivityEvent.OnResume())
    }

    override fun onPause() {
        super.onPause()
        sendEvent(activityId, ActivityEvent.OnPause())
    }

    /**
     * Called when the activity is no longer visible to the user, because another activity has been resumed and is covering this one.
     * This may happen either because a new activity is being started, an existing one is being brought in front of this one, or this one is being destroyed.
     * Followed by either onRestart() if this activity is coming back to interact with the user, or onDestroy() if this activity is going away.
     */
    override fun onStop() {
        super.onStop()
        sendEvent(activityId, ActivityEvent.OnStop())
    }

    /**
     * Callback for the result from requesting permissions.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        sendEvent(activityId, InternalEvent.OnRequestPermissionsResult(requestCode, grantResults.isNotEmpty() && grantResults.all({ it == PackageManager.PERMISSION_GRANTED })))
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with, the resultCode it returned, and any additional data from it.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        sendEvent(activityId, InternalEvent.OnActivityResult(requestCode, resultCode, data))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        sendEvent(activityId, ActivityEvent.OnKeyDown(keyCode))
        return super.onKeyDown(keyCode, event)
    }

    override fun getLifecycle() = lifecycleRegistry

    private fun quit() {
//        GRPC.leaveStream()
        MainActivity.lifecycleRegistry = null
        finish()
    }

    companion object {
        private class ActivityEventWrap(val activityId: String, val event: Any)
        private class InternalEvent {
            class RunWithActivity(val func: Activity.() -> Unit)
            class OnRequestPermissionsResult(val requestCode: Int, val allGranted: Boolean)
            class OnActivityResult(val requestCode: Int, val resultCode: Int, val data: Intent?)
        }

        private val eventObservable = PublishSubject.create<ActivityEventWrap>().toSerialized()
        private val nextRequestCode = AtomicInteger()

        fun sendEvent(activityId: String, event: Any) {
            eventObservable.onNext(ActivityEventWrap(activityId, event))
        }

        fun eventObservable(activityId: String): Observable<Any> =
                eventObservable
                        .filter { it.activityId == activityId }
                        .map { it.event }

        fun runWithActivity(activityId: String, func: Activity.() -> Unit) {
            sendEvent(activityId, InternalEvent.RunWithActivity(func))
        }

        fun checkPermissions(activityId: String, vararg permissions: String): Completable {
            val missingPermissions = Utils.getNotGrantedPermissions(*permissions)

            if (missingPermissions.isNotEmpty()) {
                return Completable.create { emitter ->
                    val requestCode = nextRequestCode.getAndIncrement()
                    val disposable = eventObservable(activityId)
                            .subscribe {
                                if (it is InternalEvent.OnRequestPermissionsResult && it.requestCode == requestCode && !emitter.isDisposed) {
                                    if (it.allGranted)
                                        emitter.onComplete()
                                    else
                                        emitter.onError(Throwable("Permission denied"))
                                }
                            }

                    emitter.setDisposable(disposable)
                    runWithActivity(activityId) {
                        ActivityCompat.requestPermissions(this, missingPermissions, requestCode)
                    }
                }
            }

            return Completable.complete()
        }

        fun startActivityGetResult(activityId: String, intent: Intent): Maybe<Pair<Int, Intent?>> =
                Maybe.create { emitter ->
                    val requestCode = nextRequestCode.getAndIncrement()
                    val disposable = eventObservable(activityId)
                            .subscribe {
                                if (it is InternalEvent.OnActivityResult && it.requestCode == requestCode) {
                                    emitter.onSuccess(Pair(it.resultCode, it.data))
                                }
                            }

                    emitter.setDisposable(disposable)

                    runWithActivity(activityId) {
                        startActivityForResult(intent, requestCode)
                    }
                }

    }
}
