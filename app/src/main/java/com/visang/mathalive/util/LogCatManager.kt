package com.visang.mathalive.util

import kotlin.concurrent.thread

class LogCatManager {

    private val logLengthLimit = 64 * 1024

    private val logList = mutableListOf<String>()
    private var logLength = 0

    init {
        thread(true) {
            Log.d("mathalive-logcat", "Starting LogCatManager")

            var count = 0

            try {
                Runtime.getRuntime().exec("logcat -b main").inputStream.bufferedReader().use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        addLog(line)
                        line = reader.readLine()
                        count++
                    }
                }
            } catch (e: Exception) {
                Log.d("mathalive-logcat", "exception ${e.message}")
            }

            Log.d("mathalive-logcat", "LogCatManager finished")
        }
    }

    @Synchronized private fun addLog(log: String) {
        logList.add(log)
        logLength += log.length

        while (logLength > logLengthLimit) {
            val removed = logList.removeAt(0)
            logLength -= removed.length
        }
    }

    @Synchronized fun getLog() = logList.toList()

}
