package com.supercilex.robotscouter.ui.scout.template

import android.app.Dialog
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
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
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.ui.CardListHelper
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerItemViewHolder
import com.supercilex.robotscouter.util.FIREBASE_SELECTED_VALUE_KEY
import com.supercilex.robotscouter.util.create
import com.supercilex.robotscouter.util.getHighestIntPriority
import com.supercilex.robotscouter.util.getRefBundle
import com.supercilex.robotscouter.util.ref
import com.supercilex.robotscouter.util.show

class SpinnerTemplateDialog : DialogFragment(), View.OnClickListener,
        LifecycleRegistryOwner { // TODO remove once arch components are merged into support lib
    private val rootView: View by lazy {
        View.inflate(context, R.layout.scout_template_edit_spinner_items, null)
    }

    private val holder by lazy { ViewModelProviders.of(this).get(SpinnerItemsHolder::class.java) }

    private val recyclerView: RecyclerView by lazy { rootView.findViewById<RecyclerView>(R.id.list) }
    private val itemTouchCallback: ScoutTemplateItemTouchCallback by lazy {
        ScoutTemplateItemTouchCallback(recyclerView)
    }
    private val adapter: FirebaseRecyclerAdapter<String, SpinnerItemViewHolder> by lazy {
        object : FirebaseRecyclerAdapter<String, SpinnerItemViewHolder>(
                holder.spinnerItems,
                R.layout.scout_template_spinner_item,
                SpinnerItemViewHolder::class.java) {
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
                        dismiss()
                        ref.parent.removeValue()
                        return
                    }

                    if (TextUtils.equals(holder.selectedValueKey, snapshot.key)) {
                        ref.parent.child(FIREBASE_SELECTED_VALUE_KEY).removeValue()
                    }
                }

                if (type == ChangeEventListener.EventType.ADDED && snapshot.priority == null) {
                    snapshot.ref.setPriority(index)
                }

                if (itemTouchCallback.onChildChanged(type, index)) {
                    super.onChildChanged(type, snapshot, index, oldIndex)
                }
            }
        }
    }
    private val cardListHelper: CardListHelper by lazy { CardListHelper(adapter, recyclerView) }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        holder.init(arguments)

        rootView.findViewById<FloatingActionButton>(R.id.fab).setOnClickListener(this)

        recyclerView.layoutManager = LinearLayoutManager(context)
        val touchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.setItemTouchHelper(touchHelper)
        touchHelper.attachToRecyclerView(recyclerView)

        recyclerView.adapter = adapter
        itemTouchCallback.setAdapter(adapter)
        itemTouchCallback.setCardListHelper(cardListHelper)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.edit_spinner_items)
            .setView(rootView)
            .setPositiveButton(android.R.string.ok, null)
            .create { window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) }

    override fun onDestroy() {
        super.onDestroy()
        adapter.cleanup()
        recyclerView.clearFocus()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    override fun onClick(v: View) {
        val itemCount: Int = adapter.itemCount
        ref.push().setValue("item ${itemCount + 1}", getHighestIntPriority(adapter.snapshots) + 1)
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
