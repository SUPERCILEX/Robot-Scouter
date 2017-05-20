package com.supercilex.robotscouter.data.remote

import android.content.Context
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.Callable

abstract class TbaServiceBase<out T>(
        team: Team, context: Context, clazz: Class<T>) : Callable<Team> {
    protected val mContext: Context = context.applicationContext
    protected val mTeam: Team = Team.Builder(team).build()
    protected val mApi: T = TBA_RETROFIT.create(clazz)

    protected val mTbaApiKey: String = mContext.getString(R.string.tba_api_key)

    protected val mYear: Int get() = Calendar.getInstance().get(Calendar.YEAR)

    @Throws(IllegalStateException::class)
    protected fun cannotContinue(response: Response<*>): Boolean {
        when {
            response.isSuccessful -> return false
            response.code() == ERROR_404 -> return true
            else -> throw IllegalStateException(response.toString())
        }
    }

    companion object {
        private val TBA_RETROFIT = Retrofit.Builder()
                .baseUrl("https://www.thebluealliance.com/api/v3/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        private val ERROR_404 = 404

        @JvmStatic
        protected fun executeAsync(service: TbaServiceBase<*>) = AsyncTaskExecutor.execute(service)
    }
}
