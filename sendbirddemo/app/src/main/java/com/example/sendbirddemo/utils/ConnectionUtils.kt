package com.example.sendbirddemo.utils

import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.DisconnectHandler
import com.sendbird.syncmanager.SendBirdSyncManager

class ConnectionUtils(val onConnectionListener: OnConnectionListener) {

    fun connectToSendBird(userID: String, nickname: String) {
        SendBird.connect(userID) { user, e ->
            if (e != null) {
                onConnectionListener.onConnectFailed()
            } else {
                SendBird.updateCurrentUserInfo(nickname, null) { e ->
                    if (e != null) {
                        onConnectionListener.onConnectFailed()
                    }
                    onConnectionListener.onConnectSuccess()
                }
            }
        }
    }

    fun disconnect(handler: DisconnectHandler?) {
        SendBird.disconnect {
            SendBirdSyncManager.getInstance().pauseSync()
            handler?.onDisconnected()
        }
    }

    interface OnConnectionListener {
        fun onConnectFailed()
        fun onConnectSuccess()
    }
}