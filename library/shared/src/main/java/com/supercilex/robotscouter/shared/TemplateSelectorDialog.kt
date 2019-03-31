package com.supercilex.robotscouter.shared

import android.app.Dialog
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.core.data.defaultTemplateId
import com.supercilex.robotscouter.core.data.model.ScoutsHolder
import com.supercilex.robotscouter.core.data.model.getTemplateName
import com.supercilex.robotscouter.core.data.model.getTemplatesQuery
import com.supercilex.robotscouter.core.model.Scout
import com.supercilex.robotscouter.core.model.TemplateType
import com.supercilex.robotscouter.core.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_template_selector.*
import kotlin.math.roundToInt

abstract class TemplateSelectorDialog : BottomSheetDialogFragmentBase() {
    private val holder by stateViewModels<ScoutsHolder>()

    override val containerView: View by unsafeLazy {
        View.inflate(context, R.layout.dialog_template_selector, null)
    }
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

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        progress.show()

        templatesView.adapter = adapter
        templatesView.addItemDecoration(object : DividerItemDecoration(
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
                val child =
                        parent.getChildAt(1 - (templatesView.layoutManager as LinearLayoutManager)
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

    @CallSuper
    protected open fun onItemSelected(id: String) {
        if (setAsDefault.isChecked) defaultTemplateId = id
    }

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
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        itemView,
                        null,
                        null,
                        AppCompatResources.getDrawable(
                                itemView.context, R.drawable.ic_star_accent_24dp),
                        null
                )
            } else {
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
