package com.supercilex.robotscouter.ui.scout.template

import android.app.Dialog
import android.os.Bundle
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
import com.supercilex.robotscouter.ui.createAndListen
import com.supercilex.robotscouter.ui.scout.viewholder.template.SpinnerItemViewHolder
import com.supercilex.robotscouter.util.Constants
import com.supercilex.robotscouter.util.DatabaseHelper
import com.supercilex.robotscouter.util.FirebaseAdapterUtils

class SpinnerTemplateDialog : DialogFragment(), View.OnClickListener {
    private val rootView: View by lazy {
        View.inflate(context, R.layout.scout_template_edit_spinner_items, null)
    }

    private val selectedValueKey: String by lazy { arguments.getString(Constants.FIREBASE_SELECTED_VALUE_KEY) }
    private val ref: DatabaseReference by lazy { DatabaseHelper.getRef(arguments) }

    private val recyclerView: RecyclerView by lazy { rootView.findViewById(R.id.list) as RecyclerView }
    private val manager: LinearLayoutManager by lazy { LinearLayoutManager(context) }
    private val itemTouchCallback: ScoutTemplateItemTouchCallback by lazy {
        ScoutTemplateItemTouchCallback(recyclerView)
    }
    private val mAdapter: FirebaseRecyclerAdapter<String, SpinnerItemViewHolder> by lazy {
        object : FirebaseRecyclerAdapter<String, SpinnerItemViewHolder>(
                String::class.java,
                R.layout.scout_template_spinner_item,
                SpinnerItemViewHolder::class.java,
                ref) {
            override fun populateViewHolder(
                    viewHolder: SpinnerItemViewHolder,
                    itemText: String,
                    position: Int) {
                viewHolder.bind(itemText, mSnapshots[position])
                itemTouchCallback.onBind(viewHolder, position)
            }

            override fun onChildChanged(
                    type: ChangeEventListener.EventType,
                    snapshot: DataSnapshot,
                    index: Int,
                    oldIndex: Int) {
                if (type == ChangeEventListener.EventType.REMOVED) {
                    if (itemCount == 0) {
                        dismiss()
                        ref.parent.removeValue()
                        return
                    }

                    if (TextUtils.equals(selectedValueKey, snapshot.key)) {
                        ref.parent.child(Constants.FIREBASE_SELECTED_VALUE_KEY).removeValue()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootView.findViewById(R.id.fab).setOnClickListener(this)


        recyclerView.layoutManager = manager
        val touchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchCallback.setItemTouchHelper(touchHelper)
        touchHelper.attachToRecyclerView(recyclerView)

        recyclerView.adapter = mAdapter
        itemTouchCallback.setAdapter(mAdapter)
        FirebaseAdapterUtils.restoreRecyclerViewState(savedInstanceState, mAdapter, manager)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.edit_spinner_items)
            .setView(rootView)
            .setPositiveButton(android.R.string.ok, null)
            .createAndListen { window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM) }

    override fun onSaveInstanceState(outState: Bundle) {
        FirebaseAdapterUtils.saveRecyclerViewState(outState, mAdapter, manager)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        mAdapter.cleanup()
        recyclerView.clearFocus()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    override fun onClick(v: View) {
        val itemCount: Int = mAdapter.itemCount
        ref.push().setValue(
                "item " + (itemCount + 1),
                FirebaseAdapterUtils.getHighestIntPriority(mAdapter.snapshots))
        itemTouchCallback.addItemToScrollQueue(itemCount)
    }

    companion object {
        private val TAG = "SpinnerTemplateDialog"

        fun show(manager: FragmentManager, ref: DatabaseReference, selectedValueIndex: String) {
            val dialog = SpinnerTemplateDialog()

            val args: Bundle = DatabaseHelper.getRefBundle(ref)
            args.putString(Constants.FIREBASE_SELECTED_VALUE_KEY, selectedValueIndex)
            dialog.arguments = args

            dialog.show(manager, TAG)
        }
    }
}
