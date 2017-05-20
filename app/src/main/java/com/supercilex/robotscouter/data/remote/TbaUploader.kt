package com.supercilex.robotscouter.data.remote

import android.content.Context
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.util.*

class TbaUploader private constructor(team: Team, context: Context) :
        TbaServiceBase<TbaTeamMediaApi>(team, context, TbaTeamMediaApi::class.java) {
    @Throws(Exception::class)
    override fun call(): Team {
        uploadToImgur()
        if (mTeam.shouldUploadMediaToTba != null) uploadToTba()
        return mTeam
    }

    @Throws(IOException::class)
    private fun uploadToImgur() {
        val response = IMGUR_RETROFIT.create(TbaTeamMediaApi::class.java)
                .postToImgur(mContext.getString(R.string.imgur_client_id),
                        mTeam.toString(),
                        RequestBody.create(MediaType.parse("image/*"), File(mTeam.media)))
                .execute()

        if (cannotContinue(response)) throw IllegalStateException(response.toString())

        var link = response.body()!!.get("data").asJsonObject.get("link").asString
        // Oh Imgur, why don't you use https by default? 😢
        link = if (link.startsWith("https://")) link else link.replace("http://", "https://")
        // And what about pngs?
        link = if (link.endsWith(".png")) link else link.replace(getFileExtension(link), ".png")

        mTeam.media = link
    }

    @Throws(IOException::class)
    private fun uploadToTba() {
        val response = mApi.postToTba(
                mTeam.number,
                mYear,
                mTbaApiKey,
                RequestBody.create(MediaType.parse("text/*"), mTeam.media))
                .execute()

        if (cannotContinue(response)) return

        val body = response.body()
        if (!body!!.get("success").asBoolean && body.get("message").asString != "suggestion_exists") {
            throw IllegalStateException()
        }
    }

    /**
     * @return Return a period plus the file extension
     */
    private fun getFileExtension(url: String): String {
        val splitUrl = Arrays.asList(*url.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        return ".${splitUrl[splitUrl.size - 1]}"
    }

    companion object {
        private val IMGUR_RETROFIT = Retrofit.Builder()
                .baseUrl("https://api.imgur.com/3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        fun upload(team: Team, context: Context): Task<Team> =
                TbaServiceBase.executeAsync(TbaUploader(team, context))
    }
}
