package com.supercilex.robotscouter.util.data.model

import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.util.FIREBASE_METRICS
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATES
import com.supercilex.robotscouter.util.FIREBASE_TEMPLATE_INDICES
import com.supercilex.robotscouter.util.uid

val templateIndicesRef: DatabaseReference get() = getTemplateIndicesRef(uid!!)

fun getTemplateIndicesRef(uid: String): DatabaseReference = FIREBASE_TEMPLATE_INDICES.child(uid)

fun getTemplateMetricsRef(key: String): DatabaseReference =
        FIREBASE_TEMPLATES.child(key).child(FIREBASE_METRICS)
