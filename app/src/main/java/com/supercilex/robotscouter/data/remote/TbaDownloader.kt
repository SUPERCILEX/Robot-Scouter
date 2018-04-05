package com.supercilex.robotscouter.data.remote

import android.support.annotation.WorkerThread
import com.bumptech.glide.Glide
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.data.remote.TbaDownloader.Media.ChiefDelphi
import com.supercilex.robotscouter.data.remote.TbaDownloader.Media.Imgur
import com.supercilex.robotscouter.data.remote.TbaDownloader.Media.Instagram
import com.supercilex.robotscouter.data.remote.TbaDownloader.Media.Unsupported
import com.supercilex.robotscouter.data.remote.TbaDownloader.Media.YouTube
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

        val media = response.body()!!.asSequence().map {
            val mediaObject: JsonObject = it.asJsonObject

            val type = mediaObject.get("type").asString
            val key = mediaObject.get("foreign_key").asString
            val preferred = mediaObject.get("preferred").asBoolean

            when (type) {
                Imgur.ID -> Imgur("https://i.imgur.com/$key.png", preferred)
                YouTube.ID -> YouTube("https://img.youtube.com/vi/$key/0.jpg", preferred)
                Instagram.ID ->
                    Instagram("https://www.instagram.com/p/$key/media/?size=l", preferred)
                ChiefDelphi.ID -> ChiefDelphi(
                        "https://www.chiefdelphi.com/media/img/" + mediaObject.get("details")
                                .asJsonObject
                                .get("image_partial")
                                .asString,
                        preferred
                )
                else -> Unsupported(type)
            }
        }.filterNot { it is Unsupported }.sorted().firstOrNull()

        if (media != null) {
            setAndCacheMedia(media.url, year)
        } else if (year > MAX_HISTORY) {
            getTeamMedia(year - 1)
        }
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

    private sealed class Media(val url: String, private val importance: Int) : Comparable<Media> {
        override fun compareTo(other: Media) = other.importance.compareTo(importance)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Media

            return url == other.url
        }

        override fun hashCode() = url.hashCode()

        override fun toString(): String {
            return "${javaClass.simpleName}(url='$url')"
        }

        class Imgur(url: String, preferred: Boolean) :
                Media(url, if (preferred) Int.MAX_VALUE else 0) {
            companion object {
                const val ID = "imgur"
            }
        }

        class Instagram(url: String, preferred: Boolean) :
                Media(url, (if (preferred) Int.MAX_VALUE else 0) - 2) {
            companion object {
                const val ID = "instagram-image"
            }
        }

        class ChiefDelphi(url: String, preferred: Boolean) :
                Media(url, (if (preferred) Int.MAX_VALUE else 0) - 3) {
            companion object {
                const val ID = "cdphotothread"
            }
        }

        class YouTube(url: String, preferred: Boolean) :
                Media(url, (if (preferred) Int.MAX_VALUE else 0) - 1) {
            companion object {
                const val ID = "youtube"
            }
        }

        class Unsupported(type: String) : Media(type, Int.MIN_VALUE)
    }

    companion object {
        private const val TEAM_NICKNAME = "nickname"
        private const val TEAM_WEBSITE = "website"
        private const val MAX_HISTORY = 2000

        @WorkerThread
        fun load(team: Team) = TbaDownloader(team).execute()
    }
}
