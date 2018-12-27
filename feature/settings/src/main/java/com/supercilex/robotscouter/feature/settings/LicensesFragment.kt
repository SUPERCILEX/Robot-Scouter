package com.supercilex.robotscouter.feature.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.supercilex.robotscouter.core.ui.FragmentBase
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.fragment_licenses.*
import net.yslibrary.licenseadapter.Library
import net.yslibrary.licenseadapter.LicenseAdapter
import net.yslibrary.licenseadapter.Licenses
import com.supercilex.robotscouter.R as RC

internal class LicensesFragment : FragmentBase() {
    private val parentActivity by unsafeLazy { activity as AppCompatActivity }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_licenses, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val libraries: List<Library> = listOf(
                Licenses.noContent(
                        "Kotlin",
                        "JetBrains",
                        "https://github.com/JetBrains/kotlin/tree/master/license"
                ),
                Licenses.noContent(
                        "Android Support Libraries",
                        "Google",
                        "https://source.android.com/setup/start/licenses"
                ),
                Licenses.noContent("Firebase", "Google", "https://firebase.google.com/terms/"),
                Licenses.noContent(
                        "Google Play Services",
                        "Google",
                        "https://developers.google.com/terms/"
                ),
                Licenses.fromGitHubApacheV2("Firebase/FirebaseUI-Android"),
                Licenses.fromGitHubApacheV2("GoogleSamples/EasyPermissions"),
                Licenses.fromGitHub(
                        "Bumptech/Glide",
                        "master/${Licenses.FILE_AUTO}",
                        "Glide license"
                ),
                Licenses.fromGitHub("Apache/POI", Licenses.LICENSE_APACHE_V2),
                Licenses.fromGitHubApacheV2("Square/Retrofit"),
                Licenses.fromGitHubApacheV2("Google/Gson"),
                Licenses.fromGitHubApacheV2("Square/Leakcanary"),
                Licenses.fromGitHubApacheV2("Sjwall/MaterialTapTargetPrompt"),
                Licenses.fromGitHubMIT("Triple-T/Gradle-Play-Publisher"),
                Licenses.fromGitHubApacheV2("Yshrsmz/LicenseAdapter")
        )

        licensesView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = LicenseAdapter(libraries)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        parentActivity.setTitle(R.string.settings_pref_licenses_title)
    }

    override fun onDestroy() {
        super.onDestroy()
        parentActivity.setTitle(RC.string.settings_activity_title)
    }

    companion object {
        const val KEY_LICENSES = "licenses"

        fun newInstance() = LicensesFragment()
    }
}
