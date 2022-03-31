package com.example.sendbirddemo.ui.member.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.databinding.ListItemUserBinding
import com.sendbird.android.SendBird
import com.sendbird.android.User

class MemberListAdapter : RecyclerView.Adapter<MemberListAdapter.MemberListViewHolder>() {

    private val mUsers = mutableListOf<User>()

    fun setUserList(users: MutableList<User>) {
        mUsers.clear()
        mUsers.addAll(users)
        notifyItemRangeInserted(0, mUsers.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberListViewHolder {
        val listItemUserBinding = ListItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberListViewHolder(listItemUserBinding)
    }

    override fun onBindViewHolder(holder: MemberListViewHolder, position: Int) {
        holder.bind(mUsers[position])
    }

    override fun getItemCount() = mUsers.size

    inner class MemberListViewHolder(val binding: ListItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvNickname.text = user.nickname
            if (SendBird.getCurrentUser() != null && SendBird.getCurrentUser().userId == user.userId) {
                binding.mLayoutBlock.visibility = View.GONE
                binding.tvBlock.visibility = View.GONE
            } else {
                binding.mLayoutBlock.visibility = View.VISIBLE
            }
        }
    }
}