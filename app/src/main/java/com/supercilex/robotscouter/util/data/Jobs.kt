package com.supercilex.robotscouter.util.data

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import androidx.core.os.bundleOf
import com.firebase.jobdispatcher.Constraint
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.firebase.jobdispatcher.Job
import com.firebase.jobdispatcher.Trigger
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import java.util.Date

private const val NUMBER = "number"
private const val ID = "id"
private const val OWNERS = "owners"
private const val ACTIVE_TOKENS = "activeTokens"
private const val PENDING_APPROVALS = "pendingApprovals"
private const val TEMPLATE_ID = "template-id"
private const val NAME = "name"
private const val MEDIA = "media"
private const val WEBSITE = "website"
private const val CUSTOM_NAME = "custom-name"
private const val CUSTOM_MEDIA = "custom-media"
private const val CUSTOM_WEBSITE = "custom-website"
private const val SHOULD_UPLOAD_MEDIA = "should-upload-media-to-tba"
private const val MEDIA_YEAR = "media-year"
private const val TIMESTAMP = "timestamp"

private val fjd by lazy { FirebaseJobDispatcher(GooglePlayDriver(RobotScouter)) }
@get:RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private val scheduler by lazy {
    RobotScouter.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
}

fun Team.startInternetJob14(
        jobId: Int,
        clazz: Class<out com.firebase.jobdispatcher.JobService>,
        config: (Job.Builder.() -> Unit)? = null
) = startJob14(jobId, clazz) {
    setConstraints(Constraint.ON_ANY_NETWORK)
    extras = toRawBundle()
    if (config != null) config()
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Team.startInternetJob21(
        jobId: Int,
        clazz: Class<out JobService>,
        config: (JobInfo.Builder.() -> Unit)? = null
) = startJob21(jobId, clazz) {
    setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
    setExtras(toRawPersistableBundle())
    if (config != null) config()
}

fun cancelAllJobs() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        scheduler.cancelAll()
    } else {
        fjd.cancelAll()
    }
}

fun Bundle.parseTeam() = Team(
        getLong(NUMBER),
        getString(ID),
        getBundleAsMap(OWNERS),
        getBundleAsMap(ACTIVE_TOKENS) { Date(getLong(it)) }.toMutableMap(),
        getBundleAsMap(PENDING_APPROVALS),
        getString(TEMPLATE_ID),
        getString(NAME),
        getString(MEDIA),
        getString(WEBSITE),
        getBoolean(CUSTOM_NAME),
        getBoolean(CUSTOM_MEDIA),
        getBoolean(CUSTOM_WEBSITE),
        getBoolean(SHOULD_UPLOAD_MEDIA),
        getInt(MEDIA_YEAR),
        Date(getLong(TIMESTAMP))
)

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun PersistableBundle.parseTeam() = Team(
        getLong(NUMBER),
        getString(ID),
        getBundleAsMap(OWNERS),
        getBundleAsMap(ACTIVE_TOKENS) { Date(getLong(it)) }.toMutableMap(),
        getBundleAsMap(PENDING_APPROVALS),
        getString(TEMPLATE_ID),
        getString(NAME),
        getString(MEDIA),
        getString(WEBSITE),
        getBooleanCompat(CUSTOM_NAME),
        getBooleanCompat(CUSTOM_MEDIA),
        getBooleanCompat(CUSTOM_WEBSITE),
        getBooleanCompat(SHOULD_UPLOAD_MEDIA),
        getInt(MEDIA_YEAR),
        Date(getLong(TIMESTAMP))
)

private inline fun startJob14(
        jobId: Int,
        clazz: Class<out com.firebase.jobdispatcher.JobService>,
        config: Job.Builder.() -> Unit
) {
    fjd.newJobBuilder()
            .setService(clazz)
            .setTag(jobId.toString())
            .setTrigger(Trigger.NOW)
            .apply { config() }
            .buildAndSchedule()
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private inline fun startJob21(
        jobId: Int,
        clazz: Class<out JobService>,
        config: JobInfo.Builder.() -> Unit
) {
    JobInfo.Builder(jobId, ComponentName(RobotScouter.packageName, clazz.name))
            .apply { config() }
            .buildAndSchedule(clazz.name)
}

private fun Job.Builder.buildAndSchedule() {
    val result: Int = fjd.schedule(build())
    check(result == FirebaseJobDispatcher.SCHEDULE_RESULT_SUCCESS) {
        getErrorMessage(service, result)
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun JobInfo.Builder.buildAndSchedule(clazz: String) {
    val result: Int = scheduler.schedule(build())
    check(result == JobScheduler.RESULT_SUCCESS) {
        getErrorMessage(clazz, result)
    }
}

private fun getErrorMessage(clazz: String, result: Int) = "$clazz failed with error code $result"

private fun Team.toRawBundle() = bundleOf(
        NUMBER to number,
        ID to id,
        OWNERS to Bundle().apply {
            owners.forEach { putLong(it.key, it.value) }
        },
        ACTIVE_TOKENS to Bundle().apply {
            activeTokens.forEach { putLong(it.key, it.value.time) }
        },
        PENDING_APPROVALS to Bundle().apply {
            pendingApprovals.forEach { putString(it.key, it.value) }
        },
        TEMPLATE_ID to templateId,
        NAME to name,
        MEDIA to media,
        WEBSITE to website,
        CUSTOM_NAME to hasCustomName,
        CUSTOM_MEDIA to hasCustomMedia,
        CUSTOM_WEBSITE to hasCustomWebsite,
        SHOULD_UPLOAD_MEDIA to shouldUploadMediaToTba,
        MEDIA_YEAR to mediaYear,
        TIMESTAMP to timestamp.time
)

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Team.toRawPersistableBundle() = PersistableBundle().apply {
    putLong(NUMBER, number)
    putString(ID, id)
    putPersistableBundle(OWNERS, PersistableBundle().apply {
        owners.forEach { putLong(it.key, it.value) }
    })
    putPersistableBundle(ACTIVE_TOKENS, PersistableBundle().apply {
        activeTokens.forEach { putLong(it.key, it.value.time) }
    })
    putPersistableBundle(PENDING_APPROVALS, PersistableBundle().apply {
        pendingApprovals.forEach { putString(it.key, it.value) }
    })
    putString(TEMPLATE_ID, templateId)
    putString(NAME, name)
    putString(MEDIA, media)
    putString(WEBSITE, website)
    putBooleanCompat(CUSTOM_NAME, hasCustomName)
    putBooleanCompat(CUSTOM_MEDIA, hasCustomMedia)
    putBooleanCompat(CUSTOM_WEBSITE, hasCustomWebsite)
    putBooleanCompat(SHOULD_UPLOAD_MEDIA, shouldUploadMediaToTba)
    putInt(MEDIA_YEAR, mediaYear)
    putLong(TIMESTAMP, timestamp.time)
}
