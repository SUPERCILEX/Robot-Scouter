package com.supercilex.robotscouter.ui.scouting.templatelist

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import com.firebase.ui.database.ChangeEventListener
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.ui.scouting.templatelist.viewholder.SpinnerItemViewHolder
import com.supercilex.robotscouter.util.FIREBASE_SELECTED_VALUE_KEY
import com.supercilex.robotscouter.util.data.getRefBundle
import com.supercilex.robotscouter.util.ui.CardListHelper
import com.supercilex.robotscouter.util.ui.create
import com.supercilex.robotscouter.util.ui.getHighestIntPriority
import com.supercilex.robotscouter.util.ui.show

class SpinnerTemplateDialog : DialogFragment(), View.OnClickListener {
    private val rootView: View by lazy {
        View.inflate(context, R.layout.scout_template_edit_spinner_items, null)
    }

    private val holder by lazy { ViewModelProviders.of(this).get(SpinnerItemsHolder::class.java) }

    private val recyclerView: RecyclerView by lazy { rootView.findViewById<RecyclerView>(R.id.list) }
    private val itemTouchCallback by lazy { TemplateItemTouchCallback<String>(recyclerView) }
    private val adapter: FirebaseRecyclerAdapter<String, SpinnerItemViewHolder> by lazy {
        object : FirebaseRecyclerAdapter<String, SpinnerItemViewHolder>(
                holder.spinnerItems,
                R.layout.scout_template_spinner_item,
                SpinnerItemViewHolder::class.java,
                this) {
            private val cardListHelper by lazy { CardListHelper(this, recyclerView) }

            override fun getItem(position: Int) = itemTouchCallback.getItem(position)

            override fun getRef(position: Int) = itemTouchCallback.getRef(position)

            override fun populateViewHolder(viewHolder: SpinnerItemViewHolder,
                                            itemText: String,
                                            position: Int) {
                itemTouchCallback.onBind(viewHolder, position)
                cardListHelper.onBind(viewHolder)
                viewHolder.bind(itemText, mSnapshots[position])
            }

            override fun onChildChanged(type: ChangeEventListener.EventType,
                                        snapshot: DataSnapshot,
                                        index: Int,
                                        oldIndex: Int) {
                if (type == ChangeEventListener.EventType.REMOVED) {
                    if (itemCount == 0) {
                        Snackbar.make(rootView, R.string.delete_spinner, Snackbar.LENGTH_LONG)
                                .setAction(R.string.delete) {
                                    holder.ref.parent.removeValue()
                                    dismiss()
                                }
                                .show()
                    }

                    if (TextUtils.equals(holder.selectedValueKey, snapshot.key)) {
                        holder.ref.parent.child(FIREBASE_SELECTED_VALUE_KEY).removeValue()
                    }
                }

                if (type == ChangeEventListener.EventType.ADDED && snapshot.priority == null) {
                    snapshot.ref.setPriority(index)
                }

                itemTouchCallback.onChildChanged(type, index) {
                    super.onChildChanged(type, snapshot, index, oldIndex)
                }
                cardListHelper.onChildChanged(type, index)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.init(arguments)

        rootView.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener(this)

        recyclerView.layoutManager = LinearLayoutManager(context)
        val touchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.itemTouchHelper = touchHelper
        touchHelper.attachToRecyclerView(recyclerView)

        recyclerView.adapter = adapter
        itemTouchCallback.adapter = adapter
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.edit_spinner_items_long)
            .setView(rootView)
            .setPositiveButton(android.R.string.ok, null)
            .create { window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) }

    override fun onStop() {
        super.onStop()
        recyclerView.clearFocus()
    }

    override fun onClick(v: View) {
        val itemCount: Int = adapter.itemCount
        holder.ref.push()
                .setValue("item ${itemCount + 1}", getHighestIntPriority(adapter.snapshots) + 1)
        itemTouchCallback.addItemToScrollQueue(itemCount)
    }

    companion object {
        private const val TAG = "SpinnerTemplateDialog"

        fun show(manager: FragmentManager, ref: DatabaseReference, selectedValueKey: String?) =
                SpinnerTemplateDialog().show(manager, TAG, getRefBundle(ref)) {
                    putString(FIREBASE_SELECTED_VALUE_KEY, selectedValueKey)
                }
    }
}
