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

import com.google.android.gms.common.GoogleApiAvailability;
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

        List<LicenseEntry> licenses = new ArrayList<>();

        licenses.add(Licenses.noContent("Firebase",
                                        "Google Inc.",
                                        "https://firebase.google.com/terms/"));
        licenses.add(Licenses.noLink("Google Play Services",
                                     "Google Inc.",
                                     GoogleApiAvailability.getInstance()
                                             .getOpenSourceSoftwareLicenseInfo(getContext())));
        licenses.add(Licenses.fromGitHubApacheV2("firebase/FirebaseUI-Android"));
        licenses.add(Licenses.fromGitHubApacheV2("firebase/firebase-jobdispatcher-android"));
        licenses.add(Licenses.fromGitHubApacheV2("googlesamples/Easypermissions"));
        licenses.add(Licenses.fromGitHub("bumptech/Glide", "Glide license", Licenses.FILE_AUTO));
        licenses.add(Licenses.fromGitHubApacheV2("hdodenhof/CircleImageView"));
        licenses.add(Licenses.fromGitHubApacheV2("Clans/FloatingActionButton"));
        licenses.add(Licenses.fromGitHubApacheV2("sjwall/MaterialTapTargetPrompt"));
        licenses.add(Licenses.fromGitHubApacheV2("square/Retrofit"));
        licenses.add(Licenses.fromGitHubApacheV2("square/Leakcanary"));
        licenses.add(Licenses.fromGitHubMIT("Triple-T/gradle-play-publisher"));
        licenses.add(Licenses.fromGitHubApacheV2("yshrsmz/LicenseAdapter"));

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
