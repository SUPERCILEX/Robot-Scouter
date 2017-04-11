package com.supercilex.robotscouter.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.util.IoHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import pub.devrel.easypermissions.EasyPermissions;

public final class TeamMediaCreator implements Parcelable, OnSuccessListener<Void> {
    public static final Parcelable.Creator<TeamMediaCreator> CREATOR = new Parcelable.Creator<TeamMediaCreator>() {
        @Override
        public TeamMediaCreator createFromParcel(Parcel source) {
            return new TeamMediaCreator(source.readParcelable(TeamHelper.class.getClassLoader()),
                                        source.readString());
        }

        @Override
        public TeamMediaCreator[] newArray(int size) {
            return new TeamMediaCreator[size];
        }
    };

    private static final int REQUEST_TAKE_PHOTO = 334;
    private static final String MEDIA_CREATOR_KEY = "media_creator";

    private Fragment mFragment;
    private TeamHelper mTeamHelper;
    private IoHelper.RequestHandler mWriteAccessRequestHandler;
    private OnSuccessListener<Uri> mListener;
    private String mPhotoPath;

    private TeamMediaCreator(TeamHelper teamHelper, String photoPath) {
        mTeamHelper = teamHelper;
        mPhotoPath = photoPath;
    }

    public static TeamMediaCreator newInstance(Fragment fragment,
                                               TeamHelper teamHelper,
                                               @Nullable OnSuccessListener<Uri> listener) {
        TeamMediaCreator mediaCreator = new TeamMediaCreator(teamHelper, null);
        init(mediaCreator, fragment, listener);
        return mediaCreator;
    }

    public static TeamMediaCreator get(Bundle bundle,
                                       Fragment fragment,
                                       @Nullable OnSuccessListener<Uri> listener) {
        TeamMediaCreator mediaCreator = bundle.getParcelable(MEDIA_CREATOR_KEY);
        init(mediaCreator, fragment, listener);
        return mediaCreator;
    }

    private static void init(TeamMediaCreator mediaCreator,
                             Fragment fragment,
                             OnSuccessListener<Uri> listener) {
        mediaCreator.mFragment = fragment;
        mediaCreator.mListener = listener;
        mediaCreator.mWriteAccessRequestHandler =
                new IoHelper.RequestHandler(fragment, mediaCreator);
    }

    public Bundle toBundle() {
        Bundle args = new Bundle();
        args.putParcelable(MEDIA_CREATOR_KEY, this);
        return args;
    }

    public void setTeamHelper(TeamHelper teamHelper) {
        mTeamHelper = teamHelper;
    }

    @Override
    public void onSuccess(Void aVoid) {
        capture();
    }

    @SuppressWarnings("MissingPermission") // TODO remove once Google fixes their plugin
    public void capture() {
        Context context = mFragment.getContext();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            String[] permsArray = IoHelper.WRITE_PERMS.toArray(new String[IoHelper.WRITE_PERMS.size()]);
            if (!EasyPermissions.hasPermissions(context, permsArray)) {
                mWriteAccessRequestHandler.requestPerms(R.string.write_storage_rationale_media);
                return;
            }

            //noinspection MissingPermission
            File rsFolder = IoHelper.getMediaFolder();

            File photoFile = null;
            try {
                photoFile = createImageFile(rsFolder);
            } catch (IOException e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }

            if (photoFile != null) {
                mPhotoPath = photoFile.getAbsolutePath();

                Uri photoUri = FileProvider.getUriForFile(
                        context, context.getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                mFragment.startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        mWriteAccessRequestHandler.onActivityResult(requestCode);

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            Uri contentUri = Uri.fromFile(new File(mPhotoPath));

            mListener.onSuccess(contentUri);

            // Tell gallery that we have a new photo
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(contentUri);
            mFragment.getContext().sendBroadcast(mediaScanIntent);
        }
    }

    private File createImageFile(File mediaFolder) throws IOException {
        String timeStamp = new SimpleDateFormat("ss", Locale.US).format(new Date());
        return File.createTempFile(mTeamHelper + "_" + timeStamp, ".jpg", mediaFolder);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) { // NOPMD
        mWriteAccessRequestHandler.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mTeamHelper, flags);
        dest.writeString(mPhotoPath);
    }
}
