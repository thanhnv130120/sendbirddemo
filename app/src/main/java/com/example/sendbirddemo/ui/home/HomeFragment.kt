package com.example.sendbirddemo.ui.home

import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentHomeBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.home.adapter.GroupChannelListAdapter
import com.sendbird.android.*
import com.sendbird.android.SendBird.ChannelHandler
import com.sendbird.syncmanager.ChannelCollection
import com.sendbird.syncmanager.ChannelEventAction
import com.sendbird.syncmanager.handler.ChannelCollectionHandler

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private var mGroupChannelListAdapter: GroupChannelListAdapter? = null
    private var mChannelCollection: ChannelCollection? = null

    override fun getLayoutID() = R.layout.fragment_home

    override fun initView() {
        mGroupChannelListAdapter = GroupChannelListAdapter()
        binding!!.rcGroupChannel.apply {
            setHasFixedSize(true)
            adapter = mGroupChannelListAdapter
        }

        // If user scrolls to bottom of the list, loads more channels.
        binding!!.rcGroupChannel.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (LinearLayoutManager(requireContext()).findLastVisibleItemPosition() == mGroupChannelListAdapter!!.itemCount - 1) {
                        if (mChannelCollection != null) {
                            mChannelCollection!!.fetch {
                                if (binding!!.mSwipeRefresh.isRefreshing) {
                                    binding!!.mSwipeRefresh.isRefreshing = false
                                }
                            }
                        }
                    }
                }
            }
        })

        binding!!.mSwipeRefresh.setOnRefreshListener {
            binding!!.mSwipeRefresh.isRefreshing = true
            refresh()
        }

        binding!!.fabAddGroupChannel.setOnClickListener {
            findNavController().navigate(R.id.action_global_selectUserFragment)
        }

        refresh()
    }

    override fun initViewModel() {

    }

    override fun onResume() {
        SendBird.addChannelHandler(
            CHANNEL_HANDLER_ID,
            object : ChannelHandler() {
                override fun onMessageReceived(
                    baseChannel: BaseChannel,
                    baseMessage: BaseMessage
                ) {
                }

                override fun onChannelChanged(channel: BaseChannel) {}
                override fun onTypingStatusUpdated(channel: GroupChannel) {
                    mGroupChannelListAdapter?.notifyDataSetChanged()
                }
            })
        super.onResume()
    }

    override fun onPause() {
        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID)
        super.onPause()
    }

    override fun onDestroy() {
        if (mChannelCollection != null) {
            mChannelCollection!!.setCollectionHandler(null)
            mChannelCollection!!.remove()
        }
        super.onDestroy()
    }

    private fun refresh() {
        if (mChannelCollection != null) {
            mChannelCollection?.remove()
        }

        mGroupChannelListAdapter?.clearMap()
        mGroupChannelListAdapter?.clearGroupChannelList()
        val query = GroupChannel.createMyGroupChannelListQuery()
        query.limit = CHANNEL_LIST_LIMIT
        mChannelCollection = ChannelCollection(query)
        mChannelCollection!!.setCollectionHandler(mGroupChannelCollectionHandler)
        mChannelCollection!!.fetch {
            if (binding!!.mSwipeRefresh.isRefreshing) {
                binding!!.mSwipeRefresh.isRefreshing = false
            }
        }
    }

    private var mGroupChannelCollectionHandler = object : ChannelCollectionHandler {
        override fun onChannelEvent(
            channelCollection: ChannelCollection?,
            groupChannelList: MutableList<GroupChannel>?,
            channelEventAction: ChannelEventAction?
        ) {
            if (activity == null) {
                return
            }

            activity!!.runOnUiThread {
                if (binding!!.mSwipeRefresh.isRefreshing) {
                    binding!!.mSwipeRefresh.isRefreshing = false
                }
                when (channelEventAction) {
                    ChannelEventAction.INSERT -> {
                        mGroupChannelListAdapter?.clearMap()
                        mGroupChannelListAdapter?.insertChannels(
                            groupChannelList!!,
                            channelCollection?.query!!.order
                        )
                    }
                    ChannelEventAction.UPDATE -> {
                        mGroupChannelListAdapter?.clearMap()
                        mGroupChannelListAdapter?.updateGroupChannels(groupChannelList!!)
                    }
                    ChannelEventAction.MOVE -> {
                        mGroupChannelListAdapter?.clearMap()
                        mGroupChannelListAdapter?.moveGroupChannels(
                            groupChannelList!!,
                            channelCollection?.query!!.order
                        )
                    }
                    ChannelEventAction.REMOVE -> {
                        mGroupChannelListAdapter?.clearMap()
                        mGroupChannelListAdapter?.removeGroupChannels(groupChannelList!!)
                    }
                    ChannelEventAction.CLEAR -> {
                        mGroupChannelListAdapter?.clearMap()
                        mGroupChannelListAdapter?.clearGroupChannelList()
                    }
                }
            }
        }

    }

    override fun getConnectionHandlerId(): String {
        return "CONNECTION_HANDLER_MAIN_ACTIVITY"
    }

    companion object {
        const val CHANNEL_LIST_LIMIT = 15
        private const val CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_LIST"
    }
}