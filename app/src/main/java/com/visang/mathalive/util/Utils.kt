package com.visang.mathalive.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import androidx.core.content.ContextCompat
import android.util.Base64
import android.util.LongSparseArray
import com.visang.mathalive.MainApplication
import java.io.File
import java.io.FileOutputStream


object Utils {

    fun save(data: ByteArray, filePath: String): File {
        val outputFile = File(filePath)

        val outputStream = FileOutputStream(outputFile)
        outputStream.write(data)
        outputStream.close()

        return outputFile
    }

    fun encodeBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.NO_WRAP)
    }

    fun getString(resId: Int): String = MainApplication.appContext.getString(resId)

    fun getScaledRect(rect: Rect, xScale: Float, yScale: Float) =
            Rect(
                Math.round(rect.left * xScale),
                Math.round(rect.top * yScale),
                Math.round(rect.right * xScale),
                Math.round(
                    rect.bottom * yScale
                )
            )

    fun getNotGrantedPermissions(vararg permissions: String): Array<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return permissions.filter({
                ContextCompat.checkSelfPermission(
                    MainApplication.appContext,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            })
                    .toTypedArray()
        }

        return arrayOf()
    }

    fun forceCrash() {
        throw ForcedException()
    }

    fun getDeviceScaledRect(rect: Rect) =
        DefaultDisplay.size.let { size ->
            getScaledRect(rect, size.width / 1280f, size.height / 800f)
        }

    fun isAppRunning(appName: String): Boolean {

        var context = MainApplication.appContext
        var timeMills = 1 * 60 * 1000

        var result = false
        //timeMils = 1000
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()

        val usageEvents = usm.queryEvents(time - timeMills, time)
        val eventMap = LongSparseArray<UsageEvents.Event>()
        var lastRunAppTimeStamp = 0L

        while (usageEvents.hasNextEvent()) {
            // 현재 이벤트를 가져오기
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            val pkgName = event.packageName

            if (appName == pkgName) {
                eventMap.put(event.timeStamp, event)
                if (event.timeStamp > lastRunAppTimeStamp) {
                    lastRunAppTimeStamp = event.timeStamp
                }
//                Log.d("eventType", event.eventType.toString())
//                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.eventType == UsageEvents.Event.ACTIVITY_RESUMED else event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }

            // 현재 이벤트가 포그라운드 상태라면 = 현재 화면에 보이는 앱이라면
//            if (isForeGroundEvent(event)) {
//                // 해당 앱 이름을 packageNameMap에 넣는다.
//                packageNameMap.put(event.timeStamp, event.packageName)
//                // 가장 최근에 실행 된 이벤트에 대한 타임스탬프를 업데이트 해준다.
//                if (event.timeStamp > lastRunAppTimeStamp) {
//                    lastRunAppTimeStamp = event.timeStamp
//                }
//            }

        }

        val lastEvent = eventMap.get(lastRunAppTimeStamp)
        if(lastEvent != null) {
            result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) lastEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED else lastEvent.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
        }
//        packageNameMap.get(lastRunAppTimeStamp, "").toString()

//        val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - timeMills, time)
//        if (appList != null && appList.size > 0) {
//            for (usageStats in appList) {
//                val pkgName = usageStats.packageName
//                if (appName == pkgName) {
//                    result = true
//                }
//            }
//        }
        return result
    }

    fun isForeGroundEvent(event: UsageEvents.Event?): Boolean {
        if (event == null) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) event.eventType == UsageEvents.Event.ACTIVITY_RESUMED else event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
    }
}
