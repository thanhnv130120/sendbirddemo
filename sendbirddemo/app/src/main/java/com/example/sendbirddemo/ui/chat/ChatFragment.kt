package com.example.sendbirddemo.ui.chat

import android.util.Log
import androidx.navigation.fragment.navArgs
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentChatBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.sendbird.android.BaseChannel
import com.sendbird.android.BaseMessage
import com.sendbird.android.GroupChannel
import com.sendbird.syncmanager.MessageCollection
import com.sendbird.syncmanager.MessageFilter

class ChatFragment : BaseFragment<FragmentChatBinding>() {

    private val args: ChatFragmentArgs by navArgs()
    private val mChannel: GroupChannel? = null
    private val mChannelUrl: String? = null
    private val mEditingMessage: BaseMessage? = null
    val mMessageFilter = MessageFilter(BaseChannel.MessageTypeFilter.ALL, null, null)
    private val mMessageCollection: MessageCollection? = null
    private val mLastRead: Long = 0

    override fun getLayoutID() = R.layout.fragment_chat

    override fun initView() {

        Log.d("TAG", "initView: ${args.groupChannelUrl}")

    }

    override fun initViewModel() {

    }
}