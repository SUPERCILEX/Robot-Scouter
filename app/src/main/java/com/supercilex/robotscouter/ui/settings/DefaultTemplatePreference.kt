package com.supercilex.robotscouter.ui.settings

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.ContextWrapper
import android.support.v4.app.FragmentActivity
import android.support.v7.preference.ListPreference
import android.util.AttributeSet
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.model.ScoutsHolder
import com.supercilex.robotscouter.util.data.model.getTemplateName
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery

class DefaultTemplatePreference : ListPreference, ChangeEventListenerBase {
    private val holder = ViewModelProviders
            .of((context as ContextWrapper).baseContext as FragmentActivity)
            .get(ScoutsHolder::class.java)
            .apply { init(getTemplatesQuery()) }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onAttached() {
        super.onAttached()
        holder.scouts.addChangeEventListener(this)
    }

    override fun onDetached() {
        super.onDetached()
        holder.scouts.removeChangeEventListener(this)
    }

    override fun onDataChanged() {
        val namesListener = holder.scouts

        isPersistent = false

        entries = TemplateType.values().map {
            context.resources.getStringArray(R.array.template_new_options)[it.id]
        }.toMutableList().apply {
            addAll(namesListener.mapIndexed { index, _ ->
                namesListener[index].getTemplateName(index)
            })
        }.toTypedArray()
        entryValues = TemplateType.values().map { it.id.toString() }.toMutableList()
                .apply { addAll(namesListener.map { it.id }) }.toTypedArray()
        value = defaultTemplateId
        notifyChanged()

        isPersistent = true
    }
}
