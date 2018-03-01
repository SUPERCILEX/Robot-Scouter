package com.supercilex.robotscouter.data.remote

import android.support.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import org.jetbrains.anko.runOnUiThread
import retrofit2.Response

class TbaDownloader private constructor(
        team: Team
) : TbaServiceBase<TbaTeamApi>(team, TbaTeamApi::class.java) {
    override fun execute(): Team? {
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
            val newName = teamNickname.asString
            if (team.name == newName) {
                team.hasCustomName = false
            } else {
                team.name = newName
            }
        }
        val teamWebsite: JsonElement? = result.get(TEAM_WEBSITE)
        if (teamWebsite?.isJsonNull == false) {
            val newWebsite = teamWebsite.asString
            if (team.website == newWebsite) {
                team.hasCustomWebsite = false
            } else {
                team.website = newWebsite
            }
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

            if (mediaType == IMGUR) {
                media = "https://i.imgur.com/${mediaObject.get("foreign_key").asString}.png"

                setAndCacheMedia(media, year)
                break
            } else if (mediaType == CHIEF_DELPHI) {
                media = "https://www.chiefdelphi.com/media/img/" + mediaObject.get("details")
                        .asJsonObject
                        .get("image_partial")
                        .asString

                setAndCacheMedia(media, year)
                break
            }
        }

        if (media.isNullOrBlank() && year > MAX_HISTORY) getTeamMedia(year - 1)
    }

    private fun setAndCacheMedia(url: String, year: Int) {
        if (team.media == url) {
            team.hasCustomMedia = false
        } else {
            team.media = url
        }
        team.mediaYear = year
        RobotScouter.runOnUiThread {
            Glide.with(this).load(url).preload()
        }
    }

    companion object {
        private const val TEAM_NICKNAME = "nickname"
        private const val TEAM_WEBSITE = "website"
        private const val IMGUR = "imgur"
        private const val CHIEF_DELPHI = "cdphotothread"
        private const val MAX_HISTORY = 2000

        @WorkerThread
        fun load(team: Team) = TbaDownloader(team).execute()
    }
}
