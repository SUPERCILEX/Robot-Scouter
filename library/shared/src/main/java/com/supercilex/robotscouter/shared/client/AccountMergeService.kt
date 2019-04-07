package com.supercilex.robotscouter.shared.client

import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.util.accountlink.ManualMergeService
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.common.FIRESTORE_TOKEN
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.model.transferUserData
import com.supercilex.robotscouter.core.data.model.userRef
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.logBreadcrumb
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.asTask
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.supercilex.robotscouter.core.data.model.userPrefs as userPrefsRef

internal class AccountMergeService : ManualMergeService() {
    private lateinit var instance: Instance

    override fun onLoadData(): Task<Void?>? {
        val prevUid = checkNotNull(uid)
        logBreadcrumb("Migrating user data from $prevUid.")

        return GlobalScope.async {
            val token = UUID.randomUUID().toString()
            userRef.set(mapOf(FIRESTORE_TOKEN to token), SetOptions.merge()).await()
            instance = Instance(prevUid, token)
        }.asTask().logFailures("loadDataForAccountMerge").continueWith { null }
    }

    override fun onTransferData(response: IdpResponse): Task<Void?>? {
        val (prevUid, token) = instance
        transferUserData(prevUid, token)
        return null
    }

    /** Stores an immutable representation of the current invocation to prevent race conditions. */
    data class Instance(val prevUid: String, val token: String)
}
