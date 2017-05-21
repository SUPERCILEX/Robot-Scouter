package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.google.android.gms.common.GoogleApiAvailability
import com.supercilex.robotscouter.R
import net.yslibrary.licenseadapter.LicenseAdapter
import net.yslibrary.licenseadapter.LicenseEntry
import net.yslibrary.licenseadapter.Licenses
import java.util.*

class LicensesDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView: View = View.inflate(context, R.layout.recycler_view, null)

        val licenses: MutableList<LicenseEntry> = ArrayList()
        licenses.apply {
            add(Licenses.noContent("Firebase", "Google Inc.", "https://firebase.google.com/terms/"))
            add(Licenses.noLink(
                    "Google Play Services",
                    "Google Inc.",
                    GoogleApiAvailability.getInstance().getOpenSourceSoftwareLicenseInfo(context)))
            add(Licenses.fromGitHubApacheV2("Firebase/FirebaseUI-Android"))
            add(Licenses.fromGitHubApacheV2("Firebase/firebase-jobdispatcher-android"))
            add(Licenses.fromGitHubApacheV2("GoogleSamples/EasyPermissions"))
            add(Licenses.fromGitHub("Bumptech/Glide", "Glide license", Licenses.FILE_AUTO))
            add(Licenses.fromGitHubApacheV2("Hdodenhof/CircleImageView"))
            add(Licenses.fromGitHub("Apache/POI", Licenses.LICENSE_APACHE_V2))
            add(Licenses.fromGitHubApacheV2("Clans/FloatingActionButton"))
            add(Licenses.fromGitHubApacheV2("Sjwall/MaterialTapTargetPrompt"))
            add(Licenses.fromGitHubApacheV2("Square/Retrofit"))
            add(Licenses.fromGitHubApacheV2("Square/Leakcanary"))
            add(Licenses.fromGitHubMIT("Triple-T/gradle-play-publisher"))
            add(Licenses.fromGitHubApacheV2("Yshrsmz/LicenseAdapter"))
        }

        val list: RecyclerView = rootView.findViewById(R.id.list) as RecyclerView
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = LicenseAdapter(licenses)

        Licenses.load(licenses)

        return AlertDialog.Builder(context)
                .setView(rootView)
                .setTitle(R.string.licenses)
                .setPositiveButton(android.R.string.ok, null)
                .create()
    }

    companion object {
        private val TAG = "LicensesDialog"

        fun show(manager: FragmentManager) = LicensesDialog().show(manager, TAG)
    }
}
