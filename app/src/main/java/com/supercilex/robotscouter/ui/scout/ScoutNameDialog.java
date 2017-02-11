package com.supercilex.robotscouter.ui.scout;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.DatabaseHelper;

public class ScoutNameDialog extends DialogFragment implements Dialog.OnClickListener {
    private static final String TAG = "ScoutNameDialog";
    private static final String NAME = "name";

    private EditText mName;

    public static void show(FragmentManager manager, DatabaseReference ref, String name) {
        ScoutNameDialog dialog = new ScoutNameDialog();

        Bundle bundle = DatabaseHelper.getRefBundle(ref);
        bundle.putString(NAME, name);
        dialog.setArguments(bundle);

        dialog.show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        TextInputLayout layout =
                (TextInputLayout) View.inflate(getContext(), R.layout.dialog_scout_name, null);
        mName = (EditText) layout.findViewById(R.id.name);
        mName.setText(getArguments().getString(NAME));

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.edit_scout_name)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        DatabaseReference ref = DatabaseHelper.getRef(getArguments());
        String name = mName.getText().toString();
        ref.child(Constants.FIREBASE_NAME).setValue(TextUtils.isEmpty(name) ? null : name);
    }
}
