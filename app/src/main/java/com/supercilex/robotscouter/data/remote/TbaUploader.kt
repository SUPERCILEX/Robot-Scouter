package com.supercilex.robotscouter.data.remote

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.gson.JsonObject
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.util.Arrays

class TbaUploader private constructor(team: Team, context: Context) :
        TbaServiceBase<TbaTeamMediaApi>(team, context, TbaTeamMediaApi::class.java) {
    @Throws(Exception::class)
    override fun call(): Team {
        uploadToImgur()
        if (team.shouldUploadMediaToTba != null) uploadToTba()
        return team
    }

    @Throws(IOException::class)
    private fun uploadToImgur() {
        val response: Response<JsonObject> = TbaTeamMediaApi.IMGUR_RETROFIT
                .create(TbaTeamMediaApi::class.java)
                .postToImgur(context.getString(R.string.imgur_client_id),
                        team.toString(),
                        RequestBody.create(MediaType.parse("image/*"), File(team.media)))
                .execute()

        if (cannotContinue(response)) throw IllegalStateException(response.toString())

        var link: String = response.body()!!.get("data").asJsonObject.get("link").asString
        // Oh Imgur, why don't you use https by default? 😢
        link = if (link.startsWith("https://")) link else link.replace("http://", "https://")
        // And what about pngs?
        link = if (link.endsWith(".png")) link else link.replace(getFileExtension(link), ".png")

        team.media = link
    }

    @Throws(IOException::class)
    private fun uploadToTba() {
        val response: Response<JsonObject> = api.postToTba(
                team.number,
                year,
                tbaApiKey,
                RequestBody.create(MediaType.parse("text/*"), team.media))
                .execute()

        if (cannotContinue(response)) return

        val body: JsonObject = response.body()!!
        if (!body.get("success").asBoolean && body.get("message").asString != "suggestion_exists") {
            throw IllegalStateException()
        }
    }

    /**
     * @return Return a period plus the file extension
     */
    private fun getFileExtension(url: String): String {
        val splitUrl: List<String> = Arrays.asList(*url.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        return ".${splitUrl[splitUrl.size - 1]}"
    }

    companion object {
        fun upload(team: Team, context: Context): Task<Team> =
                TbaServiceBase.executeAsync(TbaUploader(team, context))
    }
}
