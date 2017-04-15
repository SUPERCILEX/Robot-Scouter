package com.supercilex.robotscouter.ui.scout.template;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.DatabaseHelper;

public class RemoveAllMetricsDialog extends DialogFragment implements AlertDialog.OnClickListener {
    private static final String TAG = "RemoveAllMetricsDialog";

    public static void show(FragmentManager manager, DatabaseReference templateRef) {
        RemoveAllMetricsDialog fragment = new RemoveAllMetricsDialog();
        fragment.setArguments(DatabaseHelper.getRefBundle(templateRef));
        fragment.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_action)
                .setPositiveButton(R.string.remove_metrics, this)
                .setNegativeButton(android.R.string.no, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        DatabaseHelper.getRef(getArguments()).removeValue();
    }
}
