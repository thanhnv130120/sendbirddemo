package com.example.sendbirddemo.ui.login

import android.util.Log
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.demojetpack.ui.base.BaseFragment
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentLoginBinding
import com.example.sendbirddemo.utils.ConnectionUtils

class LoginFragment : BaseFragment<FragmentLoginBinding>(), View.OnClickListener {

    private val connectionUtils: ConnectionUtils by lazy {
        ConnectionUtils(object : ConnectionUtils.OnConnectionListener {
            override fun onConnectFailed() {
                Log.d("TAG", "onConnectFailed: ")
            }

            override fun onConnectSuccess() {
                findNavController().navigate(R.id.action_global_homeFragment)
            }

        })
    }

    override fun getLayoutID() = R.layout.fragment_login

    override fun initView() {
        binding!!.btnConnect.setOnClickListener(this)
    }

    override fun initViewModel() {

    }

    override fun onClick(p0: View?) {
        if (p0 == binding!!.btnConnect) {
            connectionUtils.connectToSendBird(
                binding!!.edtUserId.text.toString(),
                binding!!.edtNickname.text.toString()
            )
        }
    }
}