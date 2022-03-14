package com.example.sendbirddemo.ui.chat

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.navArgs
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentChatBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.chat.adapter.ChatAdapter
import com.example.sendbirddemo.utils.SharedPreferenceUtils
import com.example.sendbirddemo.utils.UrlPreviewInfo
import com.example.sendbirddemo.utils.WebUtils
import com.sendbird.android.*
import com.sendbird.android.BaseChannel.SendUserMessageHandler
import com.sendbird.android.BaseChannel.UpdateUserMessageHandler
import com.sendbird.syncmanager.MessageCollection
import com.sendbird.syncmanager.MessageFilter

class ChatFragment : BaseFragment<FragmentChatBinding>() {

    private val args: ChatFragmentArgs by navArgs()
    private var mGroupChannel: GroupChannel? = null
    private var mGroupChannelUrl: String? = null
    private var mEditingMessage: BaseMessage? = null
    val mMessageFilter = MessageFilter(BaseChannel.MessageTypeFilter.ALL, null, null)
    private var mMessageCollection: MessageCollection? = null
    private var mLastRead: Long = 0
    private var mChatAdapter: ChatAdapter? = null
    private var mCurrentState = STATE_NORMAL

    override fun getLayoutID() = R.layout.fragment_chat

    override fun initView() {
        mLastRead = SharedPreferenceUtils.getInstance(requireContext())?.getLastRead()!!
        mChatAdapter = ChatAdapter()

        binding!!.edtInputChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                binding!!.btnSend.isEnabled = s.isNotEmpty()
            }
        })

        binding!!.btnSend.isEnabled = false
        binding!!.btnSend.setOnClickListener(View.OnClickListener {
            if (mCurrentState == STATE_EDIT) {
                val userInput: String = binding!!.edtInputChat.getText().toString()
                if (userInput.isNotEmpty() && mEditingMessage != null) {
                    editMessage(mEditingMessage!!, userInput)
                }
                setState(
                    STATE_NORMAL,
                    null,
                    -1
                )
            } else {
                val userInput: String = binding!!.edtInputChat.getText().toString()
                if (userInput.length == 0) {
                    return@OnClickListener
                }
                sendUserMessage(userInput)
                binding!!.edtInputChat.setText("")
                binding!!.rcChat.scrollToPosition(0)
            }
        })
    }

    override fun initViewModel() {
        mGroupChannelUrl = args.groupChannelUrl
    }

    private fun sendUserMessage(text: String) {
        if (mGroupChannel == null) {
            return
        }
        val urls: List<String> = WebUtils.extractUrls(text)
        if (urls.size > 0) {
            sendUserMessageWithUrl(text, urls[0])
            return
        }
        val pendingMessage: UserMessage = mGroupChannel!!.sendUserMessage(text,
            SendUserMessageHandler { userMessage, e ->
                if (mMessageCollection != null) {
                    mMessageCollection!!.handleSendMessageResponse(userMessage, e)
                    mMessageCollection!!.fetchAllNextMessages(null)
                }
                if (e != null) {
                    // Error!
                    Toast.makeText(
                        activity,
                        getString(R.string.send_message_error, e.code, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@SendUserMessageHandler
                }
            })
        if (mMessageCollection != null) {
            mMessageCollection!!.appendMessage(pendingMessage)
        }
    }

    @SuppressLint("StaticFieldLeak")
    private fun sendUserMessageWithUrl(text: String, url: String) {
        if (mGroupChannel == null) {
            return
        }
        object : WebUtils.UrlPreviewAsyncTask() {
            override fun onPostExecute(info: UrlPreviewInfo) {
                if (mGroupChannel == null) {
                    return
                }
                var tempUserMessage: UserMessage? = null
                val handler =
                    SendUserMessageHandler { userMessage, e ->
                        if (e != null) {
                            // Error!
                            Toast.makeText(
                                activity,
                                getString(R.string.send_message_error, e.code, e.message),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        mMessageCollection!!.handleSendMessageResponse(userMessage, e)
                    }
                tempUserMessage = try {
                    // Sending a message with URL preview information and custom type.
                    val jsonString: String = info.toJsonString()
                    mGroupChannel!!.sendUserMessage(
                        text,
                        jsonString,
                        ChatAdapter.URL_PREVIEW_CUSTOM_TYPE,
                        handler
                    )
                } catch (e: Exception) {
                    // Sending a message without URL preview information.
                    mGroupChannel!!.sendUserMessage(text, handler)
                }


                // Display a user message to RecyclerView
                if (mMessageCollection != null) {
                    mMessageCollection!!.appendMessage(tempUserMessage)
                }
            }
        }.execute(url)
    }

    private fun setState(state: Int?, editingMessage: BaseMessage?, position: Int?) {
        when (state) {
            STATE_NORMAL -> {
                mCurrentState = STATE_NORMAL
                mEditingMessage = null
                binding!!.btnUpload.setVisibility(View.VISIBLE)
                binding!!.btnSend.setText(getString(R.string.action_send_message))
                binding!!.edtInputChat.setText("")
            }
            STATE_EDIT -> {
                mCurrentState = STATE_EDIT
                mEditingMessage = editingMessage
                binding!!.btnUpload.setVisibility(View.GONE)
                binding!!.btnSend.setText(getString(R.string.action_update_message))
                var messageString = (editingMessage as UserMessage).message
                if (messageString == null) {
                    messageString = ""
                }
                binding!!.edtInputChat.setText(messageString)
                if (messageString.length > 0) {
                    binding!!.edtInputChat.setSelection(0, messageString.length)
                }
                binding!!.edtInputChat.requestFocus()
                binding!!.edtInputChat.postDelayed(Runnable {
                    binding!!.rcChat.postDelayed(
                        Runnable { binding!!.rcChat.scrollToPosition(position!!) },
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

    companion object {
        const val STATE_NORMAL = 0
        const val STATE_EDIT = 1
    }
}