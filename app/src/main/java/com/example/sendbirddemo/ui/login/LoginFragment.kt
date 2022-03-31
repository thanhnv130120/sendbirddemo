package com.example.sendbirddemo.ui.login

import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.sendbirddemo.R
import com.example.sendbirddemo.data.LoadDataStatus
import com.example.sendbirddemo.data.response.DataResponse
import com.example.sendbirddemo.databinding.FragmentLoginBinding
import com.example.sendbirddemo.ui.base.BaseFragment
import com.example.sendbirddemo.utils.ConnectionUtils
import com.example.sendbirddemo.utils.Constants

class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    private lateinit var viewModel: LoginViewModel

    override fun getLayoutID() = R.layout.fragment_login

    override fun initView() {

        binding!!.viewModel = viewModel
    }

    override fun initViewModel() {
        val factory = LoginViewModel.Factory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[LoginViewModel::class.java]

        viewModel.validateLiveData.observe(this) {
            if (it.loadDataStatus == LoadDataStatus.SUCCESS) {
                when ((it as DataResponse.DataSuccessResponse).body) {
                    Constants.ValidateType.ValidateDone -> {
                        viewModel.onLoginLiveData.observe(this) { it1 ->
                            if (it1.loadDataStatus == LoadDataStatus.SUCCESS) {
                                val result = (it1 as DataResponse.DataSuccessResponse).body
                                if (result) {
                                    findNavController().navigate(R.id.action_global_homeFragment)
                                } else {
                                    Toast.makeText(
                                        requireContext(),
                                        getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.login_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}