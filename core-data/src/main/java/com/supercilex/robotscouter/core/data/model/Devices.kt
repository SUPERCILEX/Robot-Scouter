package com.supercilex.robotscouter.core.data.model

import com.firebase.ui.firestore.SnapshotParser
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.core.model.Device

val deviceParser = SnapshotParser {
    it.data!!.map { (id, data) ->
        val fields = data as Map<String, Any?>
        Device(id, fields[FIRESTORE_NAME] as String?)
    }
}
