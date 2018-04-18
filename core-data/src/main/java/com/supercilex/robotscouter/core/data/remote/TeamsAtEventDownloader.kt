package com.supercilex.robotscouter.core.data.remote

import android.support.annotation.WorkerThread
import com.supercilex.robotscouter.core.data.model.teamWithSafeDefaults
import com.supercilex.robotscouter.core.model.Team

class TeamsAtEventDownloader private constructor(
        private val eventKey: String
) : TbaServiceBase<TeamsApi>(TeamsApi::class.java) {
    private fun execute(): List<Team>? {
        val response = api.getTeams(eventKey, tbaApiKey).execute()

        if (cannotContinue(response)) return null

        return response.body()!!.map {
            teamWithSafeDefaults(it.asString.removePrefix("frc").toLong(), "")
        }
    }

    companion object {
        @WorkerThread
        fun execute(eventKey: String) = TeamsAtEventDownloader(eventKey).execute()
    }
}
