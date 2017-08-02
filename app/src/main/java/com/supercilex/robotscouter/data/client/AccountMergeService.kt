package com.supercilex.robotscouter.data.client

import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.util.accountlink.ManualMergeService
import com.google.android.gms.tasks.Task
import com.supercilex.robotscouter.util.data.model.transferUserData

class AccountMergeService : ManualMergeService() {
    // TODO fix this in https://github.com/SUPERCILEX/Robot-Scouter/issues/138
    override fun onLoadData(): Task<Void>? = null

    override fun onTransferData(response: IdpResponse): Task<Void>? {
        response.let { transferUserData(it.prevUid) }
        return null
    }
}
