package com.example.sendbirddemo.ui.invite

import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.data.LoadDataStatus
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.databinding.FragmentInviteMemberBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.login.LoginViewModel
import com.example.sendbirddemo.ui.selectuser.adapter.SelectableUserAdapter
import com.example.sendbirddemo.utils.GroupUtils
import com.sendbird.android.ApplicationUserListQuery
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery

class InviteMemberFragment : BaseFragment<FragmentInviteMemberBinding>() {

    private val args: InviteMemberFragmentArgs by navArgs()
    private lateinit var viewModel: InviteMemberViewModel
    private val mSelectableUserAdapter by lazy {
        SelectableUserAdapter()
    }
    private var mLayoutManager: LinearLayoutManager? = null

    override fun getLayoutID() = R.layout.fragment_invite_member

    override fun initView() {
        (activity as AppCompatActivity).setSupportActionBar(binding!!.mToolbarInviteMember)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            setUpRecyclerView()
            viewModel.onLoadInitialUserList()
        }

        mSelectableUserAdapter.setItemCheckedChangeListener(object :
            SelectableUserAdapter.OnItemCheckedChangeListener {
            override fun OnItemChecked(user: User?, checked: Boolean) {
                if (checked) {
                    viewModel.onPlusId(user)
                } else {
                    viewModel.onMinusId(user)
                }
            }

        })

        binding!!.viewModel = viewModel
    }

    override fun initViewModel() {
        setHasOptionsMenu(true)
        val factory =
            InviteMemberViewModel.Factory(requireActivity().application, args.groupChannelUrl)
        viewModel = ViewModelProvider(this, factory)[InviteMemberViewModel::class.java]

        viewModel.mInviteMembersLiveData.observe(this) {
            if (it.loadDataStatus == LoadDataStatus.SUCCESS) {
                val result = (it as DataResponse.DataSuccessResponse).body
                if (result){
                    findNavController().navigateUp()
                }
            }
        }

        viewModel.mInitialUserListLiveData.observe(this) {
            when (it.loadDataStatus) {
                LoadDataStatus.SUCCESS -> {
                    val result = (it as DataResponse.DataSuccessResponse).body
                    mSelectableUserAdapter.setUserList(result)
                }
                LoadDataStatus.ERROR -> {

                }
            }
        }

        viewModel.mNextUserListLiveData.observe(this) {
            when (it.loadDataStatus) {
                LoadDataStatus.SUCCESS -> {
                    val result = (it as DataResponse.DataSuccessResponse).body
                    mSelectableUserAdapter.addLast(result)
                }
                LoadDataStatus.ERROR -> {

                }
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
        mLayoutManager = LinearLayoutManager(activity)
        binding!!.rcInviteMember.apply {
            layoutManager = mLayoutManager
            adapter = mSelectableUserAdapter
            addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (mLayoutManager!!.findLastVisibleItemPosition() == mSelectableUserAdapter.itemCount - 1) {
                        viewModel.onLoadNextUserList()
                    }
                }
            })
        }
    }

}