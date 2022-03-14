package com.example.sendbirddemo.utils

import android.content.Context
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.DisconnectHandler
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import com.sendbird.syncmanager.SendBirdSyncManager

class ConnectionUtils {

    fun connectToSendBird(
        context: Context,
        userID: String,
        nickname: String,
        handler: SendBird.ConnectHandler?
    ) {
        SendBird.connect(userID, object : SendBird.ConnectHandler {
            override fun onConnected(user: User?, e: SendBirdException?) {
                if (e != null) {
                    if (handler != null) {
                        handler.onConnected(user, e)
                        return
                    }
                }
                SyncManagerUtils.setup(
                    context, userID
                ) { SendBirdSyncManager.getInstance().resumeSync() }
                SendBird.updateCurrentUserInfo(
                    nickname,
                    null
                ) { p0 ->
                    if (p0 == null) {
                        SharedPreferenceUtils.getInstance(context)?.setNickname(nickname)
                    }
                }
                handler?.onConnected(user, e)
            }

        })
    }

    fun disconnect(handler: DisconnectHandler?) {
        SendBird.disconnect {
            SendBirdSyncManager.getInstance().pauseSync()
            handler?.onDisconnected()
        }
    }
}