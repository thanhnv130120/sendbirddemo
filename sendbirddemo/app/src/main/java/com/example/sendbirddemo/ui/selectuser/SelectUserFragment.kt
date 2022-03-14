package com.example.sendbirddemo.ui.selectuser

import android.content.Intent
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentSelectUserBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.selectuser.adapter.SelectableUserAdapter
import com.sendbird.android.*
import com.sendbird.android.GroupChannel.GroupChannelCreateHandler
import com.sendbird.android.UserListQuery.UserListQueryResultHandler

class SelectUserFragment : BaseFragment<FragmentSelectUserBinding>() {

    private var mListUserQuery: ApplicationUserListQuery? = null
    private val mSelectableUserAdapter by lazy {
        SelectableUserAdapter()
    }
    private var mSelectedIds = mutableListOf<String>()

    override fun getLayoutID() = R.layout.fragment_select_user

    override fun initView() {
        mSelectableUserAdapter.setItemCheckedChangeListener(object : SelectableUserAdapter.OnItemCheckedChangeListener{
            override fun OnItemChecked(user: User?, checked: Boolean) {
                if (checked){
                    mSelectedIds.add(user!!.userId)
                } else {
                    mSelectedIds.remove(user!!.userId)
                }
            }

        })

        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            setUpRecyclerView()
            loadInitialUserList(15)
        }

        binding!!.fabCreateGroup.setOnClickListener {
            if (mSelectedIds.isEmpty()){

            } else {
                createGroupChannel(mSelectedIds.toList())
            }
        }
    }

    override fun initViewModel() {

    }

    private fun setUpRecyclerView() {
        binding!!.rcSelectUser.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = mSelectableUserAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (LinearLayoutManager(activity).findLastVisibleItemPosition() == mSelectableUserAdapter.itemCount - 1) {
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
        mListUserQuery?.next(UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }
            mSelectableUserAdapter.setUserList(list)
        })
    }

    private fun loadNextUserList(size: Int) {
        mListUserQuery?.setLimit(size)
        mListUserQuery?.next(UserListQueryResultHandler { list, e ->
            if (e != null) {
                // Error!
                return@UserListQueryResultHandler
            }
            for (user in list) {
                mSelectableUserAdapter.addLast(user)
            }
        })
    }

    /**
     * Creates a new Group Channel.
     *
     * Note that if you have not included empty channels in your GroupChannelListQuery,
     * the channel will not be shown in the user's channel list until at least one message
     * has been sent inside.
     *
     * @param userIds   The users to be members of the new channel.
     * @param distinct  Whether the channel is unique for the selected members.
     * If you attempt to create another Distinct channel with the same members,
     * the existing channel instance will be returned.
     */
    private fun createGroupChannel(userIds: List<String>) {
        GroupChannel.createChannelWithUserIds(userIds, false,
            GroupChannelCreateHandler { groupChannel, e ->
                if (e != null) {
                    // Error!
                    return@GroupChannelCreateHandler
                }
                val action= SelectUserFragmentDirections.actionGlobalChatFragment().setGroupChannelUrl(groupChannel.url)
                findNavController().navigate(action)
            })
    }
}