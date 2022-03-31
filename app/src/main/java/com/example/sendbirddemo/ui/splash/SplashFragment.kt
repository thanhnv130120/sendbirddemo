package com.example.sendbirddemo.ui.splash

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.sendbirddemo.R
import com.example.sendbirddemo.data.LoadDataStatus
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.databinding.FragmentSplashBinding
import com.example.sendbirddemo.ui.base.BaseFragment

class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    private lateinit var viewModel: SplashViewModel

    override fun getLayoutID() = R.layout.fragment_splash

    override fun initView() {
        viewModel.onCheckLogin()
    }

    override fun initViewModel() {
        val factory = SplashViewModel.Factory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[SplashViewModel::class.java]

        viewModel.mCheckLoginLiveData.observe(this) {
            if (it.loadDataStatus == LoadDataStatus.SUCCESS) {
                when ((it as DataResponse.DataSuccessResponse).body) {
                    true -> {
                        findNavController().navigate(R.id.action_global_homeFragment)
                    }
                    false -> {
                        Handler(Looper.getMainLooper()).postDelayed({
                            findNavController().navigate(R.id.action_global_loginFragment)
                        }, SPLASH_DELAY)
                    }
                }
            }
        }
    }

    companion object {
        const val SPLASH_DELAY = 2000L
    }
}