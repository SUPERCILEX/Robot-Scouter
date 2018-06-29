package com.supercilex.robotscouter.shared.scouting

import android.support.annotation.CallSuper
import android.support.v4.app.FragmentManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.supercilex.robotscouter.core.model.Metric
import kotlinx.android.extensions.LayoutContainer
import org.jetbrains.anko.find
import java.lang.ref.WeakReference

abstract class MetricViewHolderBase<M : Metric<T>, T>(
        override val containerView: View
) : RecyclerView.ViewHolder(containerView), LayoutContainer {
    lateinit var metric: M
        private set

    protected val name: TextView = itemView.find(R.id.name)
    private lateinit var _fragmentManager: WeakReference<FragmentManager>
    // This is safe b/c we're only using it to show dialogs. Anyways, we'll be getting rid of those.
    protected val fragmentManager get() = checkNotNull(_fragmentManager.get())

    fun bind(metric: M, manager: FragmentManager) {
        this.metric = metric
        this._fragmentManager = WeakReference(manager)

        bind()
    }

    @CallSuper
    protected open fun bind() {
        name.text = metric.name
    }
}
