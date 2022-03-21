package com.example.sendbirddemo.ui.chat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentChatBinding
import com.example.sendbirddemo.ui.base.BasePermissionRequestFragment
import com.example.sendbirddemo.ui.chat.adapter.ChatAdapter
import com.example.sendbirddemo.utils.ChatUtils
import com.example.sendbirddemo.utils.Constants
import com.example.sendbirddemo.utils.SharedPreferenceUtils
import com.example.sendbirddemo.utils.Utils
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import com.sendbird.android.*
import com.sendbird.android.BaseChannel.UpdateUserMessageHandler
import com.sendbird.android.SendBird.ChannelHandler
import com.sendbird.android.SendBird.ConnectionHandler
import com.sendbird.syncmanager.FailedMessageEventActionReason
import com.sendbird.syncmanager.MessageCollection
import com.sendbird.syncmanager.MessageEventAction
import com.sendbird.syncmanager.handler.CompletionHandler
import com.sendbird.syncmanager.handler.MessageCollectionHandler

class ChatFragment : BasePermissionRequestFragment<FragmentChatBinding>() {

    private val args: ChatFragmentArgs by navArgs()
    private var mGroupChannel: GroupChannel? = null
    private var mGroupChannelUrl: String? = null
    private var mEditingMessage: BaseMessage? = null
    private var mMessageCollection: MessageCollection? = null
    private var mLastRead: Long = 0
    private var mChatAdapter: ChatAdapter? = null
    private var mCurrentState = STATE_NORMAL
    private var mLayoutManager: LinearLayoutManager? = null
    private val chatUtils: ChatUtils by lazy {
        ChatUtils()
    }

    override fun getLayoutID() = R.layout.fragment_chat

