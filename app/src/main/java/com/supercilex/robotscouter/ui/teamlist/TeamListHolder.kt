package com.supercilex.robotscouter.ui.teamlist

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.data.model.Team

class TeamListHolder : ViewModel(), FirebaseAuth.AuthStateListener {
    private val adapterListener = MutableLiveData<TeamListAdapter>()
    private val selectedTeamKeyListener = MutableLiveData<String?>()

    init {
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    fun init(savedInstanceState: Bundle?) {
        if (selectedTeamKeyListener.value == null) {
            selectedTeamKeyListener.value = savedInstanceState?.getString(TEAM_KEY)
        }
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        cleanup()
        if (auth.currentUser != null) {
//            adapterListener.value = TeamListAdapter()
//
//            mMenuHelper.setAdapter(mAdapter)
//            mRecyclerView.setAdapter(mAdapter)
//            mMenuHelper.restoreState(mSavedInstanceState)
        }
    }

    fun onSaveInstanceState(outState: Bundle) = outState.putString(TEAM_KEY, selectedTeamKeyListener.value)

    fun selectTeam(team: Team?) {
        selectedTeamKeyListener.value = team?.key
    }

    private fun cleanup() = adapterListener.value?.cleanup()

    override fun onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(this)
        cleanup()
    }

    private companion object {
        const val TEAM_KEY = "team_key"
    }
}
