package com.example.sendbirddemo.utils

import android.annotation.SuppressLint
import com.sendbird.android.*
import com.sendbird.syncmanager.MessageCollection
import com.sendbird.syncmanager.MessageFilter
import com.sendbird.syncmanager.handler.MessageCollectionCreateHandler
import com.sendbird.syncmanager.handler.MessageCollectionHandler

class ChatUtils(val onGroupListener: OnGroupListener) {

    private var mMessageFilter = MessageFilter(BaseChannel.MessageTypeFilter.ALL, null, null)
    private var mMessageCollection: MessageCollection? = null
    private var mChannel: GroupChannel? = null

    /**
     * Creates a new Group Channel.
     *
     * Note that if you have not included empty channels in your GroupChannelListQuery,
     * the channel will not be shown in the user's channel list until at least one message
     * has been sent inside.
     *
     * @param userIds   The users to be members of the new channel.
     * @param distinct  Whether the channel is unique for the selected members.
     *                  If you attempt to create another Distinct channel with the same members,
     *                  the existing channel instance will be returned.
     */
    fun createGroupChannel(userIds: List<String>, distinct: Boolean) {
        GroupChannel.createChannelWithUserIds(
            userIds,
            distinct,
            object : GroupChannel.GroupChannelCreateHandler {
                override fun onResult(groupChannel: GroupChannel?, e: SendBirdException?) {
                    if (e != null) {
                        return
                    }
                    onGroupListener.onCreateGroupSuccess(groupChannel!!.url)
                }

            })
    }

    fun getListGroupChannel(): GroupChannelListQuery {
        val query = GroupChannel.createMyGroupChannelListQuery()
        query.limit = Constants.GROUP_CHANNEL_LIMIT
        return query
    }

    fun inviteSelectedMembers(groupUrl: String, userIds: List<String>) {
        GroupChannel.getChannel(groupUrl, object : GroupChannel.GroupChannelGetHandler {
            override fun onResult(groupChannel: GroupChannel?, e: SendBirdException?) {
                if (e != null) {
                    return
                }
                groupChannel?.inviteWithUserIds(
                    userIds,
                    object : GroupChannel.GroupChannelInviteHandler {
                        override fun onResult(e: SendBirdException?) {
                            if (e != null) {
                                return
                            }
                            onGroupListener.onInviteMembers()
                        }

                    })
            }

        })
    }

    fun leaveGroupChannel(groupUrl: String) {
        GroupChannel.getChannel(groupUrl, object : GroupChannel.GroupChannelGetHandler {
            override fun onResult(groupChannel: GroupChannel?, e: SendBirdException?) {
                if (e != null) {
                    return
                }
                groupChannel?.leave(object : GroupChannel.GroupChannelLeaveHandler {
                    override fun onResult(e: SendBirdException?) {
                        if (e != null) {
                            return
                        }
                        onGroupListener.onLeaveGroupSuccess()
                    }

                })
            }

        })
    }

    fun createMessageCollection(
        groupUrl: String,
        mLastRead: Long,
        mMessageCollectionHandler: MessageCollectionHandler
    ) {
        GroupChannel.getChannel(groupUrl, object : GroupChannel.GroupChannelGetHandler {
            override fun onResult(groupChannel: GroupChannel?, e: SendBirdException?) {
                if (e != null) {
                    MessageCollection.create(
                        groupUrl,
                        mMessageFilter,
                        mLastRead,
                        object : MessageCollectionCreateHandler {
                            override fun onResult(
                                messageCollection: MessageCollection?,
                                e: SendBirdException?
                            ) {
                                if (e == null) {
                                    if (mMessageCollection != null) {
                                        mMessageCollection!!.remove()
                                    }
                                    mMessageCollection = messageCollection
                                    mMessageCollection?.setCollectionHandler(
                                        mMessageCollectionHandler
                                    )
                                    mChannel = mMessageCollection?.channel
                                    onGroupListener.onCreateMessageCollection(mChannel!!)
                                }
                            }

                        })
                } else {
                    if (mMessageCollection != null) {
                        mMessageCollection!!.remove()
                    }
                    mMessageCollection = MessageCollection(groupChannel, mMessageFilter, mLastRead)
                    mMessageCollection!!.setCollectionHandler(mMessageCollectionHandler)
                    mChannel = groupChannel
                    onGroupListener.onCreateMessageCollection(mChannel!!)
                }
            }

        })
    }

    @SuppressLint("StaticFieldLeak")
    fun sendUserMessageWithUrl(text: String, url: String) {
        if (mChannel == null) {
            return
        }
        object : WebUtils.UrlPreviewAsyncTask() {
            override fun onPostExecute(info: UrlPreviewInfo?) {
                if (mChannel == null) {
                    return
                }
                var tempUserMessage: UserMessage? = null
                val handler =
                    BaseChannel.SendUserMessageHandler { userMessage, e ->
                        if (e != null) {

                        }
                        mMessageCollection?.handleSendMessageResponse(userMessage, e)
                    }
                tempUserMessage = try {
                    val jsonString = info?.toJsonString()
                    mChannel!!.sendUserMessage(text, jsonString, "url_preview", handler)
                } catch (e: Exception) {
                    // Sending a message without URL preview information.
                    mChannel!!.sendUserMessage(text, handler)
                }
            }
        }.execute(url)
    }

    fun sendUserMessage(text: String) {
        if (mChannel == null) {
            return
        }
        var urls: List<String> = WebUtils.extractUrls(text)
        if (urls.isNotEmpty()) {
            sendUserMessageWithUrl(text, urls[0])
            return
        }

    }


    interface OnGroupListener {
        fun onCreateGroupSuccess(groupUrl: String)
        fun onInviteMembers()
        fun onLeaveGroupSuccess()
        fun onCreateMessageCollection(mChannel: GroupChannel)
    }

}