    override fun initView() {
        (activity as AppCompatActivity).setSupportActionBar(binding!!.mToolbarInviteMember)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mLastRead = SharedPreferenceUtils.getInstance(requireContext())?.getLastRead()!!
        mChatAdapter = ChatAdapter(requireContext())

        binding!!.edtInputChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()){
                    setTypingStatus(false)
                } else {
                    setTypingStatus(true)
                }
            }
            override fun afterTextChanged(s: Editable) {
                binding!!.btnSend.isEnabled = s.isNotEmpty()
            }
        })

        binding!!.btnSend.isEnabled = false
        binding!!.btnSend.setOnClickListener(View.OnClickListener {
            if (mCurrentState == STATE_EDIT) {
                val userInput: String = binding!!.edtInputChat.text.toString()
                if (userInput.isNotEmpty() && mEditingMessage != null) {
                    editMessage(mEditingMessage!!, userInput)
                }
                setState(
                    STATE_NORMAL,
                    null,
                    -1
                )
            } else {
                val userInput: String = binding!!.edtInputChat.text.toString()
                if (userInput.isEmpty()) {
                    return@OnClickListener
                }
                chatUtils.sendUserMessage(
                    requireContext(),
                    mGroupChannel,
                    mMessageCollection,
                    userInput
                )
                binding!!.edtInputChat.setText("")
                binding!!.rcChat.scrollToPosition(0)
            }
        })
        binding!!.tvChatNewMessage.setOnClickListener {
            binding!!.tvChatNewMessage.visibility = View.GONE
            mMessageCollection!!.resetViewpointTimestamp(Long.MAX_VALUE)
            fetchInitialMessages()
        }

        binding!!.btnUpload.setOnClickListener {
            if (Utils.storagePermissionGrant(requireContext())) {
                val intent = Intent()
                intent.type = "*/*"
                intent.action = Intent.ACTION_GET_CONTENT
                getMedia.launch(intent)
                SendBird.setAutoBackgroundDetection(false)
            } else {
                requestPermission()
            }
        }

        setUpRecyclerView()
        chatUtils.createMessageCollection(
            requireContext(),
            mGroupChannelUrl!!,
            mLastRead,
            object : ChatUtils.OnCreateMessageCollection {
                override fun onCreateMessageCollectionFailed() {
                    Handler().postDelayed({ requireActivity().onBackPressed() }, 1000)
                }

                override fun onCreateMessageCollectionSucceed(
                    groupChannel: GroupChannel,
                    messageCollection: MessageCollection
                ) {
                    if (mMessageCollection != null) {
                        mMessageCollection!!.remove()
                    }
                    mMessageCollection = messageCollection
                    mMessageCollection!!.setCollectionHandler(mMessageCollectionHandler)
                    mGroupChannel = mMessageCollection!!.channel
                    mChatAdapter!!.setChannel(mGroupChannel!!)
                    if (activity == null) {
                        return
                    }
                    requireActivity().runOnUiThread {
                        mChatAdapter?.clear()
                    }
                    fetchInitialMessages()
                }

            })
        setupClickedListener()
    }

    override fun initViewModel() {
        setHasOptionsMenu(true)
        mGroupChannelUrl = args.groupChannelUrl
    }

    override fun onResume() {
        super.onResume()
        SendBird.addConnectionHandler(
            CONNECTION_HANDLER_ID,
            object : ConnectionHandler {
                override fun onReconnectStarted() {}
                override fun onReconnectSucceeded() {
                    if (mMessageCollection != null) {
                        if (mLayoutManager!!.findFirstVisibleItemPosition() <= 0) {
                            mMessageCollection!!.fetchAllNextMessages { hasMore, e -> }
                        }
                        if (mLayoutManager!!.findLastVisibleItemPosition() == mChatAdapter!!.itemCount - 1) {
                            mMessageCollection!!.fetchSucceededMessages(
                                MessageCollection.Direction.PREVIOUS
                            ) { hasMore, e -> }
                        }
                    }
                }

                override fun onReconnectFailed() {}
            })

        SendBird.addChannelHandler(
            CHANNEL_HANDLER_ID,
            object : ChannelHandler() {
                override fun onMessageReceived(
                    baseChannel: BaseChannel,
                    baseMessage: BaseMessage
                ) {
                }

                override fun onReadReceiptUpdated(channel: GroupChannel) {
                    if (channel.url == mGroupChannelUrl) {
                        mChatAdapter!!.notifyDataSetChanged()
                    }
                }

                override fun onTypingStatusUpdated(channel: GroupChannel) {
                    if (channel.url == mGroupChannelUrl) {
                        val typingUsers = channel.typingMembers
                        displayTyping(typingUsers)
                    }
                }

                override fun onDeliveryReceiptUpdated(channel: GroupChannel) {
                    if (channel.url == mGroupChannelUrl) {
                        mChatAdapter!!.notifyDataSetChanged()
                    }
                }
            })
    }

    override fun onPause() {
        super.onPause()
        setTypingStatus(false)
        displayTyping(null)

        SendBird.removeConnectionHandler(CONNECTION_HANDLER_ID)
        SendBird.removeChannelHandler(CHANNEL_HANDLER_ID)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Save messages to cache.
        if (mMessageCollection != null) {
            mMessageCollection!!.setCollectionHandler(null)
            mMessageCollection!!.remove()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_group_chat, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_group_channel_invite -> {
                val action = ChatFragmentDirections.actionGlobalInviteMemberFragment()
                    .setGroupChannelUrl(mGroupChannelUrl!!)
                findNavController().navigate(action)
                true
            }
            R.id.action_group_channel_view_members -> {
                val bundle = bundleOf(Constants.GROUP_CHANNEL_KEY to mGroupChannel!!.serialize())
                findNavController().navigate(R.id.action_global_memberListFragment, bundle)
                true
            }
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var getMedia =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                chatUtils.sendFileWithThumbnail(
                    requireContext(),
                    mGroupChannel,
                    mMessageCollection,
                    intent?.data!!,
                    object : ChatUtils.OnSendFileWithThumbnailListener {
                        override fun onSendFileWithThumbnailFailed() {
                            if (activity != null) {
                                Toast.makeText(
                                    activity,
                                    getString(R.string.sendbird_error_with_code),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onSendFileWithThumbnailSucceed(
                            tempFileMessage: FileMessage,
                            uri: Uri
                        ) {
                            mChatAdapter?.addTempFileMessageInfo(tempFileMessage, uri)
                        }

                    })
            }
        }

    private fun setupClickedListener() {
        mChatAdapter?.setOnItemMessageListener(object : ChatAdapter.OnItemMessageListener {
            override fun onItemMyMessageLongClicked(userMessage: UserMessage, position: Int) {
                if (userMessage.sender.userId == SharedPreferenceUtils.getInstance(requireContext())
                        ?.getUserId()
                ) {
                    showMessageOptionsDialog(userMessage, position)
                }
            }

        })

        mChatAdapter?.setOnFileMessageListener(object : ChatAdapter.OnFileMessageListener {
            override fun onFileMessageClicked(playerView: PlayerView, fileMessage: FileMessage) {
                mChatAdapter!!.releasePlayer()
                mChatAdapter!!.setPlayVideo(playerView, fileMessage.url)
            }

        })
    }

    private fun showMessageOptionsDialog(message: BaseMessage, position: Int) {
        val options: Array<String> = if (message.messageId == 0L) {
            arrayOf(getString(R.string.option_delete_message))
        } else {
            arrayOf(
                getString(R.string.option_edit_message),
                getString(R.string.option_delete_message)
            )
        }
        val builder = AlertDialog.Builder(requireContext())
        builder.setItems(options) { dialog, which ->
            if (options.size == 1) {
                chatUtils.deleteMessage(
                    requireContext(),
                    mGroupChannel,
                    mMessageCollection,
                    message
                )
            } else {
                if (which == 0) {
                    setState(
                        STATE_EDIT,
                        message,
                        position
                    )
                } else if (which == 1) {
                    chatUtils.deleteMessage(
                        requireContext(),
                        mGroupChannel,
                        mMessageCollection,
                        message
                    )
                }
            }
        }
        builder.create().show()
    }

    private fun fetchInitialMessages() {
        if (mMessageCollection == null) {
            return
        }
        mMessageCollection!!.fetchSucceededMessages(
            MessageCollection.Direction.PREVIOUS
        ) { hasMore, e ->
            mMessageCollection!!.fetchSucceededMessages(
                MessageCollection.Direction.NEXT
            ) { hasMore, e ->
                mMessageCollection!!.fetchFailedMessages(CompletionHandler {
                    if (activity == null) {
                        return@CompletionHandler
                    }
                    requireActivity().runOnUiThread {
                        mChatAdapter?.markAllMessagesAsRead()
                        mLayoutManager?.scrollToPositionWithOffset(
                            mChatAdapter!!.getLastReadPosition(
                                mLastRead
                            ), binding!!.rcChat.height / 2
                        )
                    }
                })
            }
        }
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(activity)
        mLayoutManager!!.reverseLayout = true
        binding!!.rcChat.apply {
            layoutManager = mLayoutManager
            adapter = mChatAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        if (mLayoutManager!!.findFirstVisibleItemPosition() == 0) {
                            mMessageCollection!!.fetchSucceededMessages(
                                MessageCollection.Direction.NEXT,
                                null
                            )
                            binding!!.tvChatNewMessage.visibility = View.GONE
                        }
                        if (mLayoutManager!!.findLastVisibleItemPosition() == mChatAdapter!!.itemCount - 1) {
                            mMessageCollection!!.fetchSucceededMessages(
                                MessageCollection.Direction.PREVIOUS,
                                null
                            )
                        }
                    }
                }
            })
        }
    }

    private fun setState(state: Int?, editingMessage: BaseMessage?, position: Int?) {
        when (state) {
            STATE_NORMAL -> {
                mCurrentState = STATE_NORMAL
                mEditingMessage = null
                binding!!.btnUpload.visibility = View.VISIBLE
                binding!!.btnSend.text = getString(R.string.action_send_message)
                binding!!.edtInputChat.setText("")
            }
            STATE_EDIT -> {
                mCurrentState = STATE_EDIT
                mEditingMessage = editingMessage
                binding!!.btnUpload.visibility = View.GONE
                binding!!.btnSend.text = getString(R.string.action_update_message)
                var messageString = (editingMessage as UserMessage).message
                if (messageString == null) {
                    messageString = ""
                }
                binding!!.edtInputChat.setText(messageString)
                if (messageString.isNotEmpty()) {
                    binding!!.edtInputChat.setSelection(0, messageString.length)
                }
                binding!!.edtInputChat.requestFocus()
                binding!!.edtInputChat.postDelayed(Runnable {
                    binding!!.rcChat.postDelayed(
                        { binding!!.rcChat.scrollToPosition(position!!) },
                        500
                    )
                }, 100)
            }
        }
    }

    private fun editMessage(message: BaseMessage, editedMessage: String) {
        if (mGroupChannel == null) {
            return
        }
        mGroupChannel?.updateUserMessage(message.messageId, editedMessage, null, null,
            UpdateUserMessageHandler { userMessage, e ->
                if (e != null) {
                    // Error!
                    Toast.makeText(
                        activity,
                        getString(R.string.sendbird_error_with_code, e.code, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@UpdateUserMessageHandler
                }
                if (mMessageCollection != null) {
                    mMessageCollection!!.updateMessage(userMessage)
                }
            })
    }

    /**
     * Notify other users whether the current user is typing.
     *
     * @param typing Whether the user is currently typing.
     */
    private fun setTypingStatus(typing: Boolean) {
        if (mGroupChannel == null) {
            return
        }
        if (typing) {
            mGroupChannel?.startTyping()
        } else {
            mGroupChannel?.endTyping()
        }
    }

    private fun updateLastSeenTimestamp(messages: List<BaseMessage>) {
        var lastSeenTimestamp = if (mLastRead == Long.MAX_VALUE) 0 else mLastRead
        for (message in messages) {
            if (lastSeenTimestamp < message.createdAt) {
                lastSeenTimestamp = message.createdAt
            }
        }
        if (lastSeenTimestamp > mLastRead) {
            SharedPreferenceUtils.getInstance(requireContext())
                ?.setLastRead(mGroupChannelUrl!!, lastSeenTimestamp)
            mLastRead = lastSeenTimestamp
        }
    }

    private val mMessageCollectionHandler: MessageCollectionHandler =
        object : MessageCollectionHandler() {
            override fun onMessageEvent(
                collection: MessageCollection,
                messages: List<BaseMessage>,
                action: MessageEventAction
            ) {
            }

            override fun onSucceededMessageEvent(
                collection: MessageCollection,
                messages: List<BaseMessage>,
                action: MessageEventAction
            ) {
                Log.d(
                    "SyncManager",
                    "onSucceededMessageEvent: size = " + messages.size + ", action = " + action
                )
                if (activity == null) {
                    return
                }
                activity!!.runOnUiThread {
                    when (action) {
                        MessageEventAction.INSERT -> {
                            mChatAdapter?.insertSucceededMessages(messages)
                            mChatAdapter!!.markAllMessagesAsRead()
                        }
                        MessageEventAction.REMOVE -> mChatAdapter?.removeSucceededMessages(messages)
                        MessageEventAction.UPDATE -> mChatAdapter?.updateSucceededMessages(messages)
                        MessageEventAction.CLEAR -> mChatAdapter?.clear()
                    }
                }
                updateLastSeenTimestamp(messages)
            }

            override fun onPendingMessageEvent(
                collection: MessageCollection,
                messages: List<BaseMessage>,
                action: MessageEventAction
            ) {
                Log.d(
                    "SyncManager",
                    "onPendingMessageEvent: size = " + messages.size + ", action = " + action
                )
                if (activity == null) {
                    return
                }
                activity!!.runOnUiThread {
                    when (action) {
                        MessageEventAction.INSERT -> {
                            val pendingMessages: MutableList<BaseMessage> =
                                ArrayList()
                            for (message in messages) {
                                if (!mChatAdapter!!.failedMessageListContains(message)) {
                                    pendingMessages.add(message)
                                }
                            }
                            mChatAdapter?.insertSucceededMessages(pendingMessages)
                        }
                        MessageEventAction.REMOVE -> mChatAdapter?.removeSucceededMessages(messages)
                    }
                }
            }

            override fun onFailedMessageEvent(
                collection: MessageCollection,
                messages: List<BaseMessage>,
                action: MessageEventAction,
                reason: FailedMessageEventActionReason
            ) {
                Log.d(
                    "SyncManager",
                    "onFailedMessageEvent: size = " + messages.size + ", action = " + action
                )
                if (activity == null) {
                    return
                }
                activity!!.runOnUiThread {
                    when (action) {
                        MessageEventAction.INSERT -> mChatAdapter?.insertFailedMessages(messages)
                        MessageEventAction.REMOVE -> mChatAdapter?.removeFailedMessages(messages)
                        MessageEventAction.UPDATE -> if (reason == FailedMessageEventActionReason.UPDATE_RESEND_FAILED) {
                            mChatAdapter?.updateFailedMessages(messages)
                        }
                    }
                }
            }

            override fun onNewMessage(collection: MessageCollection, message: BaseMessage) {
                Log.d("SyncManager", "onNewMessage: message = $message")
                //show when the scroll position is bottom ONLY.
                if (mLayoutManager!!.findFirstVisibleItemPosition() != 0) {
                    if (message is UserMessage) {
                        if (message.sender.userId != SharedPreferenceUtils.getInstance(
                                requireContext()
                            )?.getUserId()
                        ) {
                            binding!!.tvChatNewMessage.text =
                                "New Message = " + message.sender.nickname + " : " + message.message
                            binding!!.tvChatNewMessage.visibility = View.VISIBLE
                        }
                    } else if (message is FileMessage) {
                        if (message.sender.userId != SharedPreferenceUtils.getInstance(
                                requireContext()
                            )?.getUserId()
                        ) {
                            binding!!.tvChatNewMessage.text =
                                "New Message = " + message.sender.nickname + "Send a File"
                            binding!!.tvChatNewMessage.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

    /**
     * Display which users are typing.
     * If more than two users are currently typing, this will state that "multiple users" are typing.
     *
     * @param typingUsers The list of currently typing users.
     */
    private fun displayTyping(typingUsers: List<Member>?) {
        if (typingUsers != null && typingUsers.isNotEmpty()) {
            binding!!.mLayoutCurrentEvent.visibility = View.VISIBLE
            val string: String = when (typingUsers.size) {
                1 -> {
                    String.format(getString(R.string.user_typing), typingUsers[0].nickname)
                }
                2 -> {
                    String.format(
                        getString(R.string.two_users_typing),
                        typingUsers[0].nickname,
                        typingUsers[1].nickname
                    )
                }
                else -> {
                    getString(R.string.users_typing)
                }
            }
            binding!!.tvCurrentEvent.text = string
        } else {
            binding!!.mLayoutCurrentEvent.visibility = View.GONE
        }
    }

    companion object {
        const val STATE_NORMAL = 0
        const val STATE_EDIT = 1
        private const val CONNECTION_HANDLER_ID = "CONNECTION_HANDLER_GROUP_CHAT"
        private const val CHANNEL_HANDLER_ID = "CHANNEL_HANDLER_GROUP_CHANNEL_CHAT"
        private const val INTENT_REQUEST_CHOOSE_MEDIA = 301
    }

    override fun setupWhenPermissionGranted() {

    }
}