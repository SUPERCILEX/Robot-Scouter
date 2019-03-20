package com.supercilex.robotscouter.core.data.remote

import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.R
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.create
import java.io.File

internal class TeamMediaUploader private constructor(
        team: Team
) : TeamServiceBase<TeamMediaApi>(team, TeamMediaApi::class.java) {
    override suspend fun execute(): Team? {
        withContext(Dispatchers.IO) {
            team.media?.takeIf { File(it).exists() }
        } ?: return null

        uploadToImgur()
        return team
    }

    private suspend fun uploadToImgur() {
        val response = TeamMediaApi.IMGUR_RETROFIT
                .create<TeamMediaApi>()
                .postToImgurAsync(
                        RobotScouter.getString(R.string.imgur_client_id),
                        team.toString(),
                        RequestBody.create(MediaType.parse("image/*"), File(team.media))
                )
                .await()

        var link: String = response.get("data").asJsonObject.get("link").asString
        // Oh Imgur, why don't you use https by default? 😢
        link = if (link.startsWith("https://")) link else link.replace("http://", "https://")
        // And what about pngs?
        link = if (link.endsWith(".png")) link else link.replace(getFileExtension(link), ".png")

        team.media = link
        team.hasCustomMedia = true
    }

    /**
     * @return Return a period plus the file extension
     */
    private fun getFileExtension(url: String): String =
            ".${url.split(".").dropLastWhile(String::isEmpty).last()}"

    companion object {
        suspend fun upload(team: Team) = TeamMediaUploader(team).execute()
    }
}
