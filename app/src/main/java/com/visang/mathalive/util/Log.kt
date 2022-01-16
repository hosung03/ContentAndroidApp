package com.visang.mathalive.util

class Log {

    companion object {

        fun d(tag: String, msg: String) {
            Logger.d("$tag: $msg")
        }

        fun w(tag: String, msg: String) {
            Logger.w("$tag: $msg")
        }

        fun e(tag: String, msg: String) {
            Logger.e("$tag: $msg")
        }

        const val DEBUG = android.util.Log.DEBUG
        const val WARN = android.util.Log.WARN
        const val ERROR = android.util.Log.ERROR

    }
}
