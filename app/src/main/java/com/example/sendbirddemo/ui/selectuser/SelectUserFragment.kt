package com.example.sendbirddemo.ui.selectuser

import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sendbirddemo.R
import com.example.sendbirddemo.data.LoadDataStatus
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.databinding.FragmentSelectUserBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.ui.selectuser.adapter.SelectableUserAdapter
import com.example.sendbirddemo.ui.splash.SplashViewModel
import com.example.sendbirddemo.utils.GroupUtils
import com.sendbird.android.ApplicationUserListQuery
import com.sendbird.android.SendBird
import com.sendbird.android.User
import com.sendbird.android.UserListQuery.UserListQueryResultHandler

class SelectUserFragment : BaseFragment<FragmentSelectUserBinding>() {

    private lateinit var viewModel: SelectUserViewModel
    private val mSelectableUserAdapter by lazy {
        SelectableUserAdapter()
    }
    private var mLayoutManager: LinearLayoutManager? = null

    override fun getLayoutID() = R.layout.fragment_select_user

    override fun initView() {
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

        if (SendBird.getConnectionState() == SendBird.ConnectionState.OPEN) {
            setUpRecyclerView()
            viewModel.onLoadInitialUserList()
        }

        binding!!.viewModel = viewModel
    }

    override fun initViewModel() {
        val factory = SelectUserViewModel.Factory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[SelectUserViewModel::class.java]

        viewModel.mGroupCreatedLiveData.observe(this) {
            if (it.loadDataStatus == LoadDataStatus.SUCCESS) {
                val result = (it as DataResponse.DataSuccessResponse).body
                val action = SelectUserFragmentDirections.actionGlobalChatFragment()
                    .setGroupChannelUrl(result)
                findNavController().navigate(action)
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

    private fun setUpRecyclerView() {
        mLayoutManager = LinearLayoutManager(activity)
        binding!!.rcSelectUser.apply {
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