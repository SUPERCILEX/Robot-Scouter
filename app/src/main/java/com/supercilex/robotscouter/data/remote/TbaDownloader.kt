package com.supercilex.robotscouter.data.remote

import android.content.Context
import android.os.Handler
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.data.model.Team
import java.io.IOException

class TbaDownloader private constructor(team: Team, context: Context) : TbaServiceBase<TbaTeamApi>(
        team,
        context,
        TbaTeamApi::class.java) {
    @Throws(Exception::class)
    override fun call(): Team {
        getTeamInfo()
        getTeamMedia(mYear)
        return mTeam
    }

    @Throws(IOException::class)
    private fun getTeamInfo() {
        val response = mApi.getInfo(mTeam.number, mTbaApiKey).execute()

        if (cannotContinue(response)) return

        val result = response.body()
        val teamNickname = result!!.get(TEAM_NICKNAME)
        if (teamNickname != null && !teamNickname.isJsonNull) {
            mTeam.name = teamNickname.asString
        }
        val teamWebsite = result.get(TEAM_WEBSITE)
        if (teamWebsite != null && !teamWebsite.isJsonNull) {
            mTeam.website = teamWebsite.asString
        }
    }

    @Throws(IOException::class)
    private fun getTeamMedia(year: Int) {
        val response = mApi.getMedia(mTeam.number, year, mTbaApiKey).execute()

        if (cannotContinue(response)) return

        var media: String? = null

        val result = response.body()
        for (i in 0..result!!.size() - 1) {
            val mediaObject = result.get(i).asJsonObject
            val mediaType = mediaObject.get("type").asString

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
        mTeam.media = url
        mTeam.mediaYear = year
        Handler(mContext.mainLooper).post {
            Glide.with(mContext)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .preload()
        }
    }

    companion object {
        private val TEAM_NICKNAME = "nickname"
        private val TEAM_WEBSITE = "website"
        private val IMGUR = "imgur"
        private val CHIEF_DELPHI = "cdphotothread"
        private val MAX_HISTORY = 2000

        fun load(team: Team, context: Context): Task<Team> =
                TbaServiceBase.executeAsync(TbaDownloader(team, context))
    }
}
