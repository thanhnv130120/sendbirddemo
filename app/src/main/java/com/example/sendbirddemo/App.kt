package com.example.sendbirddemo

import android.app.Application
import com.sendbird.android.SendBird

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        SendBird.init(APP_ID, applicationContext)
    }

    companion object {
        const val APP_ID = "6B067DCE-D2C1-48DD-9651-26EE2F2A6680"
    }
}