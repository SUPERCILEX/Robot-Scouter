package com.supercilex.robotscouter.ui;

import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class PermissionRequestHandler implements EasyPermissions.PermissionCallbacks {
    private static final int WRITE_RC = 8653;

    private final List<String> mPerms;
    private final Fragment mFragment;
    private final OnSuccessListener<Void> mListener;

    public PermissionRequestHandler(List<String> perms,
                                    Fragment fragment,
                                    OnSuccessListener<Void> listener) {
        mPerms = Collections.unmodifiableList(perms);
        mFragment = fragment;
        mListener = listener;
    }

    public String[] getPermsArray() {
        return mPerms.toArray(new String[mPerms.size()]);
    }

    public void onActivityResult(int requestCode) {
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
                && EasyPermissions.hasPermissions(mFragment.getContext(), getPermsArray())) {
            mListener.onSuccess(null);
        }
    }

    public void requestPerms(@StringRes int rationaleId) {
        EasyPermissions.requestPermissions(mFragment,
                                           mFragment.getString(rationaleId),
                                           WRITE_RC,
                                           mPerms.toArray(new String[mPerms.size()]));
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == WRITE_RC) mListener.onSuccess(null);
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == WRITE_RC
                && EasyPermissions.somePermissionPermanentlyDenied(mFragment, perms)) {
            new AppSettingsDialog.Builder(mFragment).build().show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode,
                                                   permissions,
                                                   grantResults,
                                                   this);
    }
}
