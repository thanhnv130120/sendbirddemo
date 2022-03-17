package com.example.sendbirddemo.ui.member

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentMemberListBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.member.adapter.MemberListAdapter
import com.example.sendbirddemo.utils.Constants
import com.example.sendbirddemo.utils.SyncManagerUtils
import com.sendbird.android.BaseChannel
import com.sendbird.android.GroupChannel
import com.sendbird.android.Member

class MemberListFragment : BaseFragment<FragmentMemberListBinding>() {

    private var mLayoutManager: LinearLayoutManager? = null
    private val mMemberListAdapter: MemberListAdapter by lazy {
        MemberListAdapter()
    }
    private var mGroupChannel: GroupChannel? = null

    override fun getLayoutID() = R.layout.fragment_member_list

    override fun initView() {
        (activity as AppCompatActivity).setSupportActionBar(binding!!.mToolbarMemberList)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mGroupChannel =
            BaseChannel.buildFromSerializedData(arguments?.getByteArray(Constants.GROUP_CHANNEL_KEY)) as GroupChannel
        setMemberList(mGroupChannel!!.members)
        setUpRecyclerView()
    }

    override fun initViewModel() {
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(requireContext())
        binding!!.rcMemberList.apply {
            layoutManager = mLayoutManager
            adapter = mMemberListAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun setMemberList(memberList: List<Member>) {
        val sortedUserList: MutableList<Member> = ArrayList()
        val myUserId: String? = SyncManagerUtils.getMyUserId(requireContext())
        for (member in memberList) {
            if (member.userId == myUserId) {
                sortedUserList.add(0, member)
            } else {
                sortedUserList.add(member)
            }
        }
        mMemberListAdapter.setUserList(sortedUserList.toMutableList())
    }
}