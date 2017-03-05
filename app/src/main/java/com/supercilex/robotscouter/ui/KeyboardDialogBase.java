package com.supercilex.robotscouter.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.supercilex.robotscouter.RobotScouter;

public abstract class KeyboardDialogBase extends DialogFragment
        implements DialogInterface.OnShowListener, View.OnClickListener, TextView.OnEditorActionListener {
    protected abstract EditText getLastEditText();

    protected abstract boolean onClick();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Show keyboard
        getDialog().getWindow()
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        getLastEditText().setOnEditorActionListener(this);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RobotScouter.getRefWatcher(getActivity()).watch(this);
    }

    @Override
    public void onClick(View v) {
        if (onClick()) getDialog().dismiss();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null
                && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                || actionId == EditorInfo.IME_ACTION_DONE) {
            onClick(getLastEditText());
            return true;
        }
        return false;
    }
}
