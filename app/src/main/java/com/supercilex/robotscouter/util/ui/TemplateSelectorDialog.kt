package com.supercilex.robotscouter.util.ui

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.widget.TextViewCompat
import android.support.v7.app.AlertDialog
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.DataSnapshot
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.MATCH
import com.supercilex.robotscouter.data.model.PIT
import com.supercilex.robotscouter.util.data.defaultTemplateKey
import com.supercilex.robotscouter.util.data.model.TabNamesHolder
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar

abstract class TemplateSelectorDialog : LifecycleDialogFragment() {
    @get:StringRes
    protected abstract val title: Int

    protected open val holder: TabNamesHolder by lazy {
        ViewModelProviders.of(this).get(TabNamesHolder::class.java)
    }

    protected val rootView: LinearLayout by lazy {
        View.inflate(context, R.layout.dialog_template_selector, null) as LinearLayout
    }
    private val progress by lazy { rootView.findViewById<ContentLoadingProgressBar>(R.id.progress) }
    private val recyclerView: RecyclerView by lazy { rootView.findViewById<RecyclerView>(R.id.list) }
    private val adapter by lazy {
        object : FirebaseRecyclerAdapter<String, ItemViewHolder>(
                holder.namesListener,
                R.layout.select_dialog_item_material,
                ItemViewHolder::class.java,
                this) {
            override fun getItem(position: Int): String = when (position) {
                MATCH, PIT -> resources.getStringArray(R.array.new_template_options)[position]
                else -> super.getItem(position - EXTRA_ITEMS)
            }

            override fun getItemCount() = super.getItemCount() + EXTRA_ITEMS

            override fun onChildChanged(type: ChangeEventListener.EventType,
                                        snapshot: DataSnapshot,
                                        index: Int,
                                        oldIndex: Int) = super.onChildChanged(
                    type, snapshot, index + EXTRA_ITEMS, oldIndex + EXTRA_ITEMS)

            override fun onDataChanged() = progress.hide()

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder =
                    ItemViewHolder(
                            LayoutInflater.from(parent.context).inflate(viewType, parent, false))

            override fun populateViewHolder(holder: ItemViewHolder, text: String, position: Int) =
                    holder.bind(this@TemplateSelectorDialog, text, when (position) {
                        MATCH, PIT -> position.toString()
                        else -> getRef(position - EXTRA_ITEMS).key
                    })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.init(null)
        progress.show()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(object : DividerItemDecoration(
                context, DividerItemDecoration.VERTICAL) {
            private val divider = DividerItemDecoration::class.java
                    .getDeclaredField("mDivider")
                    .apply { isAccessible = true }
                    .get(this) as Drawable
            private val bounds = Rect()

            override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                if (parent.childCount <= EXTRA_ITEMS) return

                c.save()

                val left: Int
                val right: Int
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && parent.clipToPadding) {
                    left = parent.paddingLeft
                    right = parent.width - parent.paddingRight
                    c.clipRect(left, parent.paddingTop, right, parent.height - parent.paddingBottom)
                } else {
                    left = 0
                    right = parent.width
                }

                // Only draw the divider for the second item i.e. the last native template
                val child = parent.getChildAt(1 - (recyclerView.layoutManager as LinearLayoutManager)
                        .findFirstVisibleItemPosition()) ?: return
                parent.getDecoratedBoundsWithMargins(child, bounds)
                val bottom = bounds.bottom + Math.round(child.translationY)
                val top = bottom - divider.intrinsicHeight
                divider.setBounds(left, top, right, bottom)
                divider.draw(c)

                c.restore()
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setView(rootView)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

    protected abstract fun onItemSelected(key: String)

    private class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
        private lateinit var listener: TemplateSelectorDialog
        private lateinit var key: String

        fun bind(listener: TemplateSelectorDialog, text: String, key: String) {
            this.listener = listener
            this.key = key

            itemView as TextView
            itemView.text = text
            itemView.setOnClickListener(this)
            if (key == defaultTemplateKey) {
                itemView.compoundDrawablePadding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        itemView.resources.getDimension(R.dimen.spacing_mini),
                        itemView.resources.displayMetrics).toInt()
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        itemView,
                        AppCompatResources.getDrawable(
                                itemView.context, R.drawable.ic_star_accent_24dp),
                        null,
                        null,
                        null)
            }
        }

        override fun onClick(v: View) {
            listener.onItemSelected(key)
            listener.dismiss()
        }
    }

    private companion object {
        const val EXTRA_ITEMS = 2
    }
}
