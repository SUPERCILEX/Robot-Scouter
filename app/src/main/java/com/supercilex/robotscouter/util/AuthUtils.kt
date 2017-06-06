package com.supercilex.robotscouter.util

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig


val user: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser

val uid: String? get() = user?.uid

val isSignedIn: Boolean get() = user != null

val isFullUser: Boolean get() = isSignedIn && !user!!.isAnonymous

fun onSignedIn(): Task<FirebaseAuth> = TaskCompletionSource<FirebaseAuth>().also {
    FirebaseAuth.getInstance().addAuthStateListener(object : FirebaseAuth.AuthStateListener {
        override fun onAuthStateChanged(auth: FirebaseAuth) {
            if (auth.currentUser == null) {
                signInAnonymouslyDbInit()
            } else {
                it.trySetResult(auth)
                auth.removeAuthStateListener(this)
            }
        }
    })
}.task

fun signInAnonymouslyInitBasic(): Task<AuthResult> = FirebaseAuth.getInstance()
        .signInAnonymously().addOnSuccessListener { updateAnalyticsUserId() }

fun signInAnonymouslyDbInit(): Task<AuthResult> = signInAnonymouslyInitBasic().addOnSuccessListener {
    DatabaseInitializer()
}

private class DatabaseInitializer : ValueEventListener, OnSuccessListener<Void> {
    init {
        fetchAndActivate().addOnSuccessListener(this)
        FIREBASE_SCOUT_INDICES.addListenerForSingleValueEvent(this)
    }

    override fun onSuccess(aVoid: Void?) {
        if (FirebaseRemoteConfig.getInstance().getBoolean(SHOULD_CACHE_DB)) {
            FIREBASE_SCOUT_TEMPLATES.addListenerForSingleValueEvent(this)
        }
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        // This allows the database to work offline without any setup
    }

    override fun onCancelled(error: DatabaseError) = FirebaseCrash.report(error.toException())

    private companion object {
        const val SHOULD_CACHE_DB = "should_cache_db"
    }
}
