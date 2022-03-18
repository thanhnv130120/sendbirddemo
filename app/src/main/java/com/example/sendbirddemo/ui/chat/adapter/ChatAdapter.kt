package com.example.sendbirddemo.ui.chat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.databinding.*
import com.example.sendbirddemo.utils.SyncManagerUtils
import com.example.sendbirddemo.utils.SyncManagerUtils.getMyUserId
import com.example.sendbirddemo.utils.UrlPreviewInfo
import com.example.sendbirddemo.utils.Utils
import com.sendbird.android.*
import com.sendbird.android.FileMessage.Thumbnail
import kotlinx.android.synthetic.main.partial_group_chat_info.view.*
import org.json.JSONException
import java.util.*

class ChatAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mGroupChannel: GroupChannel? = null
    private val mMessageList = mutableListOf<BaseMessage>()
    private val mFailedMessageList = mutableListOf<BaseMessage>()
    private val mResendingMessageSet = mutableSetOf<String>()
    private val mTempFileMessageUriTable = Hashtable<String, Uri>()
    private var mOnItemMessageListener: OnItemMessageListener? = null
    private var mOnFileMessageListener: OnFileMessageListener? = null

    private fun getMessage(position: Int): BaseMessage? {
        return when {
            position < mFailedMessageList.size -> {
                mFailedMessageList[position]
            }
            position < mFailedMessageList.size + mMessageList.size -> {
                mMessageList[position - mFailedMessageList.size]
            }
            else -> {
                null
            }
        }
    }

    /**
     * Checks if the current message was sent by the same person that sent the preceding message.
     *
     *
     * This is done so that redundant UI, such as sender nickname and profile picture,
     * does not have to displayed when not necessary.
     */
    private fun isContinuous(currentMsg: BaseMessage?, precedingMsg: BaseMessage?): Boolean {
        // null check
        if (currentMsg == null || precedingMsg == null) {
            return false
        }
        if (currentMsg is AdminMessage && precedingMsg is AdminMessage) {
            return true
        }
        var currentUser: User? = null
        var precedingUser: User? = null
        if (currentMsg is UserMessage) {
            currentUser = currentMsg.sender
        } else if (currentMsg is FileMessage) {
            currentUser = currentMsg.sender
        }
        if (precedingMsg is UserMessage) {
            precedingUser = precedingMsg.sender
        } else if (precedingMsg is FileMessage) {
            precedingUser = precedingMsg.sender
        }
        if (currentUser == null || precedingUser == null) {
            return false
        }
        return if (currentUser.userId == null || precedingUser.userId == null) {
            false
        } else currentUser.userId == precedingUser.userId
    }

    fun setChannel(channel: GroupChannel) {
        mGroupChannel = channel
    }

    private fun isTempMessage(message: BaseMessage): Boolean {
        return message.messageId == 0L
    }

    fun isFailedMessage(message: BaseMessage?): Boolean {
        return if (message == null) {
            false
        } else mFailedMessageList.contains(message)
    }

    fun isResendingMessage(message: BaseMessage?): Boolean {
        return if (message == null) {
            false
        } else mResendingMessageSet.contains(getRequestId(message))
    }

    private fun getRequestId(message: BaseMessage): String? {
        if (message is UserMessage) {
            return message.requestId
        } else if (message is FileMessage) {
            return message.requestId
        }
        return ""
    }

    private fun getTempFileMessageUri(message: BaseMessage?): Uri? {
        if (!isTempMessage(message!!)) {
            return null
        }
        return if (message !is FileMessage) {
            null
        } else mTempFileMessageUriTable[message.requestId]
    }

    fun addTempFileMessageInfo(message: FileMessage, uri: Uri?) {
        mTempFileMessageUriTable[message.requestId] = uri
    }

    fun insertSucceededMessages(messages: List<BaseMessage?>) {
        for (message in messages) {
            val index: Int = SyncManagerUtils.findIndexOfMessage(mMessageList, message!!)
            mMessageList.add(index, message)
        }
        notifyDataSetChanged()
    }

    fun updateSucceededMessages(messages: List<BaseMessage?>) {
        for (message in messages) {
            val index: Int = SyncManagerUtils.getIndexOfMessage(mMessageList, message!!)
            if (index != -1) {
                mMessageList[index] = message
                notifyItemChanged(index)
            }
        }
    }

    fun removeSucceededMessages(messages: List<BaseMessage?>) {
        for (message in messages) {
            val index: Int = SyncManagerUtils.getIndexOfMessage(mMessageList, message!!)
            if (index != -1) {
                mMessageList.removeAt(index)
            }
        }
        notifyDataSetChanged()
    }

    /**
     * Notifies that the user has read all (previously unread) messages in the channel.
     * Typically, this would be called immediately after the user enters the chat and loads
     * its most recent messages.
     */
    fun markAllMessagesAsRead() {
        if (mGroupChannel != null) {
            mGroupChannel!!.markAsRead()
        }
        notifyDataSetChanged()
    }

    fun getLastReadPosition(lastRead: Long): Int {
        for (i in mMessageList.indices) {
            if (mMessageList[i].createdAt == lastRead) {
                return i + mFailedMessageList.size
            }
        }
        return 0
    }

    fun insertFailedMessages(messages: List<BaseMessage?>) {
        synchronized(mFailedMessageList) {
            for (message in messages) {
                val requestId = getRequestId(message!!)
                if (requestId!!.isEmpty()) {
                    continue
                }
                mResendingMessageSet.add(requestId)
                mFailedMessageList.add(message)
            }
            mFailedMessageList.sortWith { m1, m2 ->
                val x = m1.createdAt
                val y = m2.createdAt
                if (x < y) 1 else if (x == y) 0 else -1
            }
        }
        notifyDataSetChanged()
    }

    fun updateFailedMessages(messages: List<BaseMessage?>) {
        synchronized(mFailedMessageList) {
            for (message in messages) {
                val requestId = getRequestId(message!!)
                if (requestId!!.isEmpty()) {
                    continue
                }
                mResendingMessageSet.remove(requestId)
            }
        }
        notifyDataSetChanged()
    }

    fun removeFailedMessages(messages: List<BaseMessage?>) {
        synchronized(mFailedMessageList) {
            for (message in messages) {
                val requestId = getRequestId(message!!)
                mResendingMessageSet.remove(requestId)
                mFailedMessageList.remove(message)
            }
        }
        notifyDataSetChanged()
    }

    fun failedMessageListContains(message: BaseMessage?): Boolean {
        if (mFailedMessageList.isEmpty()) {
            return false
        }
        for (failedMessage in mFailedMessageList) {
            if (message is UserMessage && failedMessage is UserMessage) {
                if (message.requestId == failedMessage.requestId) {
                    return true
                }
            } else if (message is FileMessage && failedMessage is FileMessage) {
                if (message.requestId == failedMessage.requestId) {
                    return true
                }
            }
        }
        return false
    }

    fun clear() {
        mMessageList.clear()
        mFailedMessageList.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER_MESSAGE_ME -> {
                val listItemGroupChatUserMeBinding = ListItemGroupChatUserMeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MyUserMessageHolder(
                    listItemGroupChatUserMeBinding
                )
            }
            VIEW_TYPE_USER_MESSAGE_OTHER -> {
                val listItemGroupChatUserOtherBinding = ListItemGroupChatUserOtherBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                OtherUserMessageHolder(
                    listItemGroupChatUserOtherBinding
                )
            }
            VIEW_TYPE_ADMIN_MESSAGE -> {
                val listItemGroupChatAdminBinding = ListItemGroupChatAdminBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AdminMessageHolder(
                    listItemGroupChatAdminBinding
                )
            }
            VIEW_TYPE_FILE_MESSAGE_ME -> {
                val listItemGroupChatFileMeBinding = ListItemGroupChatFileMeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                MyFileMessageHolder(
                    listItemGroupChatFileMeBinding
                )
            }
            VIEW_TYPE_FILE_MESSAGE_OTHER -> {
                val listItemGroupChatFileOtherBinding = ListItemGroupChatFileOtherBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                OtherFileMessageHolder(
                    listItemGroupChatFileOtherBinding
                )
            }
            VIEW_TYPE_FILE_MESSAGE_IMAGE_ME -> {
                val listItemGroupChatFileImageMeBinding =
                    ListItemGroupChatFileImageMeBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                MyImageFileMessageHolder(
                    listItemGroupChatFileImageMeBinding
                )
            }
            VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER -> {
                val listItemGroupChatFileImageOtherBinding =
                    ListItemGroupChatFileImageOtherBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                OtherImageFileMessageHolder(
                    listItemGroupChatFileImageOtherBinding
                )
            }
            VIEW_TYPE_FILE_MESSAGE_VIDEO_ME -> {
                val listItemGroupChatFileMessageVideoMeBinding =
                    ListItemGroupChatFileVideoMeBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                MyVideoFileMessageHolder(
                    listItemGroupChatFileMessageVideoMeBinding
                )
            }
            else -> {
                val listItemGroupChatFileMessageVideoOtherBinding =
                    ListItemGroupChatFileVideoOtherBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                OtherVideoFileMessageHolder(
                    listItemGroupChatFileMessageVideoOtherBinding
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message: BaseMessage = getMessage(position) ?: return

        var isContinuous = false
        var isNewDay = false
        var isTempMessage = false
        var tempFileMessageUri: Uri? = null

        // If there is at least one item preceding the current one, check the previous message.

        // If there is at least one item preceding the current one, check the previous message.
        if (position < mMessageList.size + mFailedMessageList.size - 1) {
            val prevMessage: BaseMessage? = getMessage(position + 1)

            // If the date of the previous message is different, display the date before the message,
            // and also set isContinuous to false to show information such as the sender's nickname
            // and profile image.
            if (!Utils.hasSameDate(message.createdAt, prevMessage!!.createdAt)) {
                isNewDay = true
                isContinuous = false
            } else {
                isContinuous = isContinuous(message, prevMessage)
            }
        } else if (position == mFailedMessageList.size + mMessageList.size - 1) {
            isNewDay = true
        }

        isTempMessage = isTempMessage(message)
        tempFileMessageUri = getTempFileMessageUri(message)

        when (holder.itemViewType) {
            VIEW_TYPE_USER_MESSAGE_ME -> (holder as MyUserMessageHolder).bind(
                message as UserMessage,
                mGroupChannel,
                isContinuous,
                isNewDay,
                mOnItemMessageListener
            )
            VIEW_TYPE_USER_MESSAGE_OTHER -> (holder as OtherUserMessageHolder).bind(
                message as UserMessage,
                isNewDay,
                isContinuous
            )
            VIEW_TYPE_ADMIN_MESSAGE -> (holder as AdminMessageHolder).bind(
                message as AdminMessage,
                isNewDay
            )
            VIEW_TYPE_FILE_MESSAGE_ME -> (holder as MyFileMessageHolder).bind(
                message as FileMessage,
                isNewDay
            )
            VIEW_TYPE_FILE_MESSAGE_OTHER -> (holder as OtherFileMessageHolder).bind(
                message as FileMessage,
                isNewDay,
                isContinuous
            )
            VIEW_TYPE_FILE_MESSAGE_IMAGE_ME -> (holder as MyImageFileMessageHolder).bind(
                message as FileMessage,
                mGroupChannel,
                isNewDay,
                isTempMessage,
                tempFileMessageUri
            )
            VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER -> (holder as OtherImageFileMessageHolder).bind(
                message as FileMessage,
                mGroupChannel,
                isNewDay,
                isContinuous
            )
            VIEW_TYPE_FILE_MESSAGE_VIDEO_ME -> (holder as MyVideoFileMessageHolder).bind(
                message as FileMessage,
                mGroupChannel,
                isNewDay,
                isTempMessage,
                tempFileMessageUri
            )
            VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER -> (holder as OtherVideoFileMessageHolder).bind(
                message as FileMessage,
                isNewDay,
                isContinuous
            )
        }
    }

    override fun getItemCount() = mMessageList.size + mFailedMessageList.size

    override fun getItemViewType(position: Int): Int {
        val message = getMessage(position)
        var isMyMessage = false

        if (message is UserMessage) {
            val requestState = message.requestState
            if (requestState == UserMessage.RequestState.PENDING || requestState == UserMessage.RequestState.FAILED || message.sender.userId == getMyUserId(
                    context
                )
            ) {
                isMyMessage = true
            }
        } else if (message is FileMessage) {
            val requestState = message.requestState
            if (requestState == FileMessage.RequestState.PENDING || requestState == FileMessage.RequestState.FAILED || message.sender.userId == getMyUserId(
                    context
                )
            ) {
                isMyMessage = true
            }
        }

        when (message) {
            is UserMessage -> {
                return if (isMyMessage) {
                    VIEW_TYPE_USER_MESSAGE_ME
                } else {
                    VIEW_TYPE_USER_MESSAGE_OTHER
                }
            }
            is FileMessage -> {
                return if (message.type.toLowerCase().startsWith("image")) {
                    // If the sender is current user
                    if (isMyMessage) {
                        VIEW_TYPE_FILE_MESSAGE_IMAGE_ME
                    } else {
                        VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER
                    }
                } else if (message.type.toLowerCase().startsWith("video")) {
                    if (isMyMessage) {
                        VIEW_TYPE_FILE_MESSAGE_VIDEO_ME
                    } else {
                        VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER
                    }
                } else {
                    if (isMyMessage) {
                        VIEW_TYPE_FILE_MESSAGE_ME
                    } else {
                        VIEW_TYPE_FILE_MESSAGE_OTHER
                    }
                }
            }
            is AdminMessage -> {
                return VIEW_TYPE_ADMIN_MESSAGE
            }
            else -> return -1
        }

    }

    private open class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: BaseMessage, isNewDay: Boolean) {
            // Show the date if the message was sent on a different date than the previous message.
            if (isNewDay) {
                itemView.tvChatDate.visibility = View.VISIBLE
                itemView.tvChatDate.text = Utils.formatDate(message.createdAt)
            } else {
                itemView.tvChatDate.tvChatDate.visibility = View.GONE
            }
        }
    }

    private class AdminMessageHolder(val binding: ListItemGroupChatAdminBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: AdminMessage,
            isNewDay: Boolean
        ) {
            super.bind(message, isNewDay)
            binding.tvChatMessage.text = message.message
        }
    }

    private class MyUserMessageHolder(val binding: ListItemGroupChatUserMeBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: UserMessage,
            channel: GroupChannel?,
            isContinuous: Boolean,
            isNewDay: Boolean,
            mOnItemMyMessageListener: OnItemMessageListener?
        ) {
            super.bind(message, isNewDay)
            binding.tvChatMessage.text = message.message
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)
            if (message.updatedAt > 0) {
                binding.tvEdited.visibility = View.VISIBLE
            } else {
                binding.tvEdited.visibility = View.GONE
            }

            // If continuous from previous message, removeSucceededMessages extra padding.
            if (isContinuous) {
                binding.viewGroupChatPadding.visibility = View.GONE
            } else {
                binding.viewGroupChatPadding.visibility = View.VISIBLE
            }
            binding.mLayoutUrlPreview.visibility = View.GONE
            if (message.customType == URL_PREVIEW_CUSTOM_TYPE) {
                try {
                    binding.mLayoutUrlPreview.visibility = View.VISIBLE
                    val previewInfo = UrlPreviewInfo(message.data)
                    binding.tvUrlPreviewSiteName.text = "@${previewInfo.mSiteName}"
                    binding.tvUrlPreviewTitle.text = previewInfo.mTitle
                    binding.tvUrlPreviewDescription.text = previewInfo.mDescription
                    Utils.displayImageFromUrl(
                        binding.imgUrlPreviewMain.context,
                        previewInfo.mImageUrl,
                        binding.imgUrlPreviewMain
                    )
                } catch (e: JSONException) {
                    binding.mLayoutUrlPreview.visibility = View.GONE
                    e.printStackTrace()
                }
            }
            binding.root.setOnLongClickListener {
                mOnItemMyMessageListener?.onItemMyMessageLongClicked(message, position)
                true
            }
            binding.mLayoutMessageStatus.drawMessageStatus(channel, message)
        }
    }

    private class OtherUserMessageHolder(val binding: ListItemGroupChatUserOtherBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: UserMessage,
            isNewDay: Boolean,
            isContinuous: Boolean
        ) {
            super.bind(message, isNewDay)
            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
                binding.imgImageProfile.visibility = View.INVISIBLE
                binding.tvNickname.visibility = View.GONE
            } else {
                binding.imgImageProfile.visibility = View.VISIBLE
                Utils.displayRoundImageFromUrl(
                    binding.imgImageProfile.context,
                    message.sender.profileUrl,
                    binding.imgImageProfile
                )
                binding.tvNickname.visibility = View.VISIBLE
                binding.tvNickname.text = message.sender.nickname
            }
            binding.tvChatMessage.text = message.message
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)
            if (message.updatedAt > 0) {
                binding.tvEdited.visibility = View.VISIBLE
            } else {
                binding.tvEdited.visibility = View.GONE
            }
            binding.tvUrlPreviewContainer.visibility = View.GONE
            if (message.customType == URL_PREVIEW_CUSTOM_TYPE) {
                try {
                    binding.tvUrlPreviewContainer.visibility = View.VISIBLE
                    val previewInfo = UrlPreviewInfo(message.data)
                    binding.tvUrlPreviewSiteName.text = "@${previewInfo.mSiteName}"
                    binding.tvUrlPreviewTitle.text = previewInfo.mTitle
                    binding.tvUrlPreviewDescription.text = previewInfo.mDescription
                    Utils.displayImageFromUrl(
                        binding.imgImageProfile.context,
                        previewInfo.mImageUrl,
                        binding.imgImageProfile
                    )
                } catch (e: JSONException) {
                    binding.tvUrlPreviewContainer.visibility = View.GONE
                    e.printStackTrace()
                }
            }
        }
    }

    private class MyFileMessageHolder(val binding: ListItemGroupChatFileMeBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean
        ) {
            bind(message, isNewDay)
            binding.tvFileName.text = message.name
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)
            binding.mLayoutMessageStatus.drawMessageStatus(channel, message)
        }
    }

    private class OtherFileMessageHolder(val binding: ListItemGroupChatFileOtherBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: FileMessage,
            isNewDay: Boolean,
            isContinuous: Boolean
        ) {
            super.bind(message, isNewDay)
            binding.tvFileName.text = message.name
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)
            //            fileSizeText.setText(String.valueOf(message.getSize()));

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
                binding.imgImageProfile.visibility = View.INVISIBLE
                binding.tvNickname.visibility = View.GONE
            } else {
                binding.imgImageProfile.visibility = View.VISIBLE
                Utils.displayRoundImageFromUrl(
                    binding.imgImageProfile.context,
                    message.sender.profileUrl,
                    binding.imgImageProfile
                )
                binding.tvNickname.visibility = View.VISIBLE
                binding.tvNickname.text = message.sender.nickname
            }
        }
    }

    /**
     * A ViewHolder for file messages that are images.
     * Displays only the image thumbnail.
     */
    private class MyImageFileMessageHolder(val binding: ListItemGroupChatFileImageMeBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean,
            isTempMessage: Boolean,
            tempFileMessageUri: Uri?
        ) {
            super.bind(message, isNewDay)
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)
            if (isTempMessage && tempFileMessageUri != null) {
                Utils.displayImageFromUrl(
                    binding.imgChatFileThumbnail.context,
                    tempFileMessageUri.toString(),
                    binding.imgChatFileThumbnail
                )
            } else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    if (message.type.toLowerCase().contains("gif")) {
                        Utils.displayGifImageFromUrl(
                            binding.imgChatFileThumbnail.context,
                            message.url,
                            binding.imgChatFileThumbnail,
                            thumbnails[0].url
                        )
                    } else {
                        Utils.displayImageFromUrl(
                            binding.imgChatFileThumbnail.context,
                            thumbnails[0].url,
                            binding.imgChatFileThumbnail
                        )
                    }
                } else {
                    if (message.type.toLowerCase().contains("gif")) {
                        Utils.displayGifImageFromUrl(
                            binding.imgChatFileThumbnail.context,
                            message.url,
                            binding.imgChatFileThumbnail,
                            null as String?
                        )
                    } else {
                        Utils.displayImageFromUrl(
                            binding.imgChatFileThumbnail.context,
                            message.url,
                            binding.imgChatFileThumbnail
                        )
                    }
                }
            }
            binding.mLayoutMessageStatus.drawMessageStatus(channel, message)
        }
    }

    private class OtherImageFileMessageHolder(val binding: ListItemGroupChatFileImageOtherBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean,
            isContinuous: Boolean
        ) {
            super.bind(message, isNewDay)
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
                binding.imgImageProfile.visibility = View.INVISIBLE
                binding.tvNickname.visibility = View.GONE
            } else {
                binding.imgImageProfile.visibility = View.VISIBLE
                Utils.displayRoundImageFromUrl(
                    binding.imgImageProfile.context,
                    message.sender.profileUrl,
                    binding.imgImageProfile
                )
                binding.imgImageProfile.visibility = View.VISIBLE
                binding.tvNickname.text = message.sender.nickname
            }

            // Get thumbnails from FileMessage
            val thumbnails = message.thumbnails as ArrayList<Thumbnail>

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size > 0) {
                if (message.type.toLowerCase().contains("gif")) {
                    Utils.displayGifImageFromUrl(
                        binding.imgChatThumbnail.context,
                        message.url,
                        binding.imgChatThumbnail,
                        thumbnails[0].url
                    )
                } else {
                    Utils.displayImageFromUrl(
                        binding.imgChatThumbnail.context,
                        thumbnails[0].url,
                        binding.imgChatThumbnail
                    )
                }
            } else {
                if (message.type.toLowerCase().contains("gif")) {
                    Utils.displayGifImageFromUrl(
                        binding.imgChatThumbnail.context,
                        message.url,
                        binding.imgChatThumbnail,
                        null as String?
                    )
                } else {
                    Utils.displayImageFromUrl(
                        binding.imgChatThumbnail.context,
                        message.url,
                        binding.imgChatThumbnail
                    )
                }
            }
        }
    }

    /**
     * A ViewHolder for file messages that are videos.
     * Displays only the video thumbnail.
     */
    private class MyVideoFileMessageHolder(val binding: ListItemGroupChatFileVideoMeBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean,
            isTempMessage: Boolean,
            tempFileMessageUri: Uri?
        ) {
            super.bind(message, isNewDay)
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)
            if (isTempMessage && tempFileMessageUri != null) {
                Utils.displayImageFromUrl(
                    binding.imgChatFileThumbnail.context,
                    tempFileMessageUri.toString(),
                    binding.imgChatFileThumbnail
                )
            } else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    Utils.displayImageFromUrl(
                        binding.imgChatFileThumbnail.context,
                        thumbnails[0].url,
                        binding.imgChatFileThumbnail
                    )
                }
            }
            binding.mLayoutMessageStatus.drawMessageStatus(channel, message)
        }
    }

    private class OtherVideoFileMessageHolder(val binding: ListItemGroupChatFileVideoOtherBinding) :
        BaseViewHolder(binding.root) {
        fun bind(
            message: FileMessage,
            isNewDay: Boolean,
            isContinuous: Boolean
        ) {
            super.bind(message, isNewDay)
            binding.tvChatTime.text = Utils.formatTime(message.createdAt)

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
                binding.imgImageProfile.visibility = View.INVISIBLE
                binding.tvChatTime.visibility = View.GONE
            } else {
                binding.imgImageProfile.visibility = View.VISIBLE
                Utils.displayRoundImageFromUrl(
                    binding.imgImageProfile.context,
                    message.sender.profileUrl,
                    binding.imgImageProfile
                )
                binding.tvNickname.visibility = View.VISIBLE
                binding.tvNickname.text = message.sender.nickname
            }

            // Get thumbnails from FileMessage
            val thumbnails = message.thumbnails as ArrayList<Thumbnail>

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size > 0) {
                Utils.displayImageFromUrl(
                    binding.imgChatFileThumbnail.context,
                    thumbnails[0].url,
                    binding.imgChatFileThumbnail
                )
            }
        }
    }

    fun setOnItemMessageListener(onItemMessageListener: OnItemMessageListener) {
        mOnItemMessageListener = onItemMessageListener
    }

    fun setOnFileMessageListener(onFileMessageListener: OnFileMessageListener) {
        mOnFileMessageListener = onFileMessageListener
    }

    interface OnItemMessageListener {
        fun onItemMyMessageLongClicked(userMessage: UserMessage, position: Int)
    }

    interface OnFileMessageListener {
        fun onFileMessageClicked(fileMessage: FileMessage)
    }

    companion object {
        const val URL_PREVIEW_CUSTOM_TYPE = "url_preview"

        private const val VIEW_TYPE_USER_MESSAGE_ME = 10
        private const val VIEW_TYPE_USER_MESSAGE_OTHER = 11
        private const val VIEW_TYPE_FILE_MESSAGE_ME = 20
        private const val VIEW_TYPE_FILE_MESSAGE_OTHER = 21
        private const val VIEW_TYPE_FILE_MESSAGE_IMAGE_ME = 22
        private const val VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER = 23
        private const val VIEW_TYPE_FILE_MESSAGE_VIDEO_ME = 24
        private const val VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER = 25
        private const val VIEW_TYPE_ADMIN_MESSAGE = 30
    }

}