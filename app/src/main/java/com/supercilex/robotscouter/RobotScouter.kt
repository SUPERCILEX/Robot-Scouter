package com.supercilex.robotscouter

import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.arch.core.executor.ArchTaskExecutor
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.multidex.MultiDexApplication
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatDelegate
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appindexing.FirebaseAppIndex
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.squareup.leakcanary.LeakCanary
import com.supercilex.robotscouter.ui.teamlist.TeamListActivity
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.LateinitVal
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.hasPerformedV1toV2Migration
import com.supercilex.robotscouter.util.data.hasShownAddTeamTutorial
import com.supercilex.robotscouter.util.data.initDatabase
import com.supercilex.robotscouter.util.data.initPrefs
import com.supercilex.robotscouter.util.initAnalytics
import com.supercilex.robotscouter.util.initRemoteConfig
import com.supercilex.robotscouter.util.ui.initNotifications
import com.supercilex.robotscouter.util.ui.initUi
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.longToast

class RobotScouter : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return

        INSTANCE = this

        initAnalytics()
        initRemoteConfig()
        initDatabase()
        initPrefs()
        initUi()
        initNotifications()

        performMigrations()
    }

    private fun performMigrations() {
        registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is TeamListActivity && !hasPerformedV1toV2Migration) {
                    if (RobotScouter.INSTANCE.getSharedPreferences(
                            "com.supercilex.robotscouter.ui.teamlist.TeamListActivity",
                            Context.MODE_PRIVATE).all.isEmpty()) {
                        hasPerformedV1toV2Migration = true
                        return
                    }

                    ViewModelProviders.of(activity).get(V1ToV2Migrator::class.java).init(activity)
                }
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    class V1ToV2Migrator : ViewModelBase<FragmentActivity>() {
        override fun onCreate(args: FragmentActivity) {
            INSTANCE.longToast("Starting migration from v1 to v2. Please wait...")

            AuthUI.getInstance().signOut(args).addOnSuccessListener(
                    AsyncTaskExecutor, OnSuccessListener {
                try {
                    deleteStuff()
                    signBackIn()
                    copyPrefs()
                    hasPerformedV1toV2Migration = true
                    Handler(Looper.getMainLooper()).postDelayed(
                            {
                                INSTANCE.longToast("Migration complete, restarting Robot Scouter...")
                                // Ensure shared prefs were saved
                                ArchTaskExecutor.getInstance().executeOnDiskIO { restartApp() }
                            }, 500)
                } catch (e: Exception) {
                    FirebaseCrash.report(e)
                    Crashlytics.logException(e)
                }
            })
        }

        private fun deleteStuff() {
            FirebaseAppIndex.getInstance().removeAll()
            INSTANCE.getSharedPreferences("spreadsheet_export", Context.MODE_PRIVATE)
                    .edit().clear().commit()
            INSTANCE.cacheDir.deleteRecursively()
            INSTANCE.filesDir.deleteRecursively()
        }

        private fun signBackIn() {
            Tasks.await(FirebaseAuth.getInstance().signInAnonymously())
        }

        private fun copyPrefs() {
            val (hasShownFabTutorial, hasShownSignInTutorial) = INSTANCE.getSharedPreferences(
                    "com.supercilex.robotscouter.ui.teamlist.TeamListActivity",
                    Context.MODE_PRIVATE).let {
                val result = it.getBoolean("has_shown_tutorial_fab", false) to
                        it.getBoolean("has_shown_tutorial_sign_in", false)
                it.edit().clear().commit()
                result
            }
            val shouldUploadMediaToTba = INSTANCE.getSharedPreferences(
                    "upload_media", Context.MODE_PRIVATE).let {
                val result = it.getBoolean("should_upload_media_to_tba", false)
                it.edit().clear().commit()
                result
            }

            hasShownAddTeamTutorial = hasShownFabTutorial
            com.supercilex.robotscouter.util.data.hasShownSignInTutorial = hasShownSignInTutorial
            if (shouldUploadMediaToTba) {
                com.supercilex.robotscouter.util.data.shouldUploadMediaToTba = true
            }
        }

        private fun restartApp() {
            (INSTANCE.getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 100,
                    PendingIntent.getActivity(
                            INSTANCE,
                            123456,
                            INSTANCE.intentFor<TeamListActivity>(),
                            PendingIntent.FLAG_CANCEL_CURRENT))
            System.exit(0)
        }
    }

    companion object {
        var INSTANCE: RobotScouter by LateinitVal()

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }
}
