package com.example.sendbirddemo.ui.base

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import com.example.sendbirddemo.R
import com.example.sendbirddemo.utils.Utils
import com.google.android.material.snackbar.Snackbar

abstract class BasePermissionRequestFragment<V : ViewDataBinding> : BaseFragment<V>() {

    private val customContract = object : ActivityResultContract<String, Boolean>() {
        override fun createIntent(context: Context, input: String): Intent {
            val intent = Intent(input)
            intent.addCategory("android.intent.category.DEFAULT")
            intent.data = Uri.parse(
                String.format(
                    "package:%s",
                    requireActivity().packageName
                )
            )
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean =
            if (Utils.isAndroidR()) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

    }

    private val activityResultLauncher = registerForActivityResult(customContract) {
        if (it) {
            setupWhenPermissionGranted()
        } else {
            showAlertPermissionNotGrant()
        }
    }

    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (Utils.storagePermissionGrant(requireContext())
            ) {
                setupWhenPermissionGranted()
            } else {
                showAlertPermissionNotGrant()
            }
        }

    protected fun requestPermission() {
        if (Utils.isAndroidR()) {
            activityResultLauncher.launch(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        } else resultLauncher.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }


    private fun getStoragePermissions(): String {
        return if (Utils.isAndroidR()) {
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    protected fun showAlertPermissionNotGrant() {
        if (!Utils.hasShowRequestPermissionRationale(
                requireContext(),
                getStoragePermissions()
            )
        ) {
            val snackBar = Snackbar.make(
                binding!!.root,
                getString(R.string.goto_settings),
                Snackbar.LENGTH_LONG
            )
            snackBar.setAction(
                getString(R.string.settings)
            ) {
                activityResultLauncher.launch(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            }
            snackBar.show()
        } else {
//            mainActivity.showToast(getString(R.string.grant_permission))
        }
    }

    abstract fun setupWhenPermissionGranted()

}