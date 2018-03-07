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
import android.widget.TextView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Scout
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.model.ScoutsHolder
import com.supercilex.robotscouter.util.data.model.getTemplateName
import com.supercilex.robotscouter.util.data.model.getTemplatesQuery
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import kotlin.math.roundToInt

abstract class TemplateSelectorDialog : DialogFragmentBase() {
    @get:StringRes protected abstract val title: Int

    private val holder: ScoutsHolder by unsafeLazy {
        ViewModelProviders.of(this).get(ScoutsHolder::class.java)
    }

    private val progress: ContentLoadingProgressBar by bindView(R.id.progress)
    private val recyclerView: RecyclerView by bindView(R.id.list)
    private val adapter by unsafeLazy {
        val options = FirestoreRecyclerOptions.Builder<Scout>()
                .setSnapshotArray(holder.scouts)
                .setLifecycleOwner(this)
                .build()

        object : FirestoreRecyclerAdapter<Scout, ItemViewHolder>(options) {
            override fun getItem(position: Int): Scout = when (position) {
                TemplateType.MATCH.id, TemplateType.PIT.id -> Scout(
                        position.toString(),
                        position.toString(),
                        resources.getStringArray(R.array.template_new_options)[position])
                else -> super.getItem(position - EXTRA_ITEMS)
            }

            override fun getItemCount() = super.getItemCount() + EXTRA_ITEMS

            override fun onChildChanged(
                    type: ChangeEventType,
                    snapshot: DocumentSnapshot,
                    newIndex: Int,
                    oldIndex: Int
            ) = super.onChildChanged(type, snapshot, newIndex + EXTRA_ITEMS, oldIndex + EXTRA_ITEMS)

            override fun onDataChanged() = progress.hide()

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                    ItemViewHolder(LayoutInflater.from(parent.context).inflate(
                            R.layout.select_dialog_item_material, parent, false))

            override fun onBindViewHolder(
                    holder: ItemViewHolder,
                    position: Int,
                    scout: Scout
            ) = holder.bind(this@TemplateSelectorDialog, scout, when (position) {
                TemplateType.MATCH.id, TemplateType.PIT.id -> position.toString()
                else -> snapshots[position - EXTRA_ITEMS].id
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.init { getTemplatesQuery() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = View.inflate(context, R.layout.dialog_template_selector, null)
        return AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(rootView)
                .setNegativeButton(android.R.string.cancel, null)
                .create { onViewCreated(rootView, savedInstanceState) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progress.show()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(object : DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
        ) {
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
                val bottom = bounds.bottom + child.translationY.roundToInt()
                val top = bottom - divider.intrinsicHeight
                divider.setBounds(left, top, right, bottom)
                divider.draw(c)

                c.restore()
            }
        })
    }

    protected abstract fun onItemSelected(id: String)

    private class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
        private lateinit var listener: TemplateSelectorDialog
        private lateinit var id: String

        fun bind(listener: TemplateSelectorDialog, scout: Scout, id: String) {
            this.listener = listener
            this.id = id

            itemView as TextView
            itemView.text = scout.getTemplateName(adapterPosition - EXTRA_ITEMS)
            itemView.setOnClickListener(this)
            if (id == defaultTemplateId) {
                itemView.compoundDrawablePadding = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        itemView.resources.getDimension(R.dimen.spacing_mini),
                        itemView.resources.displayMetrics
                ).toInt()
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        itemView,
                        AppCompatResources.getDrawable(
                                itemView.context, R.drawable.ic_star_accent_24dp),
                        null,
                        null,
                        null
                )
            } else {
                itemView.compoundDrawablePadding = 0
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        itemView, null, null, null, null)
            }
        }

        override fun onClick(v: View) {
            listener.onItemSelected(id)
            listener.dismiss()
        }
    }

    private companion object {
        const val EXTRA_ITEMS = 2
    }
}
