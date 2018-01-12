package com.supercilex.robotscouter.util.ui

import android.arch.lifecycle.LiveData
import android.content.Intent
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.data.ViewModelBase
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

    fun requestPerms(host: Fragment, @StringRes rationaleId: Int) {
        EasyPermissions.requestPermissions(
                host, RobotScouter.getString(rationaleId), WRITE_RC, *perms.toTypedArray())
    }

    fun onRequestPermissionsResult(
            host: Fragment,
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
                    if (requestCode == WRITE_RC
                            && EasyPermissions.somePermissionPermanentlyDenied(host, perms)) {
                        AppSettingsDialog.Builder(host).build().show()
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
