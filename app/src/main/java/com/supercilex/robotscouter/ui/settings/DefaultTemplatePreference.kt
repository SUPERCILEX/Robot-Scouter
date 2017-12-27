package com.supercilex.robotscouter.ui.settings

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.ContextWrapper
import android.support.v4.app.FragmentActivity
import android.support.v7.preference.ListPreference
import android.util.AttributeSet
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.model.ScoutsHolder
import com.supercilex.robotscouter.util.data.model.getTemplateName
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.uid

@Suppress("unused") // Used through view reflection
class DefaultTemplatePreference : ListPreference, ChangeEventListenerBase,
        FirebaseAuth.AuthStateListener {
    private var holder: ScoutsHolder? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onAttached() {
        super.onAttached()
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onDetached() {
        super.onDetached()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
        holder?.scouts?.removeChangeEventListener(this)
    }

    override fun onDataChanged() {
        val namesListener = holder!!.scouts

        isPersistent = false

        entries = arrayOf(
                *TemplateType.values.map {
                    context.resources.getStringArray(R.array.template_new_options)[it.id]
                }.toTypedArray(),
                *namesListener.mapIndexed { index, _ ->
                    namesListener[index].getTemplateName(index)
                }.toTypedArray()
        )
        entryValues = arrayOf(
                *TemplateType.values.map { it.id.toString() }.toTypedArray(),
                *namesListener.map { it.id }.toTypedArray()
        )
        value = defaultTemplateId
        notifyChanged()

        isPersistent = true
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        holder?.apply {
            onCleared()
            scouts.removeChangeEventListener(this@DefaultTemplatePreference)
        }
        holder = uid?.let {
            ViewModelProviders
                    .of((context as ContextWrapper).baseContext as FragmentActivity)
                    // Ensure our instance is unique since we're mutating the listener
                    .get(javaClass.canonicalName + it, ScoutsHolder::class.java)
                    .apply { init(getTemplatesQuery()) }
        } ?: return
        holder!!.scouts.addChangeEventListener(this)
    }
}
