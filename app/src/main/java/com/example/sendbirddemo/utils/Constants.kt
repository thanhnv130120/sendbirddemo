package com.example.sendbirddemo.utils

object Constants {

    enum class ValidateType {
        ValidateDone, EmptyUserName, EmptyEmail, EmptyPassword, EmptyMessage, InvalidPassword, EmptyUserId, EmptyNickName
    }

    const val GROUP_CHANNEL_LIMIT = 15
    const val GROUP_CHANNEL_KEY = "groupchannel"
}