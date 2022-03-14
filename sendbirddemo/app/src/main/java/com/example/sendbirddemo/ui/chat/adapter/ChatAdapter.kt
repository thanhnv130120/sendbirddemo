package com.example.sendbirddemo.ui.chat.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.*
import com.example.sendbirddemo.utils.UrlPreviewInfo
import com.example.sendbirddemo.utils.Utils
import com.sendbird.android.*
import com.sendbird.android.FileMessage.Thumbnail
import kotlinx.android.synthetic.main.partial_group_chat_info.view.*
import org.json.JSONException
import java.util.*

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mGroupChannel: GroupChannel? = null
    private val mMessageList = mutableListOf<BaseMessage>()
    private val mFailedMessageList = mutableListOf<BaseMessage>()
    private val mResendingMessageSet: Set<String>? = null
    private val mTempFileMessageUriTable = Hashtable<String, Uri>()

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

    fun isTempMessage(message: BaseMessage): Boolean {
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
        } else mResendingMessageSet!!.contains(getRequestId(message))
    }

    private fun getRequestId(message: BaseMessage): String? {
        if (message is UserMessage) {
            return message.requestId
        } else if (message is FileMessage) {
            return message.requestId
        }
        return ""
    }

    fun getTempFileMessageUri(message: BaseMessage?): Uri? {
        if (!isTempMessage(message!!)) {
            return null
        }
        return if (message !is FileMessage) {
            null
        } else mTempFileMessageUriTable.get(message.requestId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER_MESSAGE_ME -> {
                val myUserMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_user_me, parent, false)
                MyUserMessageHolder(
                    myUserMsgView
                )
            }
            VIEW_TYPE_USER_MESSAGE_OTHER -> {
                val otherUserMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_user_other, parent, false)
                OtherUserMessageHolder(
                    otherUserMsgView
                )
            }
            VIEW_TYPE_ADMIN_MESSAGE -> {
                val adminMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_admin, parent, false)
                AdminMessageHolder(
                    adminMsgView
                )
            }
            VIEW_TYPE_FILE_MESSAGE_ME -> {
                val myFileMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_file_me, parent, false)
                MyFileMessageHolder(
                    myFileMsgView
                )
            }
            VIEW_TYPE_FILE_MESSAGE_OTHER -> {
                val otherFileMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_file_other, parent, false)
                OtherFileMessageHolder(
                    otherFileMsgView
                )
            }
            VIEW_TYPE_FILE_MESSAGE_IMAGE_ME -> {
                val myImageFileMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_file_image_me, parent, false)
                MyImageFileMessageHolder(
                    myImageFileMsgView
                )
            }
            VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER -> {
                val otherImageFileMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_file_image_other, parent, false)
                OtherImageFileMessageHolder(
                    otherImageFileMsgView
                )
            }
            VIEW_TYPE_FILE_MESSAGE_VIDEO_ME -> {
                val myVideoFileMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_file_video_me, parent, false)
                MyVideoFileMessageHolder(
                    myVideoFileMsgView
                )
            }
            VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER -> {
                val otherVideoFileMsgView: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_group_chat_file_video_other, parent, false)
                OtherVideoFileMessageHolder(
                    otherVideoFileMsgView
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
                mContext,
                message as UserMessage,
                mChannel,
                isContinuous,
                isNewDay,
                mItemClickListener,
                mItemLongClickListener,
                position
            )
             VIEW_TYPE_USER_MESSAGE_OTHER -> (holder as  OtherUserMessageHolder).bind(
                mContext,
                message as UserMessage,
                mChannel,
                isNewDay,
                isContinuous,
                mItemClickListener,
                mItemLongClickListener,
                position
            )
             VIEW_TYPE_ADMIN_MESSAGE -> (holder as  AdminMessageHolder).bind(
                mContext,
                message as AdminMessage,
                mChannel,
                isNewDay
            )
             VIEW_TYPE_FILE_MESSAGE_ME -> (holder as  MyFileMessageHolder).bind(
                mContext,
                message as FileMessage,
                mChannel,
                isNewDay,
                mItemClickListener
            )
             VIEW_TYPE_FILE_MESSAGE_OTHER -> (holder as  OtherFileMessageHolder).bind(
                mContext,
                message as FileMessage,
                mChannel,
                isNewDay,
                isContinuous,
                mItemClickListener
            )
             VIEW_TYPE_FILE_MESSAGE_IMAGE_ME -> (holder as  MyImageFileMessageHolder).bind(
                mContext,
                message as FileMessage,
                mChannel,
                isNewDay,
                isTempMessage,
                tempFileMessageUri,
                mItemClickListener
            )
             VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER -> (holder as  OtherImageFileMessageHolder).bind(
                mContext,
                message as FileMessage,
                mChannel,
                isNewDay,
                isContinuous,
                mItemClickListener
            )
             VIEW_TYPE_FILE_MESSAGE_VIDEO_ME -> (holder as  MyVideoFileMessageHolder).bind(
                mContext,
                message as FileMessage,
                mChannel,
                isNewDay,
                isTempMessage,
                tempFileMessageUri,
                mItemClickListener
            )
             VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER -> (holder as  OtherVideoFileMessageHolder).bind(
                mContext,
                message as FileMessage,
                mChannel,
                isNewDay,
                isContinuous,
                mItemClickListener
            )
            else -> {}
        }
    }

    override fun getItemCount(): Int {

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
            context: Context?,
            message: UserMessage,
            channel: GroupChannel?,
            isContinuous: Boolean,
            isNewDay: Boolean
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
                        context,
                        previewInfo.mImageUrl,
                        binding.imgUrlPreviewMain
                    )
                } catch (e: JSONException) {
                    binding.mLayoutUrlPreview.visibility = View.GONE
                    e.printStackTrace()
                }
            }
            binding.mLayoutMessageStatus.drawMessageStatus(channel, message)
        }
    }

    private class OtherUserMessageHolder(val binding: ListItemGroupChatUserOtherBinding) : BaseViewHolder(binding.root) {
        fun bind(
            context: Context?,
            message: UserMessage,
            channel: GroupChannel?,
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
                        context,
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

    private class MyFileMessageHolder(val binding: ListItemGroupChatFileMeBinding) : BaseViewHolder(binding.root) {
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

    private class OtherFileMessageHolder(val binding: ListItemGroupChatFileOtherBinding) : BaseViewHolder(binding.root) {
        fun bind(
            context: Context?,
            message: FileMessage,
            channel: GroupChannel?,
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
                ImageUtils.displayRoundImageFromUrl(
                    context,
                    message.sender.profileUrl,
                    profileImage
                )
                nicknameText.visibility = View.VISIBLE
                nicknameText.text = message.sender.nickname
            }
            if (listener != null) {
                itemView.setOnClickListener { listener.onFileMessageItemClick(message) }
            }
        }

        init {
            nicknameText = itemView.findViewById<View>(R.id.text_group_chat_nickname) as TextView
            timeText = itemView.findViewById<View>(R.id.text_group_chat_time) as TextView
            fileNameText = itemView.findViewById<View>(R.id.text_group_chat_file_name) as TextView
            //            fileSizeText = (TextView) itemView.findViewById(R.id.text_group_chat_file_size);
            profileImage = itemView.findViewById<View>(R.id.image_group_chat_profile) as ImageView
        }
    }

    /**
     * A ViewHolder for file messages that are images.
     * Displays only the image thumbnail.
     */
    private class MyImageFileMessageHolder(itemView: View) : BaseViewHolder(itemView) {
        var timeText: TextView
        var fileThumbnailImage: ImageView
        var messageStatusView: MessageStatusView
        fun bind(
            context: Context?,
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean,
            isTempMessage: Boolean,
            tempFileMessageUri: Uri?,
            listener: OnItemClickListener?
        ) {
            super.bind(message, isNewDay)
            timeText.setText(DateUtils.formatTime(message.createdAt))
            if (isTempMessage && tempFileMessageUri != null) {
                ImageUtils.displayImageFromUrl(
                    context,
                    tempFileMessageUri.toString(),
                    fileThumbnailImage
                )
            } else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    if (message.type.toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(
                            context,
                            message.url,
                            fileThumbnailImage,
                            thumbnails[0].url
                        )
                    } else {
                        ImageUtils.displayImageFromUrl(
                            context,
                            thumbnails[0].url,
                            fileThumbnailImage
                        )
                    }
                } else {
                    if (message.type.toLowerCase().contains("gif")) {
                        ImageUtils.displayGifImageFromUrl(
                            context,
                            message.url,
                            fileThumbnailImage,
                            null as String?
                        )
                    } else {
                        ImageUtils.displayImageFromUrl(context, message.url, fileThumbnailImage)
                    }
                }
            }
            if (listener != null) {
                itemView.setOnClickListener { listener.onFileMessageItemClick(message) }
            }
            messageStatusView.drawMessageStatus(channel, message)
        }

        init {
            timeText = itemView.findViewById<View>(R.id.text_group_chat_time) as TextView
            fileThumbnailImage =
                itemView.findViewById<View>(R.id.image_group_chat_file_thumbnail) as ImageView
            messageStatusView = itemView.findViewById(R.id.message_status_group_chat)
        }
    }

    private class OtherImageFileMessageHolder(itemView: View) : BaseViewHolder(itemView) {
        var timeText: TextView
        var nicknameText: TextView
        var profileImage: ImageView
        var fileThumbnailImage: ImageView
        fun bind(
            context: Context?,
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean,
            isContinuous: Boolean,
            listener: OnItemClickListener?
        ) {
            super.bind(message, isNewDay)
            timeText.setText(DateUtils.formatTime(message.createdAt))

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
                profileImage.visibility = View.INVISIBLE
                nicknameText.visibility = View.GONE
            } else {
                profileImage.visibility = View.VISIBLE
                ImageUtils.displayRoundImageFromUrl(
                    context,
                    message.sender.profileUrl,
                    profileImage
                )
                nicknameText.visibility = View.VISIBLE
                nicknameText.text = message.sender.nickname
            }

            // Get thumbnails from FileMessage
            val thumbnails = message.thumbnails as ArrayList<Thumbnail>

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size > 0) {
                if (message.type.toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(
                        context,
                        message.url,
                        fileThumbnailImage,
                        thumbnails[0].url
                    )
                } else {
                    ImageUtils.displayImageFromUrl(context, thumbnails[0].url, fileThumbnailImage)
                }
            } else {
                if (message.type.toLowerCase().contains("gif")) {
                    ImageUtils.displayGifImageFromUrl(
                        context,
                        message.url,
                        fileThumbnailImage,
                        null as String?
                    )
                } else {
                    ImageUtils.displayImageFromUrl(context, message.url, fileThumbnailImage)
                }
            }
            if (listener != null) {
                itemView.setOnClickListener { listener.onFileMessageItemClick(message) }
            }
        }

        init {
            timeText = itemView.findViewById<View>(R.id.text_group_chat_time) as TextView
            nicknameText = itemView.findViewById<View>(R.id.text_group_chat_nickname) as TextView
            fileThumbnailImage =
                itemView.findViewById<View>(R.id.image_group_chat_file_thumbnail) as ImageView
            profileImage = itemView.findViewById<View>(R.id.image_group_chat_profile) as ImageView
        }
    }

    /**
     * A ViewHolder for file messages that are videos.
     * Displays only the video thumbnail.
     */
    private class MyVideoFileMessageHolder(itemView: View) : BaseViewHolder(itemView) {
        var timeText: TextView
        var fileThumbnailImage: ImageView
        var messageStatusView: MessageStatusView
        fun bind(
            context: Context?,
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean,
            isTempMessage: Boolean,
            tempFileMessageUri: Uri?,
            listener: OnItemClickListener?
        ) {
            super.bind(message, isNewDay)
            timeText.setText(DateUtils.formatTime(message.createdAt))
            if (isTempMessage && tempFileMessageUri != null) {
                ImageUtils.displayImageFromUrl(
                    context,
                    tempFileMessageUri.toString(),
                    fileThumbnailImage
                )
            } else {
                // Get thumbnails from FileMessage
                val thumbnails = message.thumbnails as ArrayList<Thumbnail>

                // If thumbnails exist, get smallest (first) thumbnail and display it in the message
                if (thumbnails.size > 0) {
                    ImageUtils.displayImageFromUrl(context, thumbnails[0].url, fileThumbnailImage)
                }
            }
            if (listener != null) {
                itemView.setOnClickListener { listener.onFileMessageItemClick(message) }
            }
            messageStatusView.drawMessageStatus(channel, message)
        }

        init {
            timeText = itemView.findViewById<View>(R.id.text_group_chat_time) as TextView
            fileThumbnailImage =
                itemView.findViewById<View>(R.id.image_group_chat_file_thumbnail) as ImageView
            messageStatusView = itemView.findViewById(R.id.message_status_group_chat)
        }
    }

    private class OtherVideoFileMessageHolder(itemView: View) : BaseViewHolder(itemView) {
        var timeText: TextView
        var nicknameText: TextView
        var profileImage: ImageView
        var fileThumbnailImage: ImageView
        fun bind(
            context: Context?,
            message: FileMessage,
            channel: GroupChannel?,
            isNewDay: Boolean,
            isContinuous: Boolean,
            listener: OnItemClickListener?
        ) {
            super.bind(message, isNewDay)
            timeText.setText(DateUtils.formatTime(message.createdAt))

            // Hide profile image and nickname if the previous message was also sent by current sender.
            if (isContinuous) {
                profileImage.visibility = View.INVISIBLE
                nicknameText.visibility = View.GONE
            } else {
                profileImage.visibility = View.VISIBLE
                ImageUtils.displayRoundImageFromUrl(
                    context,
                    message.sender.profileUrl,
                    profileImage
                )
                nicknameText.visibility = View.VISIBLE
                nicknameText.text = message.sender.nickname
            }

            // Get thumbnails from FileMessage
            val thumbnails = message.thumbnails as ArrayList<Thumbnail>

            // If thumbnails exist, get smallest (first) thumbnail and display it in the message
            if (thumbnails.size > 0) {
                ImageUtils.displayImageFromUrl(context, thumbnails[0].url, fileThumbnailImage)
            }
            if (listener != null) {
                itemView.setOnClickListener { listener.onFileMessageItemClick(message) }
            }
        }

        init {
            timeText = itemView.findViewById<View>(R.id.text_group_chat_time) as TextView
            nicknameText = itemView.findViewById<View>(R.id.text_group_chat_nickname) as TextView
            fileThumbnailImage =
                itemView.findViewById<View>(R.id.image_group_chat_file_thumbnail) as ImageView
            profileImage = itemView.findViewById<View>(R.id.image_group_chat_profile) as ImageView
        }
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