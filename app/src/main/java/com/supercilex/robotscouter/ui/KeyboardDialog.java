package com.supercilex.robotscouter.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public abstract class KeyboardDialog extends DialogBase implements DialogInterface.OnShowListener {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mHelper.showKeyboard();
    }

    public void setOnEditorActionListener(final EditText editText) {
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                        || (actionId == EditorInfo.IME_ACTION_DONE)
                        && !TextUtils.isEmpty(editText.getText())) {
                    onClick();
                }
                return true;
            }
        });
    }

    public AlertDialog createDialog(View rootView, @StringRes int title) {
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
            public void onClick(View view) {
                if (KeyboardDialog.this.onClick()) getDialog().dismiss();
            }
        });
    }

    public abstract boolean onClick();
}
