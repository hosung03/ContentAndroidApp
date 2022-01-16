package com.visang.mathalive.util

import android.graphics.Point
import android.util.DisplayMetrics
import android.util.Size
import android.view.Display
import android.view.Surface
import com.visang.mathalive.MainApplication
import org.jetbrains.anko.windowManager

object DefaultDisplay {

    private val defaultDisplay: Display
        get() = MainApplication.appContext.windowManager.defaultDisplay

    val size: Size
        get() {
            val displaySize = Point()
            defaultDisplay.getRealSize(displaySize)
            return Size(displaySize.x, displaySize.y)
        }

    val realMetrics: DisplayMetrics
        get() {
            val displayMetrics = DisplayMetrics()
            defaultDisplay.getRealMetrics(displayMetrics)
            return displayMetrics
        }

    val rotation: Int
        get() = defaultDisplay.rotation

    val rotationDegree: Int
        get() =
                when (rotation) {
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 0
                }

}
