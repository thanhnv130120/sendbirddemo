package com.example.sendbirddemo.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.example.sendbirddemo.R
import com.example.sendbirddemo.ui.chat.adapter.ChatAdapter
import com.sendbird.android.*
import com.sendbird.syncmanager.MessageCollection
import com.sendbird.syncmanager.MessageFilter
import java.io.File
import java.util.*

class ChatUtils {

    private var mMessageFilter = MessageFilter(BaseChannel.MessageTypeFilter.ALL, null, null)

    fun createMessageCollection(
        context: Context,
        groupChannelUrl: String,
        mLastRead: Long,
        onCreateMessageCollection: OnCreateMessageCollection
    ) {
        GroupChannel.getChannel(groupChannelUrl) { groupChannel, e ->
            if (e != null) {
                MessageCollection.create(
                    groupChannelUrl, mMessageFilter, mLastRead
                ) { messageCollection, e ->
                    if (e == null) {
                        onCreateMessageCollection.onCreateMessageCollectionSucceed(
                            groupChannel,
                            messageCollection
                        )
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.get_channel_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        onCreateMessageCollection.onCreateMessageCollectionFailed()
                    }
                }
            } else {
                onCreateMessageCollection.onCreateMessageCollectionSucceed(
                    groupChannel,
                    MessageCollection(groupChannel, mMessageFilter, mLastRead)
                )
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun sendUserMessageWithUrl(
        context: Context,
        mGroupChannel: GroupChannel?,
        mMessageCollection: MessageCollection?,
        text: String,
        url: String
    ) {
        if (mGroupChannel == null) {
            return
        }
        object : WebUtils.UrlPreviewAsyncTask() {
            override fun onPostExecute(info: UrlPreviewInfo) {
                var tempUserMessage: UserMessage? = null
                val handler =
                    BaseChannel.SendUserMessageHandler { userMessage, e ->
                        if (e != null) {
                            // Error!
                            Toast.makeText(
                                context,
                                context.getString(R.string.send_message_error, e.code, e.message),
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        mMessageCollection!!.handleSendMessageResponse(userMessage, e)
                    }
                tempUserMessage = try {
                    // Sending a message with URL preview information and custom type.
                    val jsonString: String = info.toJsonString()
                    mGroupChannel.sendUserMessage(
                        text,
                        jsonString,
                        ChatAdapter.URL_PREVIEW_CUSTOM_TYPE,
                        handler
                    )
                } catch (e: Exception) {
                    // Sending a message without URL preview information.
                    mGroupChannel.sendUserMessage(text, handler)
                }
                // Display a user message to RecyclerView
                mMessageCollection?.appendMessage(tempUserMessage)
            }
        }.execute(url)
    }

    fun sendUserMessage(
        context: Context,
        mGroupChannel: GroupChannel?,
        mMessageCollection: MessageCollection?,
        text: String
    ) {
        if (mGroupChannel == null) {
            return
        }
        val urls: List<String> = WebUtils.extractUrls(text)
        if (urls.isNotEmpty()) {
            sendUserMessageWithUrl(context, mGroupChannel, mMessageCollection, text, urls[0])
            return
        }
        val pendingMessage: UserMessage = mGroupChannel.sendUserMessage(text,
            BaseChannel.SendUserMessageHandler { userMessage, e ->
                if (mMessageCollection != null) {
                    mMessageCollection.handleSendMessageResponse(userMessage, e)
                    mMessageCollection.fetchAllNextMessages(null)
                }
                if (e != null) {
                    // Error!
                    Toast.makeText(
                        context,
                        context.getString(R.string.send_message_error, e.code, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@SendUserMessageHandler
                }
            })
        mMessageCollection?.appendMessage(pendingMessage)

    }

    /**
     * Sends a File Message containing an image file.
     * Also requests thumbnails to be generated in specified sizes.
     *
     * @param uri The URI of the image, which in this case is received through an Intent request.
     */
    fun sendFileWithThumbnail(
        context: Context?,
        mGroupChannel: GroupChannel?,
        mMessageCollection: MessageCollection?,
        uri: Uri,
        onSendFileWithThumbnailListener: OnSendFileWithThumbnailListener
    ) {
        if (mGroupChannel == null) {
            return
        }
        // Specify two dimensions of thumbnails to generate
        val thumbnailSizes: MutableList<FileMessage.ThumbnailSize> = ArrayList()
        thumbnailSizes.add(FileMessage.ThumbnailSize(240, 240))
        thumbnailSizes.add(FileMessage.ThumbnailSize(320, 320))
        val info: Hashtable<String, Any>? =
            FileUtils.getFileInfo(context, uri)
        if (info == null || info.isEmpty) {
            Toast.makeText(context, context?.getString(R.string.wrong_file_info), Toast.LENGTH_LONG)
                .show()
            return
        }
        val name: String? = if (info.containsKey("name")) {
            info["name"] as String?
        } else {
            "Sendbird File"
        }
        val path = info["path"] as String?
        val file = File(path)
        val mime = info["mime"] as String?
        val size = info["size"] as Int
        if (path == null || path == "") {
            Toast.makeText(context, context?.getString(R.string.wrong_file_path), Toast.LENGTH_LONG)
                .show()
        } else {
            val fileMessageHandler =
                BaseChannel.SendFileMessageHandler { fileMessage, e ->
                    mMessageCollection!!.handleSendMessageResponse(fileMessage, e)
                    mMessageCollection.fetchAllNextMessages(null)
                    if (e != null) {
                        onSendFileWithThumbnailListener.onSendFileWithThumbnailFailed()
                    }
                }

            // Send image with thumbnails in the specified dimensions
            val tempFileMessage: FileMessage = mGroupChannel.sendFileMessage(
                file,
                name,
                mime,
                size,
                "",
                null,
                thumbnailSizes,
                fileMessageHandler
            )
//            mChatAdapter?.addTempFileMessageInfo(tempFileMessage, uri)
            mMessageCollection?.appendMessage(tempFileMessage)
            onSendFileWithThumbnailListener.onSendFileWithThumbnailSucceed(tempFileMessage, uri)
        }
    }

    /**
     * Deletes a message within the channel.
     * Note that users can only delete messages sent by oneself.
     *
     * @param message The message to delete.
     */
    fun deleteMessage(
        context: Context?,
        mGroupChannel: GroupChannel?,
        mMessageCollection: MessageCollection?,
        message: BaseMessage
    ) {
        if (message.messageId == 0L) {
            mMessageCollection!!.deleteMessage(message)
        } else {
            if (mGroupChannel == null) {
                return
            }
            mGroupChannel.deleteMessage(message, BaseChannel.DeleteMessageHandler { e ->
                if (e != null) {
                    // Error!
                    Toast.makeText(
                        context,
                        context?.getString(R.string.sendbird_error_with_code, e.code, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@DeleteMessageHandler
                }
                mMessageCollection!!.deleteMessage(message)
            })
        }
    }

    interface OnCreateMessageCollection {
        fun onCreateMessageCollectionFailed()
        fun onCreateMessageCollectionSucceed(
            groupChannel: GroupChannel,
            messageCollection: MessageCollection
        )
    }

    interface OnSendFileWithThumbnailListener {
        fun onSendFileWithThumbnailFailed()
        fun onSendFileWithThumbnailSucceed(tempFileMessage: FileMessage, uri: Uri)
    }

}