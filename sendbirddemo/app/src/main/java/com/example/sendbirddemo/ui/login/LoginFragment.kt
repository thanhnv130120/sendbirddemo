package com.example.sendbirddemo.ui.login

import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentLoginBinding
import com.example.sendbirddemo.utils.ConnectionUtils
import com.example.sendbirddemo.utils.SharedPreferenceUtils
import com.example.sendbirddemo.utils.SyncManagerUtils
import com.sendbird.android.SendBird
import com.sendbird.android.SendBirdException
import com.sendbird.android.User
import com.sendbird.syncmanager.handler.CompletionHandler

class LoginFragment : BaseFragment<FragmentLoginBinding>(), View.OnClickListener {

    private val connectionUtils: ConnectionUtils by lazy {
        ConnectionUtils()
    }

    override fun getLayoutID() = R.layout.fragment_login

    override fun initView() {
        binding!!.btnConnect.setOnClickListener(this)
    }

    override fun initViewModel() {

    }

    override fun onClick(p0: View?) {
        if (p0 == binding!!.btnConnect) {
            var userId = binding!!.edtUserId.text.toString()
            userId = userId.replace("\\s".toRegex(), "")
            SharedPreferenceUtils.getInstance(requireContext())?.setUserId(userId)
            SharedPreferenceUtils.getInstance(requireContext())
                ?.setNickname(binding!!.edtNickname.text.toString())
            connectionUtils.connectToSendBird(
                requireContext(),
                userId,
                binding!!.edtNickname.text.toString(),
                object : SendBird.ConnectHandler {
                    override fun onConnected(p0: User?, p1: SendBirdException?) {
                        if (p1 == null) {
                            SyncManagerUtils.setup(requireContext(),
                                SharedPreferenceUtils.getInstance(requireContext())?.getUserId()!!,
                                CompletionHandler { e ->
                                    if (e != null) {
                                        e.printStackTrace()
                                        return@CompletionHandler
                                    }
                                    SharedPreferenceUtils.getInstance(requireContext())
                                        ?.setConnected(true)
                                    findNavController().navigate(R.id.action_global_homeFragment)
                                })
                        } else {
                            Log.d("TAG", "onConnectFailed: ")
                            SharedPreferenceUtils.getInstance(requireContext())?.setConnected(false)
                        }
                    }

                }
            )
        }
    }
}