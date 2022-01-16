package com.visang.mathalive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.visang.mathalive.BuildConfig
import com.visang.mathalive.R
import com.visang.mathalive.activity.MainActivity
import com.visang.mathalive.util.PrefManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class MyFcmService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if(remoteMessage != null && remoteMessage.notification !== null) {
            val title = remoteMessage.notification!!.title
            val message = remoteMessage.notification!!.body
            sendNotification(title, message)
        }
        if (remoteMessage != null && remoteMessage.data.size > 0) {
            val title = remoteMessage.data["title"]
            val message = remoteMessage.data["message"]
            sendNotification(title, message)
        }
    }
    private fun sendNotification(title: String?, message: String?) {
        val CHANNEL_ID = "FirebaseMessagingService_ID"
        val mManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_NAME = "FirebaseMessagingService_NAME"
            val CHANNEL_DESCRIPTION = "FirebaseMessagingService_Description"
            val importance = NotificationManager.IMPORTANCE_HIGH

            // add in API level 26
            val mChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            mChannel.description = CHANNEL_DESCRIPTION
            mChannel.enableLights(true)
            mChannel.enableVibration(true)
            mChannel.vibrationPattern = longArrayOf(100, 200, 100, 200)
            mChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            mManager.createNotificationChannel(mChannel)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
//        builder.setSmallIcon(R.drawable.ic_launcher_background)
        builder.setAutoCancel(true)
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.setWhen(System.currentTimeMillis())
        builder.setSmallIcon(R.mipmap.ic_launcher)
//        builder.setColor(0xffffaec9.toInt())
        builder.setContentTitle(title)
        builder.setContentText(message)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setContentTitle(title)
            builder.setVibrate(longArrayOf(500, 500))
        }
        val notificationId = SimpleDateFormat("ddHHmmss").format(Date()).toInt()
        mManager.notify(notificationId, builder.build())

        if(MainActivity.lifecycleRegistry != null) {
            Timer("cancelNoti", false).schedule(1 * 1000) {
                 mManager.cancel(notificationId)
            }
        }
    }
    override fun onNewToken(token: String) {
//        super.onNewToken(token)
        token?.let { fcmToken ->
            // FCM 토큰을 서버에 올리는게 성공하면 preference에 저장값 지운다.
            // 만약, 실패한다면 앱을 재실행할 때, 다시 서버에 올리는 것을 시도하고 성공하면 지운다.
            if (!BuildConfig.DEBUG) {
                PrefManager.savePushToken(applicationContext, fcmToken)
            }
        }
    }
}