package com.example.sendbirddemo.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sendbirddemo.R
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.ConnectionHandler
import com.sendbird.syncmanager.SendBirdSyncManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
    }

}