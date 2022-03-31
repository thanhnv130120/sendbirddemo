package com.example.sendbirddemo.ui.selectuser

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.utils.GroupUtils
import com.sendbird.android.ApplicationUserListQuery
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery
import kotlinx.coroutines.launch

class SelectUserViewModel(private val application: Application) : ViewModel() {

    private val mSelectedIds = mutableListOf<String>()
    private var mGroupUtils: GroupUtils? = null
    private var mListUserQuery: ApplicationUserListQuery? = null
    val mGroupCreatedLiveData = MutableLiveData<DataResponse<String>>()
    val mInitialUserListLiveData = MutableLiveData<DataResponse<MutableList<User>>>()
    val mNextUserListLiveData = MutableLiveData<DataResponse<User>>()

    init {
        mGroupCreatedLiveData.value = DataResponse.DataEmptyResponse()
        mInitialUserListLiveData.value = DataResponse.DataEmptyResponse()
        mNextUserListLiveData.value = DataResponse.DataEmptyResponse()
        mGroupUtils = GroupUtils(object : GroupUtils.OnGroupListener {
            override fun onCreateGroupSuccess(groupUrl: String) {
                mGroupCreatedLiveData.value = DataResponse.DataSuccessResponse(groupUrl)
            }

            override fun onInviteMembers() {

            }

            override fun onLeaveGroupSuccess() {

            }

        })
    }

    fun onPlusId(user: User?) {
        mSelectedIds.add(user!!.userId)
    }

    fun onMinusId(user: User?) {
        mSelectedIds.remove(user!!.userId)
    }

    fun onCreateGroup() {
        if (!mSelectedIds.isNullOrEmpty()) {
            mGroupUtils!!.createGroupChannel(mSelectedIds, false)
        }
    }

    fun onLoadInitialUserList() {
        viewModelScope.launch {
            mListUserQuery = SendBird.createApplicationUserListQuery()
            mListUserQuery?.setLimit(15)
            mListUserQuery?.next(UserListQuery.UserListQueryResultHandler { list, e ->
                if (e != null) {
                    mInitialUserListLiveData.value = DataResponse.DataErrorResponse()
                    return@UserListQueryResultHandler
                }
                mInitialUserListLiveData.value = DataResponse.DataSuccessResponse(list)
            })
        }
    }

    fun onLoadNextUserList() {
        viewModelScope.launch {
            mListUserQuery?.setLimit(15)
            mListUserQuery?.next(UserListQuery.UserListQueryResultHandler { list, e ->
                if (e != null) {
                    mNextUserListLiveData.value = DataResponse.DataErrorResponse()
                    return@UserListQueryResultHandler
                }
                for (user in list) {
                    mNextUserListLiveData.value = DataResponse.DataSuccessResponse(user)
                }
            })
        }
    }

    class Factory(private val application: Application) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SelectUserViewModel::class.java)) {
                return SelectUserViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}