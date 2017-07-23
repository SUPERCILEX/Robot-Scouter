package com.supercilex.robotscouter.ui.scout.template

import android.arch.lifecycle.ViewModel
import android.os.Bundle
import com.firebase.ui.database.ClassSnapshotParser
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.ObservableSnapshotArray
import com.supercilex.robotscouter.util.FIREBASE_SELECTED_VALUE_KEY
import com.supercilex.robotscouter.util.getRef

class SpinnerItemsHolder : ViewModel() {
    var selectedValueKey: String? = null
        private set
    var spinnerItems: ObservableSnapshotArray<String>? = null
        private set

    fun init(args: Bundle) {
        if (selectedValueKey == null) {
            selectedValueKey = args.getString(FIREBASE_SELECTED_VALUE_KEY)
        }
        if (spinnerItems == null) {
            spinnerItems = FirebaseArray(getRef(args), ClassSnapshotParser(String::class.java))
//            spinnerItems!!.addChangeEventListener(object : ChangeEventListenerBase() {})
        }
    }
}
