package com.supercilex.robotscouter.shared

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.content.ContextCompat
import androidx.core.net.toUri
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.model.Team

fun Team.launchTba(context: Context) =
        launchUrl(context, "http://www.thebluealliance.com/team/$number".toUri())

fun Team.launchWebsite(context: Context) = launchUrl(context, website!!.toUri())

fun launchUrl(context: Context, url: Uri) = CustomTabsIntent.Builder()
        .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setShowTitle(true)
        .addDefaultShareMenuItem()
        .enableUrlBarHiding()
        .setStartAnimations(context, R.anim.fui_slide_in_right, R.anim.fui_slide_out_left)
        .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
        .buildWithReferrer()
        .launchUrl(context, url)

private fun CustomTabsIntent.Builder.buildWithReferrer(): CustomTabsIntent {
    val customTabsIntent: CustomTabsIntent = build()
    // Add referrer intent
    customTabsIntent.intent.putExtra(
            Intent.EXTRA_REFERRER,
            "${Intent.URI_ANDROID_APP_SCHEME}//${RobotScouter.packageName}".toUri()
    )
    return customTabsIntent
}
