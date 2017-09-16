package com.supercilex.robotscouter.data.model

import android.support.annotation.Keep
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude

interface OrderedModel {
    @get:Exclude
    var ref: DocumentReference

    @get:Keep
    var position: Int
}
