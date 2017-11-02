package com.supercilex.robotscouter.util.ui

import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import com.google.android.gms.tasks.OnSuccessListener
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.Collections

class PermissionRequestHandler(
        perms: List<String>,
        private val fragment: Fragment,
        private val listener: OnSuccessListener<Nothing?>
) : EasyPermissions.PermissionCallbacks {
    val permsArray: Array<String> get() = perms.toTypedArray()

    private val perms: List<String> = Collections.unmodifiableList(perms)

    fun onActivityResult(requestCode: Int) {
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
                && EasyPermissions.hasPermissions(fragment.context, *permsArray)) {
            listener.onSuccess(null)
        }
    }

    fun requestPerms(@StringRes rationaleId: Int) = EasyPermissions.requestPermissions(
            fragment,
            fragment.getString(rationaleId),
            WRITE_RC,
            *perms.toTypedArray())

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == WRITE_RC) listener.onSuccess(null)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (requestCode == WRITE_RC
                && EasyPermissions.somePermissionPermanentlyDenied(fragment, perms)) {
            AppSettingsDialog.Builder(fragment).build().show()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)

    private companion object {
        const val WRITE_RC = 8653
    }
}
