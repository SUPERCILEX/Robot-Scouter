package com.supercilex.robotscouter.ui.settings

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import net.yslibrary.licenseadapter.LicenseAdapter
import net.yslibrary.licenseadapter.LicenseEntry
import net.yslibrary.licenseadapter.Licenses
import java.util.ArrayList

class LicensesFragment : Fragment(), OnBackPressedListener {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val rootView: View = View.inflate(context, R.layout.fragment_licenses, null)

        val licenses: MutableList<LicenseEntry> = ArrayList()
        licenses.apply {
            add(Licenses.noContent("Firebase", "Google Inc.", "https://firebase.google.com/terms/"))
            add(Licenses.noContent(
                    "Google Play Services", "Google Inc.", "https://developers.google.com/terms/"))
            add(Licenses.fromGitHubApacheV2("Firebase/FirebaseUI-Android"))
            add(Licenses.fromGitHubApacheV2("Firebase/firebase-jobdispatcher-android"))
            add(Licenses.fromGitHubApacheV2("GoogleSamples/EasyPermissions"))
            add(Licenses.fromGitHub("Bumptech/Glide", "Glide license", Licenses.FILE_AUTO))
            add(Licenses.fromGitHub("Apache/POI", Licenses.LICENSE_APACHE_V2))
            add(Licenses.fromGitHubApacheV2("Clans/FloatingActionButton"))
            add(Licenses.fromGitHubApacheV2("Sjwall/MaterialTapTargetPrompt"))
            add(Licenses.fromGitHubApacheV2("Square/Retrofit"))
            add(Licenses.fromGitHubApacheV2("Square/Leakcanary"))
            add(Licenses.fromGitHubMIT("Triple-T/gradle-play-publisher"))
            add(Licenses.fromGitHubApacheV2("Yshrsmz/LicenseAdapter"))
        }

        val list: RecyclerView = rootView.findViewById(R.id.list)
        list.layoutManager = LinearLayoutManager(context)
        list.adapter = LicenseAdapter(licenses)

        Licenses.load(licenses)

        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as AppCompatActivity).setTitle(R.string.title_pref_licenses)
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as AppCompatActivity).setTitle(R.string.title_activity_settings)
    }

    override fun onBackPressed(): Boolean {
        fragmentManager.popBackStack()
        return true
    }

    companion object {
        fun newInstance() = LicensesFragment()
    }
}
