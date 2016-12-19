package com.supercilex.robotscouter.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

public abstract class KeyboardDialog extends DialogBase implements DialogInterface.OnShowListener {
    public abstract boolean onClick();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHelper.showKeyboard();
    }

    protected AlertDialog createDialog(View rootView, @StringRes int title) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(rootView)
                .setTitle(title)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        Button ok = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (KeyboardDialog.this.onClick()) getDialog().dismiss();
            }
        });
    }
}
