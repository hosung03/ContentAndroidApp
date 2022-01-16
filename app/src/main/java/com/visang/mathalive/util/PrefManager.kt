package com.visang.mathalive.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson

class PrefManager {
    companion object {
        val PREFS_STRING = "mathalive"
        val PREFS_ENABLE_PUSH = "enable_push"
        val PREFS_PUSH_TOKEN = "push_token"

        /**
         * PUSH 알림 ON / OFF 저장
         *
         * @param context
         * @param enable
         */
        fun setPushEnable(context: Context, enable: Boolean) {
            val Preferences: SharedPreferences = context.getSharedPreferences(PREFS_STRING, Context.MODE_PRIVATE)
            Preferences.edit().putBoolean(PREFS_ENABLE_PUSH, enable).apply()
        }

        /**
         * PUSH 알림 ON / OFF 확인
         *
         * @param context
         */
        fun isPushEnable(context: Context): Boolean {
            val Preferences: SharedPreferences = context.getSharedPreferences(PREFS_STRING, Context.MODE_PRIVATE)
            return Preferences.getBoolean(PREFS_ENABLE_PUSH, false)
        }

        /**
         * PUSH 알림을 위한 Device Token 저장
         *
         * @param context
         * @param token
         */
        fun savePushToken(context: Context, token: String) {
            val Preferences: SharedPreferences = context.getSharedPreferences(PREFS_STRING, Context.MODE_PRIVATE)
            Preferences.edit().putString(PREFS_PUSH_TOKEN, token).apply()
        }

        /**
         * PUSH 알림을 위한 Device Token 삭제
         *
         * @param context
         */
        fun removePushToken(context: Context) {
            val Preferences: SharedPreferences = context.getSharedPreferences(PREFS_STRING, Context.MODE_PRIVATE)
            Preferences.edit().remove(PREFS_PUSH_TOKEN).apply()
        }

        /**
         * PUSH 알림을 위한 Device Token 반환
         *
         * @param context
         */
        fun getPushToken(context: Context): String? {
            val Preferences: SharedPreferences = context.getSharedPreferences(PREFS_STRING, Context.MODE_PRIVATE)
            return Preferences.getString(PREFS_PUSH_TOKEN, "")
        }
    }
}