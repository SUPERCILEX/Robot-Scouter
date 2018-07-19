package com.supercilex.robotscouter.core.data

import androidx.annotation.WorkerThread
import androidx.work.Data
import androidx.work.WorkManager
import androidx.work.toWorkData
import com.supercilex.robotscouter.core.data.client.TEAM_DATA_DOWNLOAD
import com.supercilex.robotscouter.core.data.client.TEAM_MEDIA_UPLOAD
import com.supercilex.robotscouter.core.model.Team
import java.util.Date

private const val NUMBER = "number"
private const val ID = "id"
private const val OWNER_KEYS = "owner_keys"
private const val OWNER_VALUES = "owner_values"
private const val ACTIVE_TOKENS_KEYS = "active_tokens_keys"
private const val ACTIVE_TOKENS_VALUES = "active_tokens_values"
private const val TEMPLATE_ID = "template_id"
private const val NAME = "name"
private const val MEDIA = "media"
private const val WEBSITE = "website"
private const val CUSTOM_NAME = "custom_name"
private const val CUSTOM_MEDIA = "custom_media"
private const val CUSTOM_WEBSITE = "custom_website"
private const val SHOULD_UPLOAD_MEDIA = "should_upload_media"
private const val MEDIA_YEAR = "media_year"
private const val TIMESTAMP = "timestamp"

@WorkerThread
internal fun cleanupJobs() {
    checkNotNull(WorkManager.getInstance()).synchronous().apply {
        listOf(TEAM_DATA_DOWNLOAD, TEAM_MEDIA_UPLOAD).forEach { cancelAllWorkByTagSync(it) }
        pruneWorkSync()
    }
}

internal fun Data.parseTeam() = Team(
        getLong(NUMBER, 0),
        checkNotNull(getString(ID, null)),
        checkNotNull(getStringArray(OWNER_KEYS))
                .zip(checkNotNull(getLongArray(OWNER_VALUES)).toTypedArray()).toMap(),
        checkNotNull(getStringArray(ACTIVE_TOKENS_KEYS))
                .zip(checkNotNull(getLongArray(ACTIVE_TOKENS_VALUES)).map { Date(it) }).toMap(),
        checkNotNull(getString(TEMPLATE_ID, null)),
        getString(NAME, null),
        getString(MEDIA, null),
        getString(WEBSITE, null),
        getBoolean(CUSTOM_NAME, false),
        getBoolean(CUSTOM_MEDIA, false),
        getBoolean(CUSTOM_WEBSITE, false),
        getBoolean(SHOULD_UPLOAD_MEDIA, false),
        getInt(MEDIA_YEAR, 0),
        Date(getLong(TIMESTAMP, 0))
)

internal fun Team.toWorkData() = toMap().toWorkData()

private fun Team.toMap() = mapOf(
        NUMBER to number,
        ID to id,
        OWNER_KEYS to owners.map { it.key }.toTypedArray(),
        OWNER_VALUES to owners.map { it.value }.toTypedArray(),
        ACTIVE_TOKENS_KEYS to activeTokens.map { it.key }.toTypedArray(),
        ACTIVE_TOKENS_VALUES to activeTokens.map { it.value.time }.toTypedArray(),
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
