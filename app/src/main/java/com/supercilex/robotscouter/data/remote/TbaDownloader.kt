package com.supercilex.robotscouter.data.remote

import android.content.Context
import android.os.Handler
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.supercilex.robotscouter.data.model.Team
import retrofit2.Response
import java.io.IOException

class TbaDownloader private constructor(team: Team, context: Context) :
        TbaServiceBase<TbaTeamApi>(team, context, TbaTeamApi::class.java) {
    @Throws(Exception::class)
    override fun call(): Team {
        getTeamInfo()
        getTeamMedia(year)
        return team
    }

    @Throws(IOException::class)
    private fun getTeamInfo() {
        val response: Response<JsonObject> = api.getInfo(team.number, tbaApiKey).execute()

        if (cannotContinue(response)) return

        val result: JsonObject = response.body()!!
        val teamNickname: JsonElement? = result.get(TEAM_NICKNAME)
        if (teamNickname != null && !teamNickname.isJsonNull) {
            team.name = teamNickname.asString
        }
        val teamWebsite: JsonElement? = result.get(TEAM_WEBSITE)
        if (teamWebsite != null && !teamWebsite.isJsonNull) {
            team.website = teamWebsite.asString
        }
    }

    @Throws(IOException::class)
    private fun getTeamMedia(year: Int) {
        val response: Response<JsonArray> = api.getMedia(team.number, year, tbaApiKey).execute()

        if (cannotContinue(response)) return

        var media: String? = null

        for (element: JsonElement in response.body()!!) {
            val mediaObject: JsonObject = element.asJsonObject
            val mediaType: String = mediaObject.get("type").asString

            if (TextUtils.equals(mediaType, IMGUR)) {
                media = "https://i.imgur.com/${mediaObject.get("foreign_key").asString}.png"

                setAndCacheMedia(media, year)
                break
            } else if (TextUtils.equals(mediaType, CHIEF_DELPHI)) {
                media = "https://www.chiefdelphi.com/media/img/" + mediaObject.get("details")
                        .asJsonObject
                        .get("image_partial")
                        .asString

                setAndCacheMedia(media, year)
                break
            }
        }

        if (TextUtils.isEmpty(media) && year > MAX_HISTORY) getTeamMedia(year - 1)
    }

    private fun setAndCacheMedia(url: String, year: Int) {
        team.media = url
        team.mediaYear = year
        Handler(context.mainLooper).post { Glide.with(context).load(url).preload() }
    }

    companion object {
        private const val TEAM_NICKNAME = "nickname"
        private const val TEAM_WEBSITE = "website"
        private const val IMGUR = "imgur"
        private const val CHIEF_DELPHI = "cdphotothread"
        private const val MAX_HISTORY = 2000

        fun load(team: Team, context: Context): Task<Team> =
                TbaServiceBase.executeAsync(TbaDownloader(team, context))
    }
}
