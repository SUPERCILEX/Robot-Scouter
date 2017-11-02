package com.supercilex.robotscouter.data.remote

import android.os.Handler
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import retrofit2.Response

class TbaDownloader private constructor(
        team: Team
) : TbaServiceBase<TbaTeamApi>(team, TbaTeamApi::class.java) {
    override fun call(): Team {
        getTeamInfo()
        getTeamMedia(year)
        return team
    }

    private fun getTeamInfo() {
        val response: Response<JsonObject> =
                api.getInfo(team.number.toString(), tbaApiKey).execute()

        if (cannotContinue(response)) return

        val result: JsonObject = response.body()!!
        val teamNickname: JsonElement? = result.get(TEAM_NICKNAME)
        if (teamNickname?.isJsonNull == false) {
            team.name = teamNickname.asString
        }
        val teamWebsite: JsonElement? = result.get(TEAM_WEBSITE)
        if (teamWebsite?.isJsonNull == false) {
            team.website = teamWebsite.asString
        }
    }

    private fun getTeamMedia(year: Int) {
        val response: Response<JsonArray> =
                api.getMedia(team.number.toString(), year, tbaApiKey).execute()

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
        Handler(RobotScouter.INSTANCE.mainLooper).post {
            Glide.with(RobotScouter.INSTANCE).load(url).preload()
        }
    }

    companion object {
        private const val TEAM_NICKNAME = "nickname"
        private const val TEAM_WEBSITE = "website"
        private const val IMGUR = "imgur"
        private const val CHIEF_DELPHI = "cdphotothread"
        private const val MAX_HISTORY = 2000

        fun load(team: Team): Task<Team> = TbaServiceBase.executeAsync(TbaDownloader(team))
    }
}
