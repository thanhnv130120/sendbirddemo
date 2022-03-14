package com.example.sendbirddemo.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPreferenceUtils private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun putStringValue(key: String?, value: String?) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value).apply()
    }

    fun getStringValue(key: String?): String? {
        return sharedPreferences.getString(key, "")
    }

    fun putBooleanValue(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value).apply()
    }

    fun getBooleanValue(key: String): Boolean {
        return sharedPreferences.getBoolean(key, false)
    }

    fun putIntValue(key: String?, value: Int) {
        val editor = sharedPreferences.edit()
        editor.putInt(key, value).apply()
    }

    fun getIntValue(key: String?): Int {
        return sharedPreferences.getInt(key, 0)
    }

    fun getCorrectionValue(key: String?): Int {
        return sharedPreferences.getInt(key, 1)
    }

    fun putLongValue(key: String?, value: Long) {
        val editor = sharedPreferences.edit()
        editor.putLong(key, value).apply()
    }

    fun getLongValue(key: String?): Long {
        return sharedPreferences.getLong(key, 0L)
    }

    fun setUserId(userId: String?) {
        putStringValue(PREFERENCE_KEY_USER_ID, userId)
    }

    fun getUserId(): String? {
        return getStringValue(PREFERENCE_KEY_USER_ID)
    }

    fun setNickname(nickname: String?) {
        putStringValue(PREFERENCE_KEY_NICKNAME, nickname)
    }

    fun getNickname(): String? {
        return getStringValue(PREFERENCE_KEY_NICKNAME)
    }

    fun setConnected(value: Boolean) {
        putBooleanValue(PREFERENCE_KEY_CONNECTED, value)
    }

    fun getConnected(): Boolean {
        return getBooleanValue(PREFERENCE_KEY_CONNECTED)
    }


    companion object {

        const val PREFERENCE_NAME = "DemoSendBird_pref"
        const val PREFERENCE_KEY_USER_ID = "userId"
        const val PREFERENCE_KEY_NICKNAME = "nickname"
        const val PREFERENCE_KEY_CONNECTED = "connected"

        private var instance: SharedPreferenceUtils? = null
        fun getInstance(context: Context): SharedPreferenceUtils? {
            if (instance == null) {
                instance = SharedPreferenceUtils(context)
            }
            return instance
        }
    }
}