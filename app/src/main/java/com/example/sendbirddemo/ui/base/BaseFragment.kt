package com.example.sendbirddemo.ui.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.sendbird.android.SendBird
import com.sendbird.android.SendBird.ConnectionHandler
import com.sendbird.syncmanager.SendBirdSyncManager

abstract class BaseFragment<V : ViewDataBinding> : Fragment() {
    protected var binding: V? = null

    protected open fun getConnectionHandlerId(): String {
        return "CONNECTION_HANDLER_MAIN_ACTIVITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, getLayoutID(), container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.lifecycleOwner = this
        initView()
    }

    override fun onResume() {
        super.onResume()
        registerConnectionHandler()
    }

    protected fun hideKeyboard() {
        val view = requireActivity().currentFocus
        if (view != null) {
            val imm =
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun registerConnectionHandler() {
        SendBird.addConnectionHandler(getConnectionHandlerId(), object : ConnectionHandler {
            override fun onReconnectStarted() {
                SendBirdSyncManager.getInstance().pauseSync()
            }

            override fun onReconnectSucceeded() {
                SendBirdSyncManager.getInstance().resumeSync()
            }

            override fun onReconnectFailed() {}
        })
    }

    abstract fun getLayoutID(): Int
    abstract fun initView()
    abstract fun initViewModel()

    override fun onPause() {
        super.onPause()
        SendBird.removeChannelHandler(getConnectionHandlerId())
    }

    override fun onDestroy() {
        super.onDestroy()
        binding?.unbind()
    }
}