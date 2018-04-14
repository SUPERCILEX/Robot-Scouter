package com.supercilex.robotscouter.shared

import android.arch.lifecycle.LiveData
import android.content.Intent
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.SingleLiveEvent
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.ui.OnActivityResult
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class PermissionRequestHandler : ViewModelBase<List<String>>(), OnActivityResult {
    private val _onGranted = SingleLiveEvent<List<String>>()
    val onGranted: LiveData<List<String>> get() = _onGranted

    lateinit var perms: List<String>
        private set

    override fun onCreate(args: List<String>) {
        perms = args.toList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
                && EasyPermissions.hasPermissions(RobotScouter, *perms.toTypedArray())) {
            _onGranted.value = perms
        }
    }

    fun requestPerms(host: FragmentActivity, @StringRes rationaleId: Int) {
        EasyPermissions.requestPermissions(
                host, RobotScouter.getString(rationaleId), WRITE_RC, *perms.toTypedArray())
    }

    fun requestPerms(host: Fragment, @StringRes rationaleId: Int) {
        EasyPermissions.requestPermissions(
                host, RobotScouter.getString(rationaleId), WRITE_RC, *perms.toTypedArray())
    }

    fun onRequestPermissionsResult(
            host: FragmentActivity,
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = onRequestPermissionsResult(host as Any, requestCode, permissions, grantResults)

    fun onRequestPermissionsResult(
            host: Fragment,
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = onRequestPermissionsResult(host as Any, requestCode, permissions, grantResults)

    private fun onRequestPermissionsResult(
            host: Any,
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            object : EasyPermissions.PermissionCallbacks {
                override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
                    if (requestCode == WRITE_RC) _onGranted.value = perms
                }

                override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
                    if (requestCode == WRITE_RC) {
                        if (host is FragmentActivity) {
                            if (EasyPermissions.somePermissionPermanentlyDenied(host, perms)) {
                                AppSettingsDialog.Builder(host).build().show()
                            }
                        } else if (host is Fragment) {
                            if (EasyPermissions.somePermissionPermanentlyDenied(host, perms)) {
                                AppSettingsDialog.Builder(host).build().show()
                            }
                        } else {
                            error("Unknown type: $host")
                        }
                    }
                }

                override fun onRequestPermissionsResult(
                        requestCode: Int,
                        permissions: Array<out String>,
                        grantResults: IntArray
                ) = Unit
            }
    )

    private companion object {
        const val WRITE_RC = 8653
    }
}
