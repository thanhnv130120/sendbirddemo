package com.example.sendbirddemo.ui.member

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.utils.SyncManagerUtils
import com.sendbird.android.Member
import kotlinx.coroutines.launch

class MemberListViewModel(private val application: Application) : ViewModel() {

    var mMemberListLiveData = MutableLiveData<DataResponse<MutableList<Member>>>()

    init {
        mMemberListLiveData.value = DataResponse.DataEmptyResponse()
    }

    fun setMemberList(memberList: MutableList<Member>) {
        viewModelScope.launch {
            val sortedUserList: MutableList<Member> = ArrayList()
            val myUserId: String? = SyncManagerUtils.getMyUserId(application)
            for (member in memberList) {
                if (member.userId == myUserId) {
                    sortedUserList.add(0, member)
                } else {
                    sortedUserList.add(member)
                }
            }
            mMemberListLiveData.value = DataResponse.DataSuccessResponse(memberList)
        }
    }

    class Factory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MemberListViewModel::class.java)) {
                return MemberListViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}