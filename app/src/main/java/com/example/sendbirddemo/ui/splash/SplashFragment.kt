package com.example.sendbirddemo.ui.splash

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.sendbirddemo.R
import com.example.sendbirddemo.databinding.FragmentSplashBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.utils.ConnectionUtils
import com.example.sendbirddemo.utils.SharedPreferenceUtils

class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    private val connectionUtils: ConnectionUtils by lazy {
        ConnectionUtils()
    }

    override fun getLayoutID() = R.layout.fragment_splash

    override fun initView() {
        if (connectionUtils.isLogin(requireContext()) && SharedPreferenceUtils.getInstance(requireContext())?.getUserId() != null){
            connectionUtils.setUpSyncManager(requireContext(), object : ConnectionUtils.OnSetupSyncManager{
                override fun onSetupFailed() {
                    Toast.makeText(requireContext(), "Cannot setup manager", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_global_loginFragment)
                }

                override fun onSetupSucceed() {
                    findNavController().navigate(R.id.action_global_homeFragment)
                }

            })
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                findNavController().navigate(R.id.action_global_loginFragment)
            }, SPLASH_DELAY)
        }
    }

    override fun initViewModel() {

    }

    companion object {
        const val SPLASH_DELAY = 2000L
    }
}