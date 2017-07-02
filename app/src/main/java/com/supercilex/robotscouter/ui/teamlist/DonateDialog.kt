package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.ui.ManualDismissDialog
import com.supercilex.robotscouter.util.create
import com.supercilex.robotscouter.util.getUid


class DonateDialog : ManualDismissDialog(), SeekBar.OnSeekBarChangeListener, PurchasesUpdatedListener, BillingClientStateListener {
    private val root by lazy { dialog.findViewById<View>(R.id.root) }
    private val amountTextView by lazy { dialog.findViewById<TextView>(R.id.amount_textview) }
    private val amountSeekBar by lazy { dialog.findViewById<SeekBar>(R.id.amount) }
    private val monthlyCheckBox by lazy { dialog.findViewById<CheckBox>(R.id.monthly) }

    private val billingClient by lazy { BillingClient.Builder(context).setListener(this).build() }
    private val billingClientReadyTask = TaskCompletionSource<@BillingClient.BillingResponse Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingClient.startConnection(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.donate)
            .setView(R.layout.dialog_donate)
            .setPositiveButton(R.string.donate, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create { onShow(this) }

    override fun onShow(dialog: DialogInterface) {
        super.onShow(dialog)
        amountSeekBar.setOnSeekBarChangeListener(this)
        onProgressChanged(amountSeekBar, 0, false) // Init state
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        amountTextView.text = getString(R.string.donate_amount, progress + 1)

        val width = seekBar.width - (seekBar.paddingLeft + seekBar.paddingRight)
        val thumbPos = seekBar.paddingLeft + (width * progress / seekBar.max)
        val offset = seekBar.thumbOffset - if (progress == seekBar.max) 2 * seekBar.max else seekBar.max
        amountTextView.x = (thumbPos + offset).toFloat()
    }

    override fun onDestroy() {
        super.onDestroy()
        billingClient.endConnection()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    override fun onAttemptDismiss(): Boolean {
        val result = billingClient.launchBillingFlow(activity, BillingFlowParams.Builder()
                .setSku("${amountSeekBar.progress + 1}$SKU_BASE${if (monthlyCheckBox.isChecked) "monthly" else "single"}")
                .setType(if (monthlyCheckBox.isChecked) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP)
                .setAccountId(getUid()?.hashCode().toString())
                .build())

        if (result != BillingClient.BillingResponse.OK) showError()

        return false
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {
        if (responseCode == BillingResponse.OK && purchases != null) {
            val consumptions: MutableList<Task<Pair<String, Int>>> = ArrayList()

            for (purchase in purchases) {
                val consumption = TaskCompletionSource<Pair<String, Int>>()
                billingClient.consumeAsync(purchase.purchaseToken) { purchaseToken, resultCode ->
                    consumption.setResult(purchaseToken to resultCode)
                }
                consumptions.add(consumption.task)
            }

            Tasks.whenAll(consumptions).addOnSuccessListener {
                if (consumptions.find { !it.isSuccessful } == null) {
                    dismiss()
                } else {
                    showError()
                }
            }
        } else if (responseCode == BillingResponse.USER_CANCELED) {
            Snackbar.make(root, R.string.donate_cancel_message, Snackbar.LENGTH_LONG).show()
        } else {
            showError()
        }
    }

    private fun showError() =
            Snackbar.make(root, R.string.general_error, Snackbar.LENGTH_LONG).show()

    override fun onBillingSetupFinished(resultCode: Int) = billingClientReadyTask.setResult(resultCode)

    override fun onBillingServiceDisconnected() = billingClient.startConnection(this)

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit

    companion object {
        private const val TAG = "DonateDialog"
        private const val SKU_BASE = "_donate_"

        fun show(manager: FragmentManager) = DonateDialog().show(manager, TAG)
    }
}
