package com.example.sendbirddemo.ui.home.adapter

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.example.sendbirddemo.databinding.ItemRcGroupChannelBinding
import com.example.sendbirddemo.utils.SyncManagerUtils
import com.example.sendbirddemo.utils.TypingIndicator
import com.example.sendbirddemo.utils.Utils
import com.sendbird.android.*
import com.stfalcon.multiimageview.MultiImageView
import kotlinx.android.synthetic.main.layout_typing_indicator.view.*
import java.util.concurrent.ConcurrentHashMap

class GroupChannelListAdapter :
    RecyclerView.Adapter<GroupChannelListAdapter.GroupChannelViewHolder>() {

    private var mGroupChannelList = mutableListOf<GroupChannel>()

    private var mGroupChannelNumImageMap = ConcurrentHashMap<String, Int>()
    private val mSimpleTargetIndexMap = ConcurrentHashMap<SimpleTarget<Bitmap>, Int>()
    private val mSimpleTargetGroupChannelMap =
        ConcurrentHashMap<SimpleTarget<Bitmap>, GroupChannel>()
    private val mGroupChannelImageViewMap = ConcurrentHashMap<String, ImageView>()
    private val mGroupChannelBitmapMap = ConcurrentHashMap<String, SparseArray<Bitmap>>()
    private var mItemClickListener: OnItemClickListener? = null

    fun insertChannels(channels: List<GroupChannel>, order: GroupChannelListQuery.Order) {
        for (newChannel in channels) {
            val index: Int =
                SyncManagerUtils.findIndexOfChannel(mGroupChannelList, newChannel, order)
            mGroupChannelList.add(index, newChannel)
        }
        notifyItemRangeChanged(0, mGroupChannelList.size)
    }

    fun clearGroupChannelList() {
        mGroupChannelList.clear()
        notifyDataSetChanged()
    }

    fun updateGroupChannels(channels: List<GroupChannel?>) {
        for (updatedChannel in channels) {
            val index = SyncManagerUtils.getIndexOfChannel(
                mGroupChannelList,
                updatedChannel!!
            )
            if (index != -1) {
                mGroupChannelList[index] = updatedChannel
                notifyItemChanged(index)
            }
        }
    }

    fun moveGroupChannels(channels: List<GroupChannel?>, order: GroupChannelListQuery.Order?) {
        for (movedChannel in channels) {
            val fromIndex = SyncManagerUtils.getIndexOfChannel(
                mGroupChannelList,
                movedChannel!!
            )
            val toIndex = SyncManagerUtils.findIndexOfChannel(
                mGroupChannelList,
                movedChannel, order!!
            )
            if (fromIndex != -1) {
                mGroupChannelList.removeAt(fromIndex)
                mGroupChannelList.add(toIndex, movedChannel)
                notifyItemMoved(fromIndex, toIndex)
                notifyItemChanged(toIndex)
            }
        }
    }

    fun removeGroupChannels(channels: List<GroupChannel?>) {
        for (removedChannel in channels) {
            val index = SyncManagerUtils.getIndexOfChannel(
                mGroupChannelList,
                removedChannel!!
            )
            if (index != -1) {
                mGroupChannelList.removeAt(index)
                notifyItemRemoved(index)
            }
        }
    }

    // If the channel is not in the list yet, adds it.
    // If it is, finds the channel in current list, and replaces it.
    // Moves the updated channel to the front of the list.
    fun updateOrInsert(channel: BaseChannel?) {
        if (channel !is GroupChannel) {
            return
        }
        for (i in mGroupChannelList.indices) {
            if (mGroupChannelList[i].url == channel.url) {
                mGroupChannelList.remove(mGroupChannelList[i])
                mGroupChannelList.add(0, channel)
                notifyDataSetChanged()
                Log.v(GroupChannelListAdapter::class.java.simpleName, "Channel replaced.")
                return
            }
        }
        mGroupChannelList.add(0, channel)
        notifyDataSetChanged()
    }

    fun update(channels: List<GroupChannel>) {
        for (channel in channels) {
            for (i in mGroupChannelList.indices) {
                if (mGroupChannelList[i].url == channel.url) {
                    notifyItemChanged(i)
                }
            }
        }
    }

    fun clearMap() {
        mGroupChannelList.clear()
        mSimpleTargetIndexMap.clear()
        mSimpleTargetGroupChannelMap.clear()
        mGroupChannelImageViewMap.clear()
        mGroupChannelBitmapMap.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChannelViewHolder {
        val itemRcGroupChannelBinding = ItemRcGroupChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupChannelViewHolder(itemRcGroupChannelBinding)
    }

    override fun onBindViewHolder(holder: GroupChannelViewHolder, position: Int) {
        holder.bind(mGroupChannelList[position])
    }

    override fun getItemCount() = mGroupChannelList.size

    inner class GroupChannelViewHolder(val binding: ItemRcGroupChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(groupChannel: GroupChannel) {
            binding.tvGroupChannelTopic.text = Utils.getGroupChannelTitle(
                binding.tvGroupChannelTopic.context,
                groupChannel
            )
            binding.tvGroupChannelMemberCount.text =
                groupChannel.memberCount.toString()

            setChannelImage(
                binding.imgGroupChannelCover.context,
                groupChannel,
                binding.imgGroupChannelCover
            )

            val unreadCount: Int = groupChannel.unreadMessageCount
            // If there are no unread messages, hide the unread count badge.
            if (unreadCount == 0) {
                binding.tvGroupChannelUnreadCount.visibility = View.INVISIBLE
            } else {
                binding.tvGroupChannelUnreadCount.visibility = View.VISIBLE
                binding.tvGroupChannelUnreadCount.text = groupChannel.unreadMessageCount.toString()
            }

            val lastMessage: BaseMessage? = groupChannel.lastMessage
            if (lastMessage != null) {
                binding.tvGroupChannelDate.visibility = View.VISIBLE
                binding.tvGroupChannelLastMessage.visibility = View.VISIBLE

                // Display information about the most recently sent message in the channel.
                binding.tvGroupChannelDate.text =
                    java.lang.String.valueOf(Utils.formatDateTime(lastMessage.createdAt))

                // Bind last message text according to the type of message. Specifically, if
                // the last message is a File Message, there must be special formatting.
                when (lastMessage) {
                    is UserMessage -> {
                        binding.tvGroupChannelLastMessage.text = lastMessage.message
                    }
                    is AdminMessage -> {
                        binding.tvGroupChannelLastMessage.text = lastMessage.message
                    }
                    else -> {
                        val lastMessageString: String = String.format(
                            binding.tvGroupChannelLastMessage.context.getString(com.example.sendbirddemo.R.string.group_channel_list_file_message_text),
                            (lastMessage as FileMessage).sender.nickname
                        )
                        binding.tvGroupChannelLastMessage.text = lastMessageString
                    }
                }
            } else {
                binding.tvGroupChannelDate.visibility = View.INVISIBLE
                binding.tvGroupChannelLastMessage.visibility = View.INVISIBLE
            }

            /*
             * Set up the typing indicator.
             * A typing indicator is basically just three dots contained within the layout
             * that animates. The animation is implemented in the {@link TypingIndicator#animate() class}
             */

            /*
             * Set up the typing indicator.
             * A typing indicator is basically just three dots contained within the layout
             * that animates. The animation is implemented in the {@link TypingIndicator#animate() class}
             */
            val indicatorImages = ArrayList<ImageView>()
            indicatorImages.add(binding.mLayoutGroupChannelTypingIndicator.typing_indicator_dot_1 as ImageView)
            indicatorImages.add(binding.mLayoutGroupChannelTypingIndicator.typing_indicator_dot_2 as ImageView)
            indicatorImages.add(binding.mLayoutGroupChannelTypingIndicator.typing_indicator_dot_3 as ImageView)

            val indicator = TypingIndicator(indicatorImages, 600)
            indicator.animate()

            // debug
//            typingIndicatorContainer.setVisibility(View.VISIBLE);
//            lastMessageText.setText(("Someone is typing"));

            // If someone in the channel is typing, display the typing indicator.

            // debug
//            typingIndicatorContainer.setVisibility(View.VISIBLE);
//            lastMessageText.setText(("Someone is typing"));

            // If someone in the channel is typing, display the typing indicator.
            if (groupChannel.isTyping) {
                binding.mLayoutGroupChannelTypingIndicator.visibility = View.VISIBLE
                binding.tvGroupChannelLastMessage.text =
                    binding.tvGroupChannelLastMessage.context.getString(com.example.sendbirddemo.R.string.typing_status)
            } else {
                // Display typing indicator only when someone is typing
                binding.mLayoutGroupChannelTypingIndicator.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                mItemClickListener?.onItemClick(groupChannel)
            }
            binding.root.setOnLongClickListener(View.OnLongClickListener {
                mItemClickListener?.onItemLongClick(groupChannel)
                return@OnLongClickListener true
            })

        }

        private fun setChannelImage(
            context: Context,
            channel: GroupChannel,
            multiImageView: MultiImageView
        ) {
            val members = channel.members
            val size = members.size
            if (size >= 1) {
                var imageNum = size
                if (size >= 4) {
                    imageNum = 4
                }
                if (!mGroupChannelNumImageMap.containsKey(channel.url)) {
                    mGroupChannelNumImageMap[channel.url] = imageNum
                    mGroupChannelImageViewMap[channel.url] = multiImageView
                    multiImageView.clear()
                    for (index in 0 until imageNum) {
                        val simpleTarget: SimpleTarget<Bitmap> = object : SimpleTarget<Bitmap>() {
                            override fun onResourceReady(
                                bitmap: Bitmap,
                                glideAnimation: Transition<in Bitmap>?
                            ) {
                                val channel: GroupChannel? = mSimpleTargetGroupChannelMap[this]
                                val index: Int? = mSimpleTargetIndexMap[this]
                                if (channel != null && index != null) {
                                    var bitmapSparseArray: SparseArray<Bitmap>? =
                                        mGroupChannelBitmapMap[channel.url]
                                    if (bitmapSparseArray == null) {
                                        bitmapSparseArray = SparseArray()
                                        mGroupChannelBitmapMap[channel.url] = bitmapSparseArray
                                    }
                                    bitmapSparseArray.put(index, bitmap)
                                    val num: Int? = mGroupChannelNumImageMap[channel.url]
                                    if (num != null && num == bitmapSparseArray.size()) {
                                        val multiImageView =
                                            mGroupChannelImageViewMap[channel.url] as MultiImageView
                                        if (multiImageView != null) {
                                            for (i in 0 until bitmapSparseArray.size()) {
                                                multiImageView.addImage(bitmapSparseArray[i]!!)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        mSimpleTargetIndexMap[simpleTarget] = index
                        mSimpleTargetGroupChannelMap[simpleTarget] = channel
                        val myOptions = RequestOptions()
                            .dontAnimate()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        Glide.with(context)
                            .asBitmap()
                            .load(members[index].profileUrl)
                            .apply(myOptions)
                            .into(simpleTarget)
                        Log.d("TAG", "setChannelImage: ${members[index].profileUrl}")
                    }
                } else {
                    val bitmapSparseArray: SparseArray<Bitmap>? =
                        mGroupChannelBitmapMap[channel.url]
                    if (bitmapSparseArray != null) {
                        val num: Int? = mGroupChannelNumImageMap[channel.url]
                        if (num != null && num == bitmapSparseArray.size()) {
                            multiImageView.clear()
                            for (i in 0 until bitmapSparseArray.size()) {
                                multiImageView.addImage(bitmapSparseArray[i])
                            }
                        }
                        Log.d("TAG", "setChannelImage: ${mGroupChannelNumImageMap[channel.url]}")
                    }
                }
            }
        }
    }

    fun setOnItemClickListener(onItemClicked: OnItemClickListener) {
        mItemClickListener = onItemClicked
    }

    interface OnItemClickListener {
        fun onItemClick(channel: GroupChannel?)
        fun onItemLongClick(channel: GroupChannel?)
    }
}