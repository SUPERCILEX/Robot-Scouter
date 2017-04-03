package com.supercilex.robotscouter.ui.teamlist;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.supercilex.robotscouter.BuildConfig;
import com.supercilex.robotscouter.R;

public class UpdateDialog extends DialogFragment implements AlertDialog.OnClickListener {
    private static final String TAG = "UpdateDialog";
    private static final Uri STORE_LISTING_URI =
            Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID);

    public static void show(FragmentManager manager) {
        if (manager.findFragmentByTag(TAG) == null) new UpdateDialog().show(manager, TAG);
    }

    public static void showStoreListing(Activity activity) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW).setData(STORE_LISTING_URI));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.update_required_title)
                .setMessage(R.string.update_required_message)
                .setPositiveButton(R.string.update, this)
                .setOnCancelListener(this)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        showStoreListing(getActivity());
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        getActivity().finish();
    }
}
