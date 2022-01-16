package com.visang.mathalive.media

import android.media.CamcorderProfile
import com.visang.mathalive.MainApplication
import com.visang.mathalive.util.DefaultDisplay
import org.jetbrains.anko.landscape


object MediaUtils {

    // https://github.com/JakeWharton/Telecine/blob/72c1b15dadf3373a39d915b50984bdda09669ee4/telecine/src/main/java/com/jakewharton/telecine/RecordingSession.java#L415-L427
    class RecordingInfo(val width: Int, val height: Int, val frameRate: Int, val density: Int)

    // https://github.com/JakeWharton/Telecine/blob/72c1b15dadf3373a39d915b50984bdda09669ee4/telecine/src/main/java/com/jakewharton/telecine/RecordingSession.java#L387-L413
    private fun calculateRecordingInfo(displayWidth: Int, displayHeight: Int,
                               displayDensity: Int, isLandscapeDevice: Boolean, cameraWidth: Int, cameraHeight: Int,
                               cameraFrameRate: Int, sizePercentage: Int): RecordingInfo {
        var displayWidth = displayWidth
        var displayHeight = displayHeight
        // Scale the display size before any maximum size calculations.
        displayWidth = displayWidth * sizePercentage / 100
        displayHeight = displayHeight * sizePercentage / 100

        if (cameraWidth == -1 && cameraHeight == -1) {
            // No cameras. Fall back to the display size.
            return RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity)
        }

        var frameWidth = if (isLandscapeDevice) cameraWidth else cameraHeight
        var frameHeight = if (isLandscapeDevice) cameraHeight else cameraWidth
        if (frameWidth >= displayWidth && frameHeight >= displayHeight) {
            // Frame can hold the entire display. Use exact values.
            return RecordingInfo(displayWidth, displayHeight, cameraFrameRate, displayDensity)
        }

        // Calculate new width or height to preserve aspect ratio.
        if (isLandscapeDevice) {
            frameWidth = displayWidth * frameHeight / displayHeight
        } else {
            frameHeight = displayHeight * frameWidth / displayWidth
        }
        return RecordingInfo(frameWidth, frameHeight, cameraFrameRate, displayDensity)
    }

    // https://github.com/JakeWharton/Telecine/blob/72c1b15dadf3373a39d915b50984bdda09669ee4/telecine/src/main/java/com/jakewharton/telecine/RecordingSession.java#L177-L202
    fun getRecordingInfo(): RecordingInfo {
        val displayMetrics = DefaultDisplay.realMetrics
        val displayWidth = displayMetrics.widthPixels
        val displayHeight = displayMetrics.heightPixels
        val displayDensity = displayMetrics.densityDpi

        // Get the best camera profile available. We assume MediaRecorder supports the highest.
        val camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        val cameraWidth = camcorderProfile.videoFrameWidth
        val cameraHeight = camcorderProfile.videoFrameHeight
        val cameraFrameRate = camcorderProfile.videoFrameRate

        return calculateRecordingInfo(displayWidth, displayHeight, displayDensity, MainApplication.appContext.resources.configuration.landscape,
                cameraWidth, cameraHeight, cameraFrameRate, 100)
    }

}
