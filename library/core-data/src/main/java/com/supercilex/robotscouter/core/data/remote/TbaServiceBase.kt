package com.supercilex.robotscouter.core.data.remote

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.R
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal abstract class TbaServiceBase<out T>(clazz: Class<T>) {
    protected val api: T = TBA_RETROFIT.create(clazz)

    protected val tbaApiKey: String = RobotScouter.getString(R.string.tba_api_key)

    protected companion object {
        const val ERROR_404 = 404

        private val TBA_RETROFIT: Retrofit = Retrofit.Builder()
                .baseUrl("https://www.thebluealliance.com/api/v3/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}
