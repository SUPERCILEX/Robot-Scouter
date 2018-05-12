package com.supercilex.robotscouter.core.model

import android.support.annotation.Keep
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude

interface OrderedRemoteModel : OrderedModel {
    @get:Keep
    override var position: Int

    @get:Exclude
    val ref: DocumentReference
}
