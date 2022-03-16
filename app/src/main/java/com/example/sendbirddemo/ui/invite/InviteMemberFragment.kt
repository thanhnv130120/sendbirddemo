package com.example.sendbirddemo.ui.invite

import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentInviteMemberBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.selectuser.adapter.SelectableUserAdapter
import com.example.sendbirddemo.utils.GroupUtils
import com.sendbird.android.ApplicationUserListQuery
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery

class InviteMemberFragment : BaseFragment<FragmentInviteMemberBinding>(), View.OnClickListener {

    private val args: InviteMemberFragmentArgs by navArgs()
    private val mSelectableUserAdapter by lazy {
        SelectableUserAdapter()
    }
    private var mListUserQuery: ApplicationUserListQuery? = null
    private var mSelectedIds = mutableListOf<String>()
    private val mGroupUtils: GroupUtils by lazy {
        GroupUtils(object : GroupUtils.OnGroupListener{
            override fun onCreateGroupSuccess(groupUrl: String) {

            }

            override fun onInviteMembers() {
                findNavController().navigateUp()
            }

            override fun onLeaveGroupSuccess() {

            }

        })
    }
    private var mLayoutManager: LinearLayoutManager? = null

    override fun getLayoutID() = R.layout.fragment_invite_member

    override fun initView() {
        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            setUpRecyclerView()
            loadInitialUserList(15)
        }

        binding!!.btnInvite.isEnabled = false
        binding!!.btnInvite.setOnClickListener(this)

        mSelectableUserAdapter.setItemCheckedChangeListener(object :
            SelectableUserAdapter.OnItemCheckedChangeListener {
            override fun OnItemChecked(user: User?, checked: Boolean) {
                if (checked) {
                    mSelectedIds.add(user!!.userId)
                } else {
                    mSelectedIds.remove(user!!.userId)
                }
                binding!!.btnInvite.isEnabled = mSelectedIds.isNotEmpty()
            }

        })
    }

    override fun initViewModel() {

    }

    override fun onClick(p0: View?) {
        if (p0 == binding!!.btnInvite) {
            if (mSelectedIds.isNotEmpty()){
                mGroupUtils.inviteSelectedMembers(args.groupChannelUrl, mSelectedIds)
            }
        }
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(activity)
        binding!!.rcInviteMember.apply {
            layoutManager = mLayoutManager
            adapter = mSelectableUserAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (mLayoutManager!!.findLastVisibleItemPosition() == mSelectableUserAdapter.itemCount - 1) {
                        loadNextUserList(10)
                    }
                }
            })
        }
    }

    /**
     * Replaces current user list with new list.
     * Should be used only on initial load.
     */
    private fun loadInitialUserList(size: Int) {
        mListUserQuery = SendBird.createApplicationUserListQuery()
        mListUserQuery?.setLimit(size)
        mListUserQuery?.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }
            mSelectableUserAdapter.setUserList(list)
        })
    }

    private fun loadNextUserList(size: Int) {
        mListUserQuery?.setLimit(size)
        mListUserQuery?.next(UserListQuery.UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }
            for (user in list) {
                mSelectableUserAdapter.addLast(user)
            }
        })
    }

}