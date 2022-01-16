package com.visang.mathalive.media

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import com.visang.mathalive.MainApplication
import com.visang.mathalive.R
import com.visang.mathalive.util.Log
import com.visang.mathalive.util.Logger
import com.visang.mathalive.util.PathUtils
import com.visang.mathalive.util.Utils
import java.io.File

/**
 * 전체 화면 동영상 녹화
 */
class ScreenToVideoRecorder(mediaProjection: MediaProjection) {

    val mediaRecorder = MediaRecorder()
    var filePath: String = PathUtils.getOutputFilePath("mp4")
    private var mHandler: Handler? = null

    init {
        // start record handling thread
        object : Thread() {
            override fun run() {
                Looper.prepare()
                mHandler = Handler()
                Looper.loop()
            }
        }.start()

        start(mediaProjection)
    }

    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    private fun start(mediaProjection: MediaProjection) {
        Logger.d("ScreenToVideoRecorder : ${filePath}")
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))

            val recordingInfo = MediaUtils.getRecordingInfo()
            mediaRecorder.setVideoFrameRate(recordingInfo.frameRate)
            mediaRecorder.setVideoSize(recordingInfo.width, recordingInfo.height)
            //mediaRecorder.setVideoSize(720, 480)
            //mediaRecorder.setVideoEncodingBitRate(Constants.VIDEO_RECORD_BIT_RATE)
            //mediaRecorder.setVideoFrameRate(Constants.VIDEO_RECORD_FRAME_RATE)

            val sharedPref = PreferenceManager.getDefaultSharedPreferences(MainApplication.appContext)
            var bitrate = sharedPref.getString(Utils.getString(R.string.preference_video_bitrate), "2000")
            mediaRecorder.setVideoEncodingBitRate(Integer.parseInt(bitrate!!) * 1000)

//        val framerate = sharedPref.getString(Utils.getString(R.string.preference_video_framerate), "25")
//        mediaRecorder.setVideoFrameRate(Integer.parseInt(framerate))

            mediaRecorder.setOutputFile(filePath)
            mediaRecorder.prepare()
            virtualDisplay = mediaProjection.createVirtualDisplay("VirtualDisplay", recordingInfo.width, recordingInfo.height, recordingInfo.density, DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION, mediaRecorder.surface, null, mHandler)
            mediaRecorder.start()
            this.mediaProjection = mediaProjection
        } catch (ignored: java.lang.RuntimeException) {
            Log.d("", ignored.toString())
        }
    }

    fun stop(): String {

        mHandler?.post {
            mediaProjection?.stop()
            mediaProjection = null
        }

        try {
            mediaRecorder.stop()
        } catch (t: Throwable) {
        }
        mediaRecorder.release()

        virtualDisplay?.release()

        virtualDisplay = null

        return File(filePath).toURI().toURL().toString()
    }
}
