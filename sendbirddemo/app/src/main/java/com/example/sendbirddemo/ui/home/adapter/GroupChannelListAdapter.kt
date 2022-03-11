package com.example.sendbirddemo.ui.home.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.databinding.ItemRcGroupChannelBinding
import com.example.sendbirddemo.utils.SyncManagerUtils
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import com.sendbird.android.GroupChannelListQuery

class GroupChannelListAdapter :
    RecyclerView.Adapter<GroupChannelListAdapter.GroupChannelViewHolder>() {

    private var mGroupChannelList = mutableListOf<GroupChannel>()

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
        val groupChannel = channel
        for (i in mGroupChannelList.indices) {
            if (mGroupChannelList[i].url == groupChannel.url) {
                mGroupChannelList.remove(mGroupChannelList[i])
                mGroupChannelList.add(0, groupChannel)
                notifyDataSetChanged()
                Log.v(GroupChannelListAdapter::class.java.simpleName, "Channel replaced.")
                return
            }
        }
        mGroupChannelList.add(0, groupChannel)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChannelViewHolder {
        val itemRcGroupChannelBinding = ItemRcGroupChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupChannelViewHolder(itemRcGroupChannelBinding)
    }

    override fun onBindViewHolder(holder: GroupChannelViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount() = mGroupChannelList.size

    inner class GroupChannelViewHolder(val binding: ItemRcGroupChannelBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {

        }
    }
}