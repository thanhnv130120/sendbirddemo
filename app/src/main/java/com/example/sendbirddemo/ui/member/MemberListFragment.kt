package com.example.sendbirddemo.ui.member

import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sendbirddemo.R
import com.example.sendbirddemo.data.LoadDataStatus
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.databinding.FragmentMemberListBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.member.adapter.MemberListAdapter
import com.example.sendbirddemo.ui.selectuser.SelectUserViewModel
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
    private lateinit var viewModel: MemberListViewModel

    override fun getLayoutID() = R.layout.fragment_member_list

    override fun initView() {
        (activity as AppCompatActivity).setSupportActionBar(binding!!.mToolbarMemberList)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mGroupChannel =
            BaseChannel.buildFromSerializedData(arguments?.getByteArray(Constants.GROUP_CHANNEL_KEY)) as GroupChannel
        viewModel.setMemberList(mGroupChannel!!.members)
        setUpRecyclerView()
    }

    override fun initViewModel() {
        setHasOptionsMenu(true)
        val factory = MemberListViewModel.Factory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[MemberListViewModel::class.java]

        viewModel.mMemberListLiveData.observe(this){
            if (it.loadDataStatus == LoadDataStatus.SUCCESS) {
                val result = (it as DataResponse.DataSuccessResponse).body
                mMemberListAdapter.setUserList(result.toMutableList())
            }
        }
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
}