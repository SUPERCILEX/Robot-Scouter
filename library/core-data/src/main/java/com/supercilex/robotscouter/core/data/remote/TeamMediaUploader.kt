package com.supercilex.robotscouter.core.data.remote

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.R
import com.supercilex.robotscouter.core.data.logFailures
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.create
import java.io.File

internal class TeamMediaUploader(
        private val id: String,
        private val name: String,
        private val media: String,
        private val shouldUploadMediaToTba: Boolean
) {
    suspend fun execute() {
        if (!File(media).exists()) return

        val imageUrl = uploadToImgur()
        notifyBackend(imageUrl)
    }

    private suspend fun uploadToImgur(): String {
        val response = TeamMediaApi.IMGUR_RETROFIT
                .create<TeamMediaApi>()
                .postToImgurAsync(
                        RobotScouter.getString(R.string.imgur_client_id),
                        name,
                        RequestBody.create(MediaType.parse("image/*"), File(media))
                )

        var link: String = response.get("data").asJsonObject.get("link").asString
        // Oh Imgur, why don't you use https by default? 😢
        link = if (link.startsWith("https://")) link else link.replace("http://", "https://")
        // And what about pngs?
        link = if (link.endsWith(".png")) link else link.replace(getFileExtension(link), ".png")

        return link
    }

    private suspend fun notifyBackend(url: String) {
        Firebase.functions
                .getHttpsCallable("clientApi")
                .call(mapOf(
                        "operation" to "update-team-media",
                        "teamId" to id,
                        "url" to url,
                        "shouldUploadMediaToTba" to shouldUploadMediaToTba
                ))
                .logFailures("updateTeamMedia", url)
                .await()
    }

    /**
     * @return Return a period plus the file extension
     */
    private fun getFileExtension(url: String): String =
            ".${url.split(".").dropLastWhile(String::isEmpty).last()}"
}
