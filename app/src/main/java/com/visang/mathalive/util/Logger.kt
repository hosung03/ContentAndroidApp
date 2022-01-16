package com.visang.mathalive.util

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.LogcatAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.FileAppender
import com.visang.mathalive.MainApplication
import org.slf4j.LoggerFactory
import java.io.File

class Logger {

    companion object {

        fun d(message: String) {
            writeLog(Log.DEBUG, message)
        }

        fun w(message: String) {
            writeLog(Log.WARN, message)
        }

        fun e(message: String) {
            writeLog(Log.ERROR, message)
        }

        fun initialize() {
//            val lc = LoggerFactory.getILoggerFactory() as LoggerContext
//            lc.stop()
//
//            val logcatEncoder = PatternLayoutEncoder()
//            logcatEncoder.context = lc
//            logcatEncoder.pattern = "[%thread] %msg%n"
//            logcatEncoder.start()
//
//            val logcatAppender = LogcatAppender()
//            logcatAppender.context = lc
//            logcatAppender.encoder = logcatEncoder
//            logcatAppender.start()
//
//            val fileEncoder = PatternLayoutEncoder()
//            fileEncoder.context = lc
//            fileEncoder.pattern = "%d [%thread] - %msg%n"
//            fileEncoder.start()
//
//            val fileAppender = FileAppender<ILoggingEvent>()
//            fileAppender.context = lc
//            fileAppender.encoder = fileEncoder
//            fileAppender.isAppend = false
//            fileAppender.file = File(MainApplication.appContext.getExternalFilesDir(null), "mathalive.log").absolutePath
//            fileAppender.start()
//
//            val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger
//            root.addAppender(logcatAppender)
//            root.addAppender(fileAppender)

            d("Log initialized")
        }

        private fun writeLog(level: Int, message: String) {
            val logger = LoggerFactory.getLogger("MathALive-Log")

            when (level) {
                Log.ERROR -> logger.error(message)
                Log.WARN -> logger.warn(message)
                else -> logger.debug(message)
            }
        }

    }

}
