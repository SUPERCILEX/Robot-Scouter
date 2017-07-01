package com.supercilex.robotscouter.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.supercilex.robotscouter.util.ConstantsKt.providerAuthorityJava;
import static com.supercilex.robotscouter.util.IoUtilsKt.IO_PERMS;
import static com.supercilex.robotscouter.util.IoUtilsKt.createFile;
import static com.supercilex.robotscouter.util.IoUtilsKt.getMediaFolder;
import static com.supercilex.robotscouter.util.IoUtilsKt.hideFile;
import static com.supercilex.robotscouter.util.IoUtilsKt.unhideFile;

public final class TeamMediaCreator implements Parcelable, OnSuccessListener<Void>, ActivityCompat.OnRequestPermissionsResultCallback {
    public interface StartCaptureListener {
        void onStartCapture(boolean shouldUploadMediaToTba);
    }

    public static final Parcelable.Creator<TeamMediaCreator> CREATOR = new Parcelable.Creator<TeamMediaCreator>() {
        @Override
        public TeamMediaCreator createFromParcel(Parcel source) {
            return new TeamMediaCreator(source.readParcelable(TeamHelper.class.getClassLoader()),
                                        source.readString(),
                                        source.readInt() == 1);
        }

        @Override
        public TeamMediaCreator[] newArray(int size) {
            return new TeamMediaCreator[size];
        }
    };

    private static final int TAKE_PHOTO_RC = 334;
    private static final List<String> PERMS;
    private static final String MEDIA_CREATOR_KEY = "media_creator";

    private WeakReference<Fragment> mFragment;
    private TeamHelper mTeamHelper;
    private PermissionRequestHandler mPermHandler;
    private WeakReference<OnSuccessListener<TeamHelper>> mListener;
    private String mPhotoPath;
    private boolean mShouldUploadMediaToTba;

    static {
        List<String> perms = new ArrayList<>();
        perms.addAll(IO_PERMS);
        perms.add(Manifest.permission.CAMERA);
        PERMS = Collections.unmodifiableList(perms);
    }

    private TeamMediaCreator(TeamHelper teamHelper,
                             String photoPath,
                             boolean shouldUploadMediaToTba) {
        mTeamHelper = teamHelper;
        mPhotoPath = photoPath;
        mShouldUploadMediaToTba = shouldUploadMediaToTba;
    }

    public static TeamMediaCreator newInstance(Fragment fragment,
                                               TeamHelper teamHelper,
                                               OnSuccessListener<TeamHelper> listener) {
        TeamMediaCreator mediaCreator = new TeamMediaCreator(teamHelper, null, false);
        init(mediaCreator, fragment, listener);
        return mediaCreator;
    }

    public static TeamMediaCreator get(Bundle bundle,
                                       Fragment fragment,
                                       OnSuccessListener<TeamHelper> listener) {
        TeamMediaCreator mediaCreator = bundle.getParcelable(MEDIA_CREATOR_KEY);
        init(mediaCreator, fragment, listener);
        return mediaCreator;
    }

    private static void init(TeamMediaCreator mediaCreator,
                             Fragment fragment,
                             OnSuccessListener<TeamHelper> listener) {
        mediaCreator.mFragment = new WeakReference<>(fragment);
        mediaCreator.mListener = new WeakReference<>(listener);
        mediaCreator.mPermHandler = new PermissionRequestHandler(PERMS, fragment, mediaCreator);
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
        startCapture(mShouldUploadMediaToTba);
    }

    public void startCapture(boolean shouldUploadMediaToTba) {
        mShouldUploadMediaToTba = shouldUploadMediaToTba;

        Context context = mFragment.get().getContext();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            if (!EasyPermissions.hasPermissions(context, mPermHandler.getPermsArray())) {
                mPermHandler.requestPerms(R.string.write_storage_rationale_media);
                return;
            }

            File photoFile = null;
            try {
                //noinspection MissingPermission
                photoFile = createImageFile(getMediaFolder());
            } catch (IOException e) {
                FirebaseCrash.report(e);
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }

            if (photoFile != null) {
                mPhotoPath = photoFile.getAbsolutePath();

                Uri photoUri = FileProvider.getUriForFile(
                        context, providerAuthorityJava, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                mFragment.get().startActivityForResult(takePictureIntent, TAKE_PHOTO_RC);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode) {
        mPermHandler.onActivityResult(requestCode);

        if (requestCode == TAKE_PHOTO_RC) {
            File photo = new File(mPhotoPath);
            Context context = mFragment.get().getContext();
            if (resultCode == Activity.RESULT_OK) {
                try {
                    photo = unhideFile(photo);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

                Uri contentUri = Uri.fromFile(photo);

                mTeamHelper.getTeam().setHasCustomMedia(true);
                mTeamHelper.getTeam().setShouldUploadMediaToTba(mShouldUploadMediaToTba);
                mTeamHelper.getTeam().setMedia(contentUri.getPath());
                mListener.get().onSuccess(mTeamHelper);

                // Tell gallery that we have a new photo
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(contentUri);
                context.sendBroadcast(mediaScanIntent);
            } else {
                photo.delete();
            }
        }
    }

    private File createImageFile(File mediaFolder) throws IOException {
        return createFile(hideFile(mTeamHelper.toString()),
                          "jpg",
                          mediaFolder,
                          String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        mPermHandler.onRequestPermissionsResult(
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
        dest.writeInt(mShouldUploadMediaToTba ? 1 : 0);
    }
}
