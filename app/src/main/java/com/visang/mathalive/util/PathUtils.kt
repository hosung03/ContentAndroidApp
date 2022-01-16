package com.visang.mathalive.util

import android.os.Environment
import android.os.SystemClock
import com.visang.mathalive.MainApplication
import java.io.File
import kotlin.math.min

/**
 * PathUtils
 *
 * 경로 관련 함수들 모음.
 */
object PathUtils {

    private lateinit var baseDir: File
    private val webBaseUrl = "http://127.0.0.1:${Constants.LOCAL_WEB_SERVER_PORT}/"

    /**
     * 초기화 함수
     */
    fun initialize() {
        baseDir = File(MainApplication.appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "MATHALIVE")

        if (!baseDir.exists()) {
            baseDir.mkdir()
        }

        val externalFilesDirs = MainApplication.appContext.getExternalFilesDirs(null).filterNotNull()
        val externalFileDir = externalFilesDirs[min(externalFilesDirs.size - 1, 1)]
    }

    /**
     * Local web server의 홈 디렉토리 리턴.
     */
    fun getLocalWebServerHome() = baseDir

    private fun getOutputFile(fileName: String) =
            File("${baseDir.path}${File.separator}$fileName")

    /**
     * 카메라 미디어 (사진, 영상) 등의 데이터를 저장할 파일 가져오기.
     * @param ext 저장할 파일의 확장자 (jpg, mp4, mp3 등).
     * @return 미디어 저장에 사용할 파일.
     */
    fun getOutputFilePath(ext: String): String =
            getOutputFile("DAT_${System.currentTimeMillis()}_${SystemClock.elapsedRealtimeNanos()}.$ext").absolutePath

    fun getConfigFilePath(): String =
            File(MainApplication.appContext.getExternalFilesDir(""), "config.txt").absolutePath

    /**
     * Local web server url을 file 경로로 변환.
     */
    fun webToFilePath(path: String) = path.replace(webBaseUrl, baseDir.toURI().toURL().toString())

    /**
     * File 경로를 local web serer url로 변환.
     */
    fun fileToWebUrl(path: String) = path.replace(baseDir.toURI().toURL().toString(), webBaseUrl)
}
