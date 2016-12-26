package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.DialogBase;

public class AboutDialog extends DialogBase {
    private static final String TAG = "AboutDialog";

    public static void show(FragmentManager manager) {
        new AboutDialog().show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        TextView rootView = (TextView) View.inflate(getContext(), R.layout.about, null);
        rootView.setMovementMethod(LinkMovementMethod.getInstance());
        return new AlertDialog.Builder(getContext())
                .setView(rootView)
                .setTitle(R.string.about)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
