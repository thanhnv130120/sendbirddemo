package com.example.sendbirddemo.ui.selectuser.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sendbirddemo.databinding.ItemRcSelectableUserBinding
import com.example.sendbirddemo.utils.Utils
import com.sendbird.android.User

class SelectableUserAdapter :
    RecyclerView.Adapter<SelectableUserAdapter.SelectableUserViewHolder>() {

    private var users = mutableListOf<User>()
    private var mSelectedUserIds = mutableListOf<String>()
    private var mCheckedChangeListener: OnItemCheckedChangeListener? = null

    fun setUserList(userList: MutableList<User>) {
        users = userList
        notifyItemRangeInserted(0, users.size)
    }

    fun addLast(user: User) {
        users.add(user)
        notifyItemInserted(users.indexOf(user))
    }

    fun getUsers(): MutableList<User> {
        return users
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableUserViewHolder {
        val itemRcSelectableUserBinding = ItemRcSelectableUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SelectableUserViewHolder(itemRcSelectableUserBinding)
    }

    override fun onBindViewHolder(holder: SelectableUserViewHolder, position: Int) {
        holder.bind(users[position], isSelected(users[position]))
    }

    override fun getItemCount() = users.size

    inner class SelectableUserViewHolder(val binding: ItemRcSelectableUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User, isSelected: Boolean) {
            binding.tvSelectableUserNickname.text = user.nickname
            Utils.displayRoundImageFromUrl(
                binding.imgSelectableUser.context,
                user.profileUrl,
                binding.imgSelectableUser
            )
            binding.cbSelectableUser.visibility = View.VISIBLE
            binding.cbSelectableUser.isChecked = isSelected

            itemView.setOnClickListener {
                binding.cbSelectableUser.isChecked = !binding.cbSelectableUser.isChecked
            }

            binding.cbSelectableUser.setOnCheckedChangeListener { buttonView, isChecked ->
                mCheckedChangeListener?.OnItemChecked(user, isChecked)
                if (isChecked) {
                    mSelectedUserIds.add(
                        user.userId
                    )
                } else {
                    mSelectedUserIds.remove(
                        user.userId
                    )
                }
            }
        }
    }

    fun setItemCheckedChangeListener(listener: OnItemCheckedChangeListener) {
        mCheckedChangeListener = listener
    }

    fun isSelected(user: User): Boolean {
        return mSelectedUserIds.contains(
            user.userId
        )
    }

    interface OnItemCheckedChangeListener {
        fun OnItemChecked(user: User?, checked: Boolean)
    }
}