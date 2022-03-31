package com.example.sendbirddemo.ui.splash

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.utils.ConnectionUtils
import com.example.sendbirddemo.utils.SharedPreferenceUtils

class SplashViewModel(private val application: Application) : ViewModel() {

    private var mConnectionUtils: ConnectionUtils? = null
    var mCheckLoginLiveData = MutableLiveData<DataResponse<Boolean>>()

    init {
        mConnectionUtils = ConnectionUtils()
        mCheckLoginLiveData.value = DataResponse.DataEmptyResponse()
    }

    fun onCheckLogin() {
        if (mConnectionUtils!!.isLogin(application) && SharedPreferenceUtils.getInstance(application)
                ?.getUserId() != null
        ) {
            mConnectionUtils!!.setUpSyncManager(
                application,
                object : ConnectionUtils.OnSetupSyncManager {
                    override fun onSetupFailed() {
                        mCheckLoginLiveData.value = DataResponse.DataSuccessResponse(false)
                    }

                    override fun onSetupSucceed() {
                        mCheckLoginLiveData.value = DataResponse.DataSuccessResponse(true)
                    }

                })
        } else {
            mCheckLoginLiveData.value = DataResponse.DataSuccessResponse(false)
        }
    }

    class Factory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
                return SplashViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

}