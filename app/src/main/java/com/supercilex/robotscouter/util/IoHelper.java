package com.supercilex.robotscouter.util;

import android.Manifest;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import java.io.File;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("MissingPermission") // TODO remove once Google fixes their plugin
public final class IoHelper {
    public static final List<String> PERMS = Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static final File ROOT_FOLDER =
            new File(Environment.getExternalStorageDirectory(), "Robot Scouter");
    private static final File MEDIA_FOLDER = new File(ROOT_FOLDER, "Media");

    private IoHelper() {
        throw new AssertionError("No instance for you!");
    }

    @Nullable
    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static File getRootFolder() {
        return isExternalStorageMounted() && (ROOT_FOLDER.exists() || ROOT_FOLDER.mkdirs()) ? ROOT_FOLDER : null;
    }

    @Nullable
    @RequiresPermission(value = Manifest.permission.WRITE_EXTERNAL_STORAGE)
    public static File getMediaFolder() {
        return getRootFolder() != null && (MEDIA_FOLDER.exists() || MEDIA_FOLDER.mkdirs()) ? MEDIA_FOLDER : null;
    }

    private static boolean isExternalStorageMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}
