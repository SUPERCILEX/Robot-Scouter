package com.supercilex.robotscouter.util.ui;

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
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.data.IoUtilsKt;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

import static com.supercilex.robotscouter.util.ConstantsKt.getProviderAuthority;
import static com.supercilex.robotscouter.util.data.IoUtilsKt.createFile;
import static com.supercilex.robotscouter.util.data.IoUtilsKt.getIO_PERMS;
import static com.supercilex.robotscouter.util.data.IoUtilsKt.getMediaFolder;

public final class TeamMediaCreator implements Parcelable, OnSuccessListener<Void>, ActivityCompat.OnRequestPermissionsResultCallback {
    public interface StartCaptureListener {
        void onStartCapture(boolean shouldUploadMediaToTba);
    }

    public static final Parcelable.Creator<TeamMediaCreator> CREATOR = new Parcelable.Creator<TeamMediaCreator>() {
        @Override
        public TeamMediaCreator createFromParcel(Parcel source) {
            return new TeamMediaCreator(source.readParcelable(Team.class.getClassLoader()),
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

    static {
        List<String> perms = new ArrayList<>();
        perms.addAll(getIO_PERMS());
        perms.add(Manifest.permission.CAMERA);
        PERMS = Collections.unmodifiableList(perms);
    }

    private WeakReference<Fragment> mFragment;
    private Team mTeam;
    private PermissionRequestHandler mPermHandler;
    private WeakReference<OnSuccessListener<Team>> mListener;
    private String mPhotoPath;
    private boolean mShouldUploadMediaToTba;

    private TeamMediaCreator(Team team,
                             String photoPath,
                             boolean shouldUploadMediaToTba) {
        mTeam = team;
        mPhotoPath = photoPath;
        mShouldUploadMediaToTba = shouldUploadMediaToTba;
    }

    public static TeamMediaCreator newInstance(Fragment fragment,
                                               Team team,
                                               OnSuccessListener<Team> listener) {
        TeamMediaCreator mediaCreator = new TeamMediaCreator(team, null, false);
        init(mediaCreator, fragment, listener);
        return mediaCreator;
    }

    public static TeamMediaCreator get(Bundle bundle,
                                       Fragment fragment,
                                       OnSuccessListener<Team> listener) {
        TeamMediaCreator mediaCreator = bundle.getParcelable(MEDIA_CREATOR_KEY);
        init(mediaCreator, fragment, listener);
        return mediaCreator;
    }

    private static void init(TeamMediaCreator mediaCreator,
                             Fragment fragment,
                             OnSuccessListener<Team> listener) {
        mediaCreator.mFragment = new WeakReference<>(fragment);
        mediaCreator.mListener = new WeakReference<>(listener);
        mediaCreator.mPermHandler = new PermissionRequestHandler(PERMS, fragment, mediaCreator);
    }

    public Bundle toBundle() {
        Bundle args = new Bundle();
        args.putParcelable(MEDIA_CREATOR_KEY, this);
        return args;
    }

    public void setTeam(Team team) {
        mTeam = team;
    }

    @Override
    public void onSuccess(Void nothing) {
        startCapture(mShouldUploadMediaToTba);
    }

    public void startCapture(boolean shouldUploadMediaToTba) {
        mShouldUploadMediaToTba = shouldUploadMediaToTba;

        Context context = mFragment.get().getContext();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            if (!EasyPermissions.hasPermissions(context, mPermHandler.getPermsArray())) {
                mPermHandler.requestPerms(R.string.media_write_storage_rationale);
                return;
            }

            File photoFile = null;
            try {
                //noinspection MissingPermission
                photoFile = createImageFile(getMediaFolder());
            } catch (Exception e) { // NOPMD
                FirebaseCrash.report(e);
                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();
            }

            if (photoFile != null) {
                mPhotoPath = photoFile.getAbsolutePath();

                Uri photoUri = FileProvider.getUriForFile(
                        context, getProviderAuthority(), photoFile);
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
                    photo = IoUtilsKt.unhide(photo);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

                Uri contentUri = Uri.fromFile(photo);

                mTeam.setHasCustomMedia(true);
                mTeam.setShouldUploadMediaToTba(mShouldUploadMediaToTba);
                mTeam.setMedia(contentUri.getPath());
                mListener.get().onSuccess(mTeam);

                // Tell gallery that we have a new photo
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(contentUri);
                context.sendBroadcast(mediaScanIntent);
            } else {
                photo.delete();
            }
        }
    }

    private File createImageFile(File mediaFolder) {
        return IoUtilsKt.hide(createFile(mTeam.toString(),
                                         "jpg",
                                         mediaFolder,
                                         String.valueOf(System.currentTimeMillis())));
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
        dest.writeParcelable(mTeam, flags);
        dest.writeString(mPhotoPath);
        dest.writeInt(mShouldUploadMediaToTba ? 1 : 0);
    }
}
