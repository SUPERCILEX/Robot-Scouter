package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.Constants;

public class AboutDialog extends DialogFragment implements DialogInterface.OnClickListener {
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
                .setNeutralButton(R.string.copy_debug_info, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText(getString(R.string.debug_info_name),
                                                      Constants.getDebugInfo(getContext())));
        Toast.makeText(getContext(), R.string.debug_info_copied, Toast.LENGTH_SHORT).show();
    }
}
