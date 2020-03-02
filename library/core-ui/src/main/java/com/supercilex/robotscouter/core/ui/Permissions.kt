package com.supercilex.robotscouter.core.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.supercilex.robotscouter.core.RobotScouter
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

private const val RC = 8653

fun hasPerms(perms: Array<String>) = EasyPermissions.hasPermissions(RobotScouter, *perms)

fun FragmentActivity.requestPerms(perms: Array<String>, @StringRes rationaleId: Int) {
    EasyPermissions.requestPermissions(this, RobotScouter.getString(rationaleId), RC, *perms)
}

fun Fragment.requestPerms(perms: Array<String>, @StringRes rationaleId: Int) {
    EasyPermissions.requestPermissions(this, RobotScouter.getString(rationaleId), RC, *perms)
}

fun hasPermsOnActivityResult(perms: Array<String>, requestCode: Int): Boolean {
    return requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE &&
            EasyPermissions.hasPermissions(RobotScouter, *perms)
}

fun FragmentActivity.hasPermsOnRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
): Boolean = onRequestPermissionsResult(this as Any, requestCode, permissions)

fun Fragment.hasPermsOnRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
): Boolean = onRequestPermissionsResult(this as Any, requestCode, permissions)

private fun onRequestPermissionsResult(
        host: Any,
        requestCode: Int,
        permissions: Array<String>
): Boolean {
    if (EasyPermissions.hasPermissions(RobotScouter, *permissions)) {
        return true
    }

    if (requestCode == RC) {
        if (host is FragmentActivity) {
            if (EasyPermissions.somePermissionPermanentlyDenied(host, permissions.toList())) {
                AppSettingsDialog.Builder(host).build().show()
            }
        } else if (host is Fragment) {
            if (EasyPermissions.somePermissionPermanentlyDenied(host, permissions.toList())) {
                AppSettingsDialog.Builder(host).build().show()
            }
        } else {
            error("Unknown type: $host")
        }
    }

    return false
}
