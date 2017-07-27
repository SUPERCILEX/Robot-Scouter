package com.supercilex.robotscouter.util.ui

import android.content.Intent
import android.os.Build

fun Intent.addNewDocumentFlags(): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
    }
    return this
}
