package com.example.sendbirddemo.ui.login

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.utils.ConnectionUtils
import com.example.sendbirddemo.utils.Constants
import com.example.sendbirddemo.utils.SharedPreferenceUtils
import com.example.sendbirddemo.utils.SyncManagerUtils
import com.sendbird.android.SendBird
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import com.sendbird.syncmanager.handler.CompletionHandler

class LoginViewModel(private val application: Application) : ViewModel() {

    private var mConnectionUtils: ConnectionUtils? = null
    var userIdLiveData = MutableLiveData<String>()
    var nickNameLiveData = MutableLiveData<String>()
    var validateLiveData = MutableLiveData<DataResponse<Constants.ValidateType>>()
    var onLoginLiveData = MutableLiveData<DataResponse<Boolean>>()

    init {
        mConnectionUtils = ConnectionUtils()
        validateLiveData.value = DataResponse.DataEmptyResponse()
        onLoginLiveData.value = DataResponse.DataEmptyResponse()
    }

    fun onLogin() {
        val validateType = validate()
        if (validateType == Constants.ValidateType.ValidateDone) {
            val userId = userIdLiveData.value!!.replace("\\s".toRegex(), "")
            SharedPreferenceUtils.getInstance(application)?.setUserId(userId)
            SharedPreferenceUtils.getInstance(application)?.setNickname(nickNameLiveData.value)
            mConnectionUtils!!.connect(
                application,
                userId,
                nickNameLiveData.value!!,
                object : SendBird.ConnectHandler {
                    override fun onConnected(user: User?, e: SendBirdException?) {
                        if (e == null) {
                            SyncManagerUtils.setup(application,
                                SharedPreferenceUtils.getInstance(application)?.getUserId()!!,
                                CompletionHandler { e ->
                                    if (e != null) {
                                        onLoginLiveData.value =
                                            DataResponse.DataSuccessResponse(false)
                                        return@CompletionHandler
                                    }
                                    SharedPreferenceUtils.getInstance(application)
                                        ?.setConnected(true)
                                    onLoginLiveData.value = DataResponse.DataSuccessResponse(true)
                                })
                        } else {
                            onLoginLiveData.value = DataResponse.DataSuccessResponse(false)
                            SharedPreferenceUtils.getInstance(application)?.setConnected(false)
                        }
                    }

                }
            )
        }
        validateLiveData.value = DataResponse.DataSuccessResponse(validateType)
    }

    private fun validate(): Constants.ValidateType {
        return when {
            userIdLiveData.value == null -> {
                Constants.ValidateType.EmptyUserName
            }
            nickNameLiveData.value == null -> {
                Constants.ValidateType.EmptyPassword
            }
            else -> Constants.ValidateType.ValidateDone
        }
    }

    class Factory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}