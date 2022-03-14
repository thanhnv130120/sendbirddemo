package com.example.sendbirddemo.utils

import android.content.Context
import com.sendbird.android.*
import com.sendbird.syncmanager.SendBirdSyncManager
import com.sendbird.syncmanager.handler.CompletionHandler

object SyncManagerUtils {

    fun setup(context: Context, userId: String, handler: CompletionHandler) {
        val options = SendBirdSyncManager.Options.Builder()
            .setMessageResendPolicy(SendBirdSyncManager.MessageResendPolicy.AUTOMATIC)
            .setAutomaticMessageResendRetryCount(5)
            .build()
        SendBirdSyncManager.setup(context, userId, options, handler)
    }

    /**
     *  It returns the index that targetChannel should be inserted to the given channel list.
     */
    open fun findIndexOfChannel(
        channels: List<GroupChannel>,
        targetChannel: GroupChannel,
        order: GroupChannelListQuery.Order
    ): Int {
        if (channels.isEmpty()) {
            return 0
        }
        val index = channels.size
        for (i in channels.indices) {
            val c = channels[i]
            if (c.url == targetChannel.url) {
                return i
            }
            if (GroupChannel.compareTo(targetChannel, c, order) < 0) {
                return i
            }
        }
        return index
    }

    /**
     * It returns the index of targetChannel in the given channel list.
     * If not exists, it will return -1.
     */
    fun getIndexOfChannel(channels: List<GroupChannel>, targetChannel: GroupChannel): Int {
        for (i in channels.indices) {
            if (channels[i].url == targetChannel.url) {
                return i
            }
        }
        return -1
    }

    /**
     * It returns the index that targetMessage should be inserted to the given message list.
     * If isLatestFirst is set to true, latest message's index will be zero.
     * If isLatestFirst is set to true, oldest message's index will be zero.
     *
     * @param messages `BaseMessage` list associated with view.
     * @param newMessage New `BaseMessage` to be inserted to existing message list.
     * @return Index of new message have to be inserted.
     */
    fun findIndexOfMessage(messages: List<BaseMessage>, newMessage: BaseMessage): Int {
        if (messages.isEmpty()) {
            return 0
        }
        if (messages[0].createdAt < newMessage.createdAt) {
            return 0
        }
        for (i in 0 until messages.size - 1) {
            val m1 = messages[i]
            val m2 = messages[i + 1]
            if (m1.createdAt > newMessage.createdAt && newMessage.createdAt > m2.createdAt) {
                return i + 1
            }
        }
        return messages.size
    }

    /**
     * It returns the index of targetMessage in the given message list.
     * If not exists, it will return -1.
     *
     * @param messages `BaseMessage` list associated with view.
     * @param targetMessage Target `BaseMessage` to find out.
     * @return Index of target message in the given message list.
     */
    fun getIndexOfMessage(messages: List<BaseMessage>, targetMessage: BaseMessage): Int {
        for (i in messages.indices) {
            val msgId1 = messages[i].messageId
            val msgId2 = targetMessage.messageId
            if (msgId1 == msgId2) {
                if (msgId1 == 0L) {
                    if (getRequestId(messages[i]) == getRequestId(targetMessage)) {
                        return i
                    }
                } else {
                    return i
                }
            }
        }
        return -1
    }

    private fun getRequestId(message: BaseMessage): String {
        if (message is AdminMessage) {
            return ""
        }
        if (message is UserMessage) {
            return message.requestId
        }
        return if (message is FileMessage) {
            message.requestId
        } else ""
    }

    fun getMyUserId(context: Context): String? {
        return if (SendBird.getCurrentUser() == null) {
            SharedPreferenceUtils.getInstance(context)?.getUserId()
        } else SendBird.getCurrentUser().userId
    }
}