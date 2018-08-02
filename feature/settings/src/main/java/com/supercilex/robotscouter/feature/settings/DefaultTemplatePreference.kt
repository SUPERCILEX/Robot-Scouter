package com.supercilex.robotscouter.feature.settings

import android.content.Context
import android.content.ContextWrapper
import android.util.AttributeSet
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import androidx.preference.ListPreference
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.model.ScoutsHolder
import com.supercilex.robotscouter.core.data.model.getTemplateName
import com.supercilex.robotscouter.core.data.model.getTemplatesQuery
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.R as RC

@Suppress("unused") // Used through view reflection
internal class DefaultTemplatePreference : ListPreference, ChangeEventListenerBase,
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
        val namesListener = checkNotNull(holder).scouts

        isPersistent = false

        val defaultTemplateTypes = TemplateType.values.filterNot { it == TemplateType.EMPTY }
        entries = arrayOf(
                *defaultTemplateTypes.map {
                    context.resources.getStringArray(RC.array.template_new_options)[it.id]
                }.map {
                    context.getString(R.string.settings_pref_default_template_name, it)
                }.toTypedArray(),
                *namesListener.mapIndexed { index, _ ->
                    namesListener[index].getTemplateName(index)
                }.toTypedArray()
        )
        entryValues = arrayOf(
                *defaultTemplateTypes.map { it.id.toString() }.toTypedArray(),
                *namesListener.map { it.id }.toTypedArray()
        )
        value = defaultTemplateId
        notifyChanged()

        isPersistent = true
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        holder?.apply {
            scouts.removeChangeEventListener(this@DefaultTemplatePreference)
            onCleared()
        }
        holder = uid?.let {
            ViewModelProviders
                    .of((context as ContextWrapper).baseContext as FragmentActivity)
                    // Ensure our instance is unique since we're mutating the listener
                    .get(javaClass.canonicalName + it, ScoutsHolder::class.java)
                    .apply { init { getTemplatesQuery() } }
        }
        holder?.scouts?.addChangeEventListener(this)
    }
}
