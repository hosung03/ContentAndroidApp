package com.visang.mathalive.activity

import android.app.Activity
import android.content.Intent

open class BaseActivityCompanion {
    val activityId: String = this::class.java.name
    fun sendEvent(event: Any) = BaseActivity.sendEvent(activityId, event)
    fun eventObservable() = BaseActivity.eventObservable(activityId)
    fun runWithActivity(func: Activity.() -> Unit) = BaseActivity.runWithActivity(activityId, func)
    fun checkPermissions(vararg permissions: String) = BaseActivity.checkPermissions(activityId, *permissions)
    fun startActivityGetResult(intent: Intent) = BaseActivity.startActivityGetResult(activityId, intent)
}
