package com.example.sendbirddemo.utils

import com.sendbird.android.GroupChannel

object Utils {

    fun getGroupChannelTitle(channel: GroupChannel): String? {
        val members = channel.members
        val myUserId: String = getMyUserId()
        return if (members.size < 2 || myUserId == null) {
            "No Members"
        } else if (members.size == 2) {
            val names = StringBuffer()
            for (member in members) {
                if (member.userId == myUserId) {
                    continue
                }
                names.append(", " + member.nickname)
            }
            names.delete(0, 2).toString()
        } else {
            var count = 0
            val names = StringBuffer()
            for (member in members) {
                if (member.userId == myUserId) {
                    continue
                }
                count++
                names.append(", " + member.nickname)
                if (count >= 10) {
                    break
                }
            }
            names.delete(0, 2).toString()
        }
    }
}