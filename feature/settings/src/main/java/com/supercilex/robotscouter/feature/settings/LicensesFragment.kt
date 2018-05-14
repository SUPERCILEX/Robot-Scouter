package com.supercilex.robotscouter.feature.settings

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import kotlinx.android.synthetic.main.fragment_licenses.*
import net.yslibrary.licenseadapter.Library
import net.yslibrary.licenseadapter.LicenseAdapter
import net.yslibrary.licenseadapter.Licenses
import com.supercilex.robotscouter.R as RC

internal class LicensesFragment : FragmentBase(), OnBackPressedListener {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = View.inflate(context, R.layout.fragment_licenses, null)

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

        licensesView.apply {
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
        (activity as AppCompatActivity).setTitle(RC.string.settings_activity_title)
    }

    override fun onBackPressed(): Boolean {
        requireFragmentManager().popBackStack()
        return true
    }

    companion object {
        fun newInstance() = LicensesFragment()
    }
}
