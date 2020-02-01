package com.supercilex.robotscouter.server.functions

import com.supercilex.robotscouter.common.FIRESTORE_LAST_LOGIN
import com.supercilex.robotscouter.common.FIRESTORE_NAME
import com.supercilex.robotscouter.server.utils.FIRESTORE_EMAIL
import com.supercilex.robotscouter.server.utils.FIRESTORE_PHONE_NUMBER
import com.supercilex.robotscouter.server.utils.FIRESTORE_PHOTO_URL
import com.supercilex.robotscouter.server.utils.types.SetOptions
import com.supercilex.robotscouter.server.utils.types.Timestamps
import com.supercilex.robotscouter.server.utils.types.UserInfo
import com.supercilex.robotscouter.server.utils.users
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlin.js.Date
import kotlin.js.Promise
import kotlin.js.json

fun initUser(user: UserInfo): Promise<*>? = GlobalScope.async {
    console.log("Initializing user: ${JSON.stringify(user.toJSON())}")

    users.doc(user.uid).set(json(
            FIRESTORE_LAST_LOGIN to Timestamps.fromDate(Date()),
            FIRESTORE_EMAIL to user.email,
            FIRESTORE_NAME to user.displayName,
            FIRESTORE_PHONE_NUMBER to user.phoneNumber,
            FIRESTORE_PHOTO_URL to user.photoURL
    ), SetOptions.merge).await()
}.asPromise()
