package com.example.sendbirddemo.ui.home

import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentHomeBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.home.adapter.GroupChannelListAdapter
import com.example.sendbirddemo.utils.ConnectionUtils
import com.example.sendbirddemo.utils.GroupUtils
import com.example.sendbirddemo.utils.SharedPreferenceUtils
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.ChannelHandler
import com.sendbird.syncmanager.ChannelCollection
import com.sendbird.syncmanager.ChannelEventAction
import com.sendbird.syncmanager.handler.ChannelCollectionHandler

class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val mGroupChannelListAdapter by lazy {
        GroupChannelListAdapter()
    }
    private var mChannelCollection: ChannelCollection? = null
    private var mLayoutManager: LinearLayoutManager? = null
    private val connectionUtils: ConnectionUtils by lazy {
        ConnectionUtils()
    }
    private val groupUtils: GroupUtils by lazy {
        GroupUtils(object : GroupUtils.OnGroupListener {
            override fun onCreateGroupSuccess(groupUrl: String) {

            }

            override fun onInviteMembers() {

            }

            override fun onLeaveGroupSuccess() {
                refresh()
            }

        })
    }

    override fun getLayoutID() = R.layout.fragment_home

    override fun initView() {
        binding!!.mSwipeRefresh.setOnRefreshListener {
            binding!!.mSwipeRefresh.isRefreshing = true
            refresh()
        }

        binding!!.fabAddGroupChannel.setOnClickListener {
            findNavController().navigate(R.id.action_global_selectUserFragment)
        }

        setUpRecyclerView()
        setUpChannelListAdapter()
        refresh()
    }

    override fun initViewModel() {
        connect()
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
                    mGroupChannelListAdapter.notifyGroupChannelChange(channel)
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

    private fun connect() {
        if (SendBird.getConnectionState() != SendBird.ConnectionState.OPEN) {
            connectionUtils.connect(
                requireContext(),
                SharedPreferenceUtils.getInstance(requireContext())?.getUserId()!!,
                SharedPreferenceUtils.getInstance(requireContext())?.getNickname()!!
            ) { _, e ->
                if (e != null) {
                    e.printStackTrace()
                } else {

                }
            }
        } else {

        }
    }

    // Sets up recycler view
    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(context)
        binding!!.rcGroupChannel.apply {
            layoutManager = mLayoutManager
            adapter = mGroupChannelListAdapter
            addItemDecoration(
                DividerItemDecoration(
                    context,
                    DividerItemDecoration.VERTICAL
                )
            )
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (mLayoutManager!!.findLastVisibleItemPosition() == mGroupChannelListAdapter.itemCount.minus(
                                1
                            )
                        ) {
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
        }
    }

    // Sets up channel list adapter
    private fun setUpChannelListAdapter() {
        mGroupChannelListAdapter.setOnItemClickListener(object :
            GroupChannelListAdapter.OnItemClickListener {
            override fun onItemClick(channel: GroupChannel?) {
                val action = HomeFragmentDirections.actionGlobalChatFragment()
                action.groupChannelUrl = channel!!.url
                findNavController().navigate(action)
            }

            override fun onItemLongClick(channel: GroupChannel?) {
                showChannelOptionsDialog(channel!!)
            }
        })
    }

    /**
     * Displays a dialog listing channel-specific options.
     */
    private fun showChannelOptionsDialog(channel: GroupChannel) {
        val options: Array<String>
        val pushCurrentlyEnabled = channel.isPushEnabled
        options = if (pushCurrentlyEnabled) arrayOf(
            getString(R.string.leave_channel),
            getString(R.string.set_push_off)
        ) else arrayOf(getString(R.string.leave_channel), getString(R.string.set_push_on))
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(getString(R.string.channel_option))
            .setItems(options) { _, which ->
                if (which == 0) {
                    // Show a dialog to confirm that the user wants to leave the channel.
                    AlertDialog.Builder(requireActivity())
                        .setTitle(getString(R.string.request_leave_channel, channel.name))
                        .setPositiveButton(R.string.action_leave_channel) { _, _ ->
                            groupUtils.leaveGroupChannel(
                                channel.url
                            )
                        }
                        .setNegativeButton(getString(R.string.cancel), null)
                        .create().show()
                } else if (which == 1) {
//                    setChannelPushPreferences(channel, !pushCurrentlyEnabled)
                }
            }
        builder.create().show()
    }

    /**
     * Creates a new query to get the list of the user's Group Channels,
     * then replaces the existing dataset.
     *
     */
    private fun refresh() {
        if (mChannelCollection != null) {
            mChannelCollection!!.remove()
        }
        mGroupChannelListAdapter.clearMap()
        mGroupChannelListAdapter.clearGroupChannelList()
        val query = groupUtils.getListGroupChannel()
        mChannelCollection = ChannelCollection(query)
        mChannelCollection!!.setCollectionHandler(mChannelCollectionHandler)
        mChannelCollection!!.fetch {
            if (binding!!.mSwipeRefresh.isRefreshing) {
                binding!!.mSwipeRefresh.isRefreshing = false
            }
        }
    }

    private var mChannelCollectionHandler =
        ChannelCollectionHandler { channelCollection, list, channelEventAction ->
            if (activity == null) {
                return@ChannelCollectionHandler
            }
            requireActivity().runOnUiThread {
                if (binding!!.mSwipeRefresh.isRefreshing) {
                    binding!!.mSwipeRefresh.isRefreshing = false
                }
                when (channelEventAction) {
                    ChannelEventAction.INSERT -> {
                        mGroupChannelListAdapter.clearMap()
                        mGroupChannelListAdapter.insertChannels(
                            list,
                            channelCollection.query.order
                        )
                    }
                    ChannelEventAction.UPDATE -> {
                        mGroupChannelListAdapter.clearMap()
                        mGroupChannelListAdapter.updateGroupChannels(list)
                    }
                    ChannelEventAction.MOVE -> {
                        mGroupChannelListAdapter.clearMap()
                        mGroupChannelListAdapter.moveGroupChannels(
                            list,
                            channelCollection.query.order
                        )
                    }
                    ChannelEventAction.REMOVE -> {
                        mGroupChannelListAdapter.clearMap()
                        mGroupChannelListAdapter.removeGroupChannels(list)
                    }
                    else -> {
                        mGroupChannelListAdapter.clearMap()
                        mGroupChannelListAdapter.clearGroupChannelList()
                    }
                }
            }
        }

    override fun getConnectionHandlerId(): String {
        return "CONNECTION_HANDLER_GROUP_CHANNEL_ACTIVITY"
    }

    companion object {
        private const val CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_LIST"
    }
}