package com.supercilex.robotscouter.util;

import android.Manifest;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks;

public final class IoHelper {
    public static final List<String> WRITE_PERMS = Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final String ROOT_DIRECTORY = Environment.getExternalStorageDirectory() + "/Robot Scouter/";
    private static final File ROOT_FOLDER = new File(ROOT_DIRECTORY);
    private static final File MEDIA_FOLDER = new File(ROOT_FOLDER, "Media");

    private IoHelper() {
        throw new AssertionError("No instance for you!");
    }

    @Nullable
    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static File getRootFolder() {
        return isExternalStorageMounted() ? !ROOT_FOLDER.exists() && !ROOT_FOLDER.mkdirs() ? null : ROOT_FOLDER : null;
    }

    @Nullable
    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static File getMediaFolder() {
        //noinspection MissingPermission
        return getRootFolder() == null ? null : !MEDIA_FOLDER.exists() && !MEDIA_FOLDER.mkdirs() ? null : MEDIA_FOLDER;
    }

    private static boolean isExternalStorageMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static final class RequestHandler implements PermissionCallbacks {
        public static final int WRITE_RC = 8653;
        private Fragment mFragment;
        private OnSuccessListener<Void> mListener;

        public RequestHandler(Fragment fragment, OnSuccessListener<Void> listener) {
            mFragment = fragment;
            mListener = listener;
        }

        public void onActivityResult(int requestCode) {
            if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE
                    && EasyPermissions.hasPermissions(mFragment.getContext(),
                                                      WRITE_PERMS.toArray(new String[WRITE_PERMS.size()]))) {
                mListener.onSuccess(null);
            }
        }

        public void requestPerms(@StringRes int rationaleId) {
            EasyPermissions.requestPermissions(mFragment,
                                               mFragment.getString(rationaleId),
                                               WRITE_RC,
                                               WRITE_PERMS.toArray(new String[WRITE_PERMS.size()]));
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
}
