package com.supercilex.robotscouter.ui.teamlist;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.supercilex.robotscouter.R;

import net.yslibrary.licenseadapter.LicenseAdapter;
import net.yslibrary.licenseadapter.LicenseEntry;
import net.yslibrary.licenseadapter.Licenses;

import java.util.ArrayList;
import java.util.List;

public class LicensesDialog extends DialogFragment {
    private static final String TAG = "LicensesDialog";

    public static void show(FragmentManager manager) {
        new LicensesDialog().show(manager, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = View.inflate(getContext(), R.layout.recycler_view, null);

        List<LicenseEntry> licenses = new ArrayList<>(); // TODO check that I have them all
        // TODO fix licenses once new repo version comes out

        licenses.add(Licenses.noContent("Firebase",
                                        "Google Inc.",
                                        "https://firebase.google.com/terms/"));
//        licenses.add(Licenses.noContent("Google Play Services",
//                                        "Google Inc.",
//                                        GoogleApiAvailability.getInstance()
//                                                .getOpenSourceSoftwareLicenseInfo(getContext())));
        licenses.add(Licenses.fromGitHub("firebase/FirebaseUI-Android",
                                         Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("firebase/firebase-jobdispatcher-android",
                                         Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("square/retrofit", Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("bumptech/glide", "Glide license", "LICENSE"));
        licenses.add(Licenses.fromGitHub("hdodenhof/CircleImageView", Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHubMIT("Triple-T/gradle-play-publisher"));
        licenses.add(Licenses.fromGitHub("square/leakcanary", Licenses.LICENSE_APACHE_V2));
        licenses.add(Licenses.fromGitHub("yshrsmz/LicenseAdapter", Licenses.LICENSE_APACHE_V2));

        RecyclerView list = (RecyclerView) rootView.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(new LicenseAdapter(licenses));

        Licenses.load(licenses);

        return new AlertDialog.Builder(getContext())
                .setView(rootView)
                .setTitle(R.string.licenses)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
