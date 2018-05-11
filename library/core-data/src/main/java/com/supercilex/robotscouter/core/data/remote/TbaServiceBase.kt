package com.supercilex.robotscouter.core.data.remote

import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.R
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal abstract class TbaServiceBase<out T>(clazz: Class<T>) {
    protected val api: T = TBA_RETROFIT.create(clazz)

    protected val tbaApiKey: String = RobotScouter.getString(R.string.tba_api_key)

    protected fun cannotContinue(response: Response<*>): Boolean = when {
        response.isSuccessful -> false
        response.code() == ERROR_404 -> true
        else -> error(response.toString())
    }

    protected companion object {
        private val TBA_RETROFIT: Retrofit = Retrofit.Builder()
                .baseUrl("https://www.thebluealliance.com/api/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        private const val ERROR_404 = 404
    }
}
