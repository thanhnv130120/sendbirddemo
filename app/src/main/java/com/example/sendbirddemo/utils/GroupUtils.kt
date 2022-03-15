package com.example.sendbirddemo.utils

import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelListQuery
import com.sendbird.android.SendBirdException

class GroupUtils(val onGroupListener: OnGroupListener) {

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
    fun createGroupChannel(
        userIds: List<String>,
        distinct: Boolean
    ) {
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

    interface OnGroupListener {
        fun onCreateGroupSuccess(groupUrl: String)
        fun onInviteMembers()
        fun onLeaveGroupSuccess()
    }

}