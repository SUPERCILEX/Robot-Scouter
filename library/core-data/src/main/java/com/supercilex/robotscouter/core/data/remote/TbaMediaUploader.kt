package com.supercilex.robotscouter.core.data.remote

import com.supercilex.robotscouter.core.model.Team
import okhttp3.MediaType
import okhttp3.RequestBody
import java.util.Calendar

internal class TbaMediaUploader private constructor(
        private val team: Team
) : TbaServiceBase<TeamMediaApi>(TeamMediaApi::class.java) {
    suspend fun execute() {
        if (team.shouldUploadMediaToTba) uploadToTba()
    }

    private suspend fun uploadToTba() {
        val response = api.postToTbaAsync(
                team.number.toString(),
                Calendar.getInstance().get(Calendar.YEAR),
                tbaApiKey,
                RequestBody.create(MediaType.parse("text/*"), checkNotNull(team.media))
        ).await()

        check(response.get("success").asBoolean || response.get("message").asString.let {
            it == "media_exists" || it == "suggestion_exists"
        }) {
            "Failed to upload suggestion: $response"
        }
    }

    companion object {
        suspend fun upload(team: Team) = TbaMediaUploader(team).execute()
    }
}
