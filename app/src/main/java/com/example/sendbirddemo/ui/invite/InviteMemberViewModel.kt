package com.example.sendbirddemo.ui.invite

import android.app.Application
import androidx.lifecycle.*
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.utils.GroupUtils
import com.sendbird.android.ApplicationUserListQuery
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery
import kotlinx.coroutines.launch

class InviteMemberViewModel(private val application: Application, val groupChannelUrl: String) : ViewModel() {

    private var mGroupUtils: GroupUtils? = null
    private val mSelectedIds = mutableListOf<String>()
    private var mListUserQuery: ApplicationUserListQuery? = null
    val mInitialUserListLiveData = MutableLiveData<DataResponse<MutableList<User>>>()
    val mNextUserListLiveData = MutableLiveData<DataResponse<User>>()
    val mInviteMembersLiveData = MutableLiveData<DataResponse<Boolean>>()

    init {
        mInitialUserListLiveData.value = DataResponse.DataEmptyResponse()
        mNextUserListLiveData.value = DataResponse.DataEmptyResponse()
        mInviteMembersLiveData.value = DataResponse.DataEmptyResponse()
        mGroupUtils = GroupUtils(object : GroupUtils.OnGroupListener {
            override fun onCreateGroupSuccess(groupUrl: String) {

            }

            override fun onInviteMembers() {
                mInviteMembersLiveData.value = DataResponse.DataSuccessResponse(true)
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

    fun onInviteMembers() {
        if (!mSelectedIds.isNullOrEmpty()) {
            mGroupUtils!!.inviteSelectedMembers(groupChannelUrl, mSelectedIds)
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

    val isSelectedIdsEmpty = mSelectedIds.isNotEmpty()

    class Factory(private val application: Application, private val groupChannelUrl: String) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(InviteMemberViewModel::class.java)) {
                return InviteMemberViewModel(application, groupChannelUrl) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}