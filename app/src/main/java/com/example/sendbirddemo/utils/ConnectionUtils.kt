package com.example.sendbirddemo.utils

import android.content.Context
import com.sendbird.android.ConnectionManager
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.ConnectHandler
import com.sendbird.android.SendBird.DisconnectHandler
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import com.sendbird.syncmanager.SendBirdSyncManager
import com.sendbird.syncmanager.handler.CompletionHandler

class ConnectionUtils {

    fun isLogin(context: Context): Boolean {
        return SharedPreferenceUtils.getInstance(context)?.getConnected()!!
    }

    fun connect(
        context: Context,
        userID: String,
        nickname: String,
        handler: ConnectHandler?
    ) {
        SendBird.connect(userID, object : ConnectHandler {
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

    fun setUpSyncManager(context: Context, onSetupSyncManager: OnSetupSyncManager) {
        if (SharedPreferenceUtils.getInstance(context)?.getUserId() != null) {
            SyncManagerUtils.setup(context,
                SharedPreferenceUtils.getInstance(context)?.getUserId()!!,
                CompletionHandler { e ->
                    if (e != null) {
                        SendBirdSyncManager.getInstance().clearCache()
                        onSetupSyncManager.onSetupFailed()
                        return@CompletionHandler
                    }
                    onSetupSyncManager.onSetupSucceed()
                })
        }
    }

    fun disconnect(handler: DisconnectHandler?) {
        SendBird.disconnect {
            SendBirdSyncManager.getInstance().pauseSync()
            handler?.onDisconnected()
        }
    }

    interface OnSetupSyncManager {
        fun onSetupFailed()
        fun onSetupSucceed()
    }
}