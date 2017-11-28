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
import net.yslibrary.licenseadapter.Library
import net.yslibrary.licenseadapter.LicenseAdapter
import net.yslibrary.licenseadapter.Licenses
import org.jetbrains.anko.find

class LicensesFragment : Fragment(), OnBackPressedListener {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View =
            View.inflate(context, R.layout.fragment_licenses, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val libraries: List<Library> = listOf(
                Licenses.noContent("Firebase", "Google Inc.", "https://firebase.google.com/terms/"),
                Licenses.noContent(
                        "Google Play Services",
                        "Google Inc.",
                        "https://developers.google.com/terms/"
                ),
                Licenses.fromGitHubApacheV2("Firebase/firebase-jobdispatcher-android"),
                Licenses.fromGitHubApacheV2("GoogleSamples/EasyPermissions"),
                Licenses.fromGitHub(
                        "Bumptech/Glide",
                        "master/${Licenses.FILE_AUTO}",
                        "Glide license"
                ),
                Licenses.fromGitHub("Apache/POI", Licenses.LICENSE_APACHE_V2),
                Licenses.fromGitHubApacheV2("Clans/FloatingActionButton"),
                Licenses.fromGitHubApacheV2("Sjwall/MaterialTapTargetPrompt"),
                Licenses.fromGitHubApacheV2("Firebase/FirebaseUI-Android"),
                Licenses.fromGitHubApacheV2("Square/Retrofit"),
                Licenses.fromGitHubApacheV2("Square/Leakcanary"),
                Licenses.fromGitHubMIT("Triple-T/gradle-play-publisher"),
                Licenses.fromGitHubApacheV2("Yshrsmz/LicenseAdapter")
        )

        view.find<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LicenseAdapter(libraries)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as AppCompatActivity).setTitle(R.string.settings_pref_licenses_title)
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as AppCompatActivity).setTitle(R.string.settings_activity_title)
    }

    override fun onBackPressed(): Boolean {
        fragmentManager!!.popBackStack()
        return true
    }

    companion object {
        fun newInstance() = LicensesFragment()
    }
}
