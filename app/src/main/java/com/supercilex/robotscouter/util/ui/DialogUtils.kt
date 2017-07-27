package com.supercilex.robotscouter.util.ui

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog

inline fun AlertDialog.Builder.create(crossinline listener: AlertDialog.() -> Unit): AlertDialog =
        create().apply { setOnShowListener { (it as AlertDialog).listener() } }

fun DialogFragment.show(manager: FragmentManager,
                        tag: String,
                        args: Bundle = Bundle(),
                        argsListener: (Bundle.() -> Unit)? = null) {
    arguments = args.apply { argsListener?.invoke(this) }
    show(manager, tag)
}
