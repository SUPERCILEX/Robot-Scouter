package com.supercilex.robotscouter.core.data.remote

import com.bumptech.glide.Glide
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.model.teamWithSafeDefaults
import com.supercilex.robotscouter.core.data.remote.TeamDetailsDownloader.Media.ChiefDelphi
import com.supercilex.robotscouter.core.data.remote.TeamDetailsDownloader.Media.Imgur
import com.supercilex.robotscouter.core.data.remote.TeamDetailsDownloader.Media.Instagram
import com.supercilex.robotscouter.core.data.remote.TeamDetailsDownloader.Media.Unsupported
import com.supercilex.robotscouter.core.data.remote.TeamDetailsDownloader.Media.YouTube
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

internal class TeamDetailsDownloader private constructor(
        team: Team
) : TeamServiceBase<TeamDetailsApi>(team, TeamDetailsApi::class.java) {
    override suspend fun execute(): Team? {
        getTeamInfo()
        getTeamMedia(Calendar.getInstance().get(Calendar.YEAR))
        return team
    }

    private suspend fun getTeamInfo() {
        val newTeam = try {
            api.getInfoAsync(team.number.toString(), tbaApiKey).await()
        } catch (e: HttpException) {
            if (e.code() == ERROR_404) return else throw e
        }

        team.name = newTeam.name
        team.website = newTeam.website
    }

    private suspend fun getTeamMedia(year: Int) {
        val response = try {
            api.getMediaAsync(team.number.toString(), year, tbaApiKey).await()
        } catch (e: HttpException) {
            if (e.code() == ERROR_404) return else throw e
        }

        val media = response.map { (type, key, preferred, details) ->
            when (type) {
                Imgur.ID -> Imgur("https://i.imgur.com/$key.png", preferred)
                YouTube.ID -> YouTube("https://img.youtube.com/vi/$key/0.jpg", preferred)
                Instagram.ID ->
                    Instagram("https://www.instagram.com/p/$key/media/?size=l", preferred)
                ChiefDelphi.ID -> ChiefDelphi(
                        "https://www.chiefdelphi.com/media/img/${checkNotNull(details).id}",
                        preferred
                )
                else -> Unsupported(type)
            }
        }.filterNot { it is Unsupported }.sortedDescending().firstOrNull { (url) ->
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD" // Don't download body
                connection.responseCode == 200
            } catch (e: Exception) {
                false
            }
        }

        if (media != null) {
            setAndCacheMedia(media.url, year)
        } else if (year > MAX_HISTORY) {
            getTeamMedia(year - 1)
        }
    }

    private suspend fun setAndCacheMedia(url: String, year: Int) {
        team.media = url
        team.mediaYear = year
        withContext(Dispatchers.Main) {
            Glide.with(RobotScouter).load(url).preload()
        }
    }

    private sealed class Media(val url: String, private val importance: Int) : Comparable<Media> {
        operator fun component1() = url

        override fun compareTo(other: Media) = importance.compareTo(other.importance)

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
        private const val MAX_HISTORY = 2000

        suspend fun load(team: Team) =
                TeamDetailsDownloader(teamWithSafeDefaults(team.number, team.id)).execute()
    }
}
