package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.ui.DialogBase;
import com.supercilex.robotscouter.util.BaseHelper;

import net.yslibrary.licenseadapter.LicenseAdapter;
import net.yslibrary.licenseadapter.LicenseEntry;
import net.yslibrary.licenseadapter.Licenses;

import java.util.ArrayList;
import java.util.List;

public class LicensesDialog extends DialogBase {
    public static void show(FragmentManager manager) {
        new LicensesDialog().show(manager, BaseHelper.getTag(LicensesDialog.class));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.recycler_view, null);

        List<LicenseEntry> licenses = new ArrayList<>();

        licenses.add(Licenses.noContent("Firebase",
                                        "Google Inc.",
                                        "https://firebase.google.com/terms/"));
        licenses.add(Licenses.fromGitHub("firebase/FirebaseUI-Android",
                                         Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("firebase/firebase-jobdispatcher-android",
                                         Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("square/retrofit", Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("bumptech/glide", "LICENSE"));
        licenses.add(Licenses.fromGitHub("hdodenhof/CircleImageView", Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("Triple-T/gradle-play-publisher", Licenses.NAME_MIT));
        licenses.add(Licenses.fromGitHub("square/leakcanary", Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("yshrsmz/LicenseAdapter", Licenses.LICENSE_APACHE_V2));

        RecyclerView list = (RecyclerView) rootView.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(new LicenseAdapter(licenses));

        Licenses.load(licenses);

        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .setTitle(getString(R.string.licenses))
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
