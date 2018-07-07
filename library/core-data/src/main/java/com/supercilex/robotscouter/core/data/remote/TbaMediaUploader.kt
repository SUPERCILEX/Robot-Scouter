package com.supercilex.robotscouter.core.data.remote

import androidx.annotation.WorkerThread
import com.google.gson.JsonObject
import com.supercilex.robotscouter.core.model.Team
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Response
import java.util.Calendar

internal class TbaMediaUploader private constructor(
        private val team: Team
) : TbaServiceBase<TeamMediaApi>(TeamMediaApi::class.java) {
    fun execute() {
        if (team.shouldUploadMediaToTba) uploadToTba()
    }

    private fun uploadToTba() {
        val response: Response<JsonObject> = api.postToTba(
                team.number.toString(),
                Calendar.getInstance().get(Calendar.YEAR),
                tbaApiKey,
                RequestBody.create(MediaType.parse("text/*"), checkNotNull(team.media))
        ).execute()

        if (cannotContinue(response)) return

        val body: JsonObject = checkNotNull(response.body())
        check(body.get("success").asBoolean || body.get("message").asString.let {
            it == "media_exists" || it == "suggestion_exists"
        }) {
            "Failed to upload suggestion: $body"
        }
    }

    companion object {
        @WorkerThread
        fun upload(team: Team) = TbaMediaUploader(team).execute()
    }
}
