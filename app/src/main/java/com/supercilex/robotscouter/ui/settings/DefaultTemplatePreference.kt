package com.supercilex.robotscouter.ui.settings

import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.ContextWrapper
import android.support.v4.app.FragmentActivity
import android.support.v7.preference.ListPreference
import android.util.AttributeSet
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.TEMPLATE_TYPES
import com.supercilex.robotscouter.util.data.ChangeEventListenerBase
import com.supercilex.robotscouter.util.data.defaultTemplateKey
import com.supercilex.robotscouter.util.data.model.TabNamesHolder

class DefaultTemplatePreference : ListPreference, ChangeEventListenerBase {
    private val templateNamesHolder = ViewModelProviders
            .of((context as ContextWrapper).baseContext as FragmentActivity)
            .get(TabNamesHolder::class.java)
            .apply { init(null) }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onAttached() {
        super.onAttached()
        templateNamesHolder.namesListener.addChangeEventListener(this)
    }

    override fun onDetached() {
        super.onDetached()
        templateNamesHolder.namesListener.removeChangeEventListener(this)
    }

    override fun onDataChanged() {
        val namesListener = templateNamesHolder.namesListener

        isPersistent = false

        entries = TEMPLATE_TYPES.mapIndexed { index, _ ->
            context.resources.getStringArray(R.array.new_template_options)[index]
        }.toMutableList().apply {
            addAll(namesListener.mapIndexed { index, _ -> namesListener.getObject(index) })
        }.toTypedArray()
        entryValues = TEMPLATE_TYPES.map { it.toString() }.toMutableList()
                .apply { addAll(namesListener.map { it.key }) }.toTypedArray()
        value = defaultTemplateKey
        notifyChanged()

        isPersistent = true
    }
}
