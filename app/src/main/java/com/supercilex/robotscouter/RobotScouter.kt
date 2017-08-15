package com.supercilex.robotscouter

import android.support.multidex.MultiDexApplication
import android.support.text.emoji.EmojiCompat
import android.support.text.emoji.FontRequestEmojiCompatConfig
import android.support.v4.provider.FontRequest
import android.support.v7.app.AppCompatDelegate
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.squareup.leakcanary.LeakCanary
import com.supercilex.robotscouter.data.client.spreadsheet.initSpreadsheet
import com.supercilex.robotscouter.util.data.initPrefs
import com.supercilex.robotscouter.util.initAnalytics
import com.supercilex.robotscouter.util.initConnectivity
import com.supercilex.robotscouter.util.initConstants
import com.supercilex.robotscouter.util.ui.initNotifications
import com.supercilex.robotscouter.util.ui.initUi

class RobotScouter : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return

        initAnalytics(this)
        initConnectivity(this)
        initConstants(this)
        initPrefs(this)
        initUi(this)
        initSpreadsheet(this)
        initNotifications(this)

        FirebaseRemoteConfig.getInstance().apply {
            setDefaults(R.xml.remote_config_defaults)
            setConfigSettings(FirebaseRemoteConfigSettings.Builder()
                                      .setDeveloperModeEnabled(BuildConfig.DEBUG)
                                      .build())
        }

        EmojiCompat.init(FontRequestEmojiCompatConfig(this, FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs))
                                 .registerInitCallback(object : EmojiCompat.InitCallback() {
                                     override fun onFailed(throwable: Throwable?) {
                                         FirebaseCrash.report(throwable)
                                     }
                                 }))
    }

    companion object {
        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
