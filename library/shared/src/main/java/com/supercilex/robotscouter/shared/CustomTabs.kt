package com.supercilex.robotscouter.shared

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.longToast
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.colorPrimary

fun Team.launchTba(context: Context) =
        launchUrl(context, "http://www.thebluealliance.com/team/$number".toUri())

fun Team.launchWebsite(context: Context) = launchUrl(context, requireNotNull(website).toUri())

fun launchUrl(context: Context, url: Uri) {
    val intent = CustomTabsIntent.Builder()
            .setToolbarColor(colorPrimary)
            .setShowTitle(true)
            .addDefaultShareMenuItem()
            .enableUrlBarHiding()
            .setStartAnimations(context, R.anim.fui_slide_in_right, R.anim.fui_slide_out_left)
            .setExitAnimations(context, R.anim.slide_in_left, R.anim.slide_out_right)
            .buildWithReferrer()

    try {
        intent.launchUrl(context, url)
    } catch (e: ActivityNotFoundException) {
        try {
            val intentCompat = Intent(Intent.ACTION_VIEW)
            intentCompat.data = Uri.parse(url.toString())
            context.startActivity(intentCompat)
        } catch (e: ActivityNotFoundException) {
            longToast(R.string.error_unknown)
        }
    }
}

private fun CustomTabsIntent.Builder.buildWithReferrer(): CustomTabsIntent {
    val customTabsIntent: CustomTabsIntent = build()
    // Add referrer intent
    customTabsIntent.intent.putExtra(
            Intent.EXTRA_REFERRER,
            "${Intent.URI_ANDROID_APP_SCHEME}//${RobotScouter.packageName}".toUri()
    )
    return customTabsIntent
}
