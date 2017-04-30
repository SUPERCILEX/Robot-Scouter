package com.supercilex.robotscouter.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.util.Pair;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.PreferencesUtils;

public class ShouldUploadMediaToTbaDialog extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnShowListener {
    private static final String TAG = "ShouldUploadMediaToTbaD";

    private CheckBox mSaveResponseCheckbox;

    public static void show(Fragment fragment) {
        Pair<Boolean, Boolean> uploadMediaToTbaPair =
                PreferencesUtils.shouldAskToUploadMediaToTba(fragment.getContext());
        if (uploadMediaToTbaPair.first) {
            new ShouldUploadMediaToTbaDialog().show(fragment.getChildFragmentManager(), TAG);
        } else {
            ((TeamMediaCreator.StartCaptureListener) fragment).onStartCapture(uploadMediaToTbaPair.second);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FrameLayout rootView = (FrameLayout) View.inflate(getContext(),
                                                          R.layout.dialog_should_upload_media,
                                                          null);
        mSaveResponseCheckbox = (CheckBox) rootView.findViewById(R.id.save_response);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.should_upload_media_dialog_title))
                .setMessage(R.string.should_upload_media_rationale)
                .setView(rootView)
                .setPositiveButton(R.string.yes, this)
                .setNegativeButton(R.string.no, this)
                .create();
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((TextView) ((AlertDialog) dialog).findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        boolean isYes = which == Dialog.BUTTON_POSITIVE;
        PreferencesUtils.setShouldAskToUploadMediaToTba(
                getContext(), Pair.create(!mSaveResponseCheckbox.isChecked(), isYes));

        ((TeamMediaCreator.StartCaptureListener) getParentFragment()).onStartCapture(isYes);
    }
}
