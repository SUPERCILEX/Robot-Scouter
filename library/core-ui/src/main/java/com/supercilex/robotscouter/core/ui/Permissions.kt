package com.supercilex.robotscouter.core.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.supercilex.robotscouter.core.RobotScouter
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

fun hasPerms(perms: Array<String>) = EasyPermissions.hasPermissions(RobotScouter, *perms)

fun FragmentActivity.requestPerms(
        perms: Array<String>,
        @StringRes rationaleId: Int,
        requestCode: Int
) {
    EasyPermissions.requestPermissions(
            this, RobotScouter.getString(rationaleId), requestCode, *perms)
}

fun Fragment.requestPerms(perms: Array<String>, @StringRes rationaleId: Int, requestCode: Int) {
    EasyPermissions.requestPermissions(
            this, RobotScouter.getString(rationaleId), requestCode, *perms)
}

fun hasPermsOnActivityResult(perms: Array<String>, requestCode: Int): Boolean {
    return requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE &&
            EasyPermissions.hasPermissions(RobotScouter, *perms)
}

fun FragmentActivity.hasPermsOnRequestPermissionsResult(
        permissions: Array<String>,
        grantResults: IntArray
): Boolean = onRequestPermissionsResult(this as Any, permissions, grantResults)

fun Fragment.hasPermsOnRequestPermissionsResult(
        permissions: Array<String>,
        grantResults: IntArray
): Boolean = onRequestPermissionsResult(this as Any, permissions, grantResults)

private fun onRequestPermissionsResult(
        host: Any,
        permissions: Array<String>,
        @Suppress("UNUSED_PARAMETER") grantResults: IntArray
): Boolean {
    if (EasyPermissions.hasPermissions(RobotScouter, *permissions)) {
        return true
    }

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

    return false
}
