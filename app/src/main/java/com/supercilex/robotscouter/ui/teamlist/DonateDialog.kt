package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.ui.ManualDismissDialog
import com.supercilex.robotscouter.util.create
import com.supercilex.robotscouter.util.getUid

class DonateDialog : ManualDismissDialog(), SeekBar.OnSeekBarChangeListener, PurchasesUpdatedListener, BillingClientStateListener {
    private val root by lazy { View.inflate(context, R.layout.dialog_donate, null) }
    private val content by lazy { root.findViewById<View>(R.id.content) }
    private val progress by lazy { root.findViewById<ProgressBar>(R.id.progress) }
    private val amountTextView by lazy { root.findViewById<TextView>(R.id.amount_textview) }
    private val amountSeekBar by lazy { root.findViewById<SeekBar>(R.id.amount) }
    private val monthlyCheckBox by lazy { root.findViewById<CheckBox>(R.id.monthly) }

    private val billingClient by lazy { BillingClient.Builder(context).setListener(this).build() }
    private val billingClientReadyTask = TaskCompletionSource<@BillingClient.BillingResponse Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingClient.startConnection(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.donate)
            .setView(root)
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
        updateProgress(true)

        billingClientReadyTask.task.addOnSuccessListener(activity) {
            val sku = "${amountSeekBar.progress + 1}$SKU_BASE${if (monthlyCheckBox.isChecked) "monthly" else "single"}"
            val type = if (monthlyCheckBox.isChecked) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP

            val result = billingClient.launchBillingFlow(activity, BillingFlowParams.Builder()
                    .setSku(sku)
                    .setType(type)
                    .setAccountId(getUid()?.hashCode().toString())
                    .build())

            if (result != BillingResponse.OK) {
                if (result == BillingResponse.ITEM_ALREADY_OWNED) {
                    billingClient.queryPurchaseHistoryAsync(type) {
                        if (it.responseCode != BillingResponse.OK) {
                            FirebaseCrash.report(
                                    IllegalStateException("Purchase fetch failed with code ${it.responseCode}"))
                            showError()
                            return@queryPurchaseHistoryAsync
                        }

                        getConsumePurchasesTask(it.purchasesList)
                                .addOnSuccessListener { onAttemptDismiss() }
                                .addOnFailureListener { showError() }
                    }
                    return@addOnSuccessListener
                }

                FirebaseCrash.report(IllegalStateException("Unknown purchase error: $result"))
                showError()
            }
        }

        return false
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {
        if (responseCode == BillingResponse.OK && purchases != null) {
            getConsumePurchasesTask(purchases)
                    .addOnSuccessListener { dismiss() }
                    .addOnFailureListener { showError() }
        } else if (responseCode == BillingResponse.USER_CANCELED) {
            Snackbar.make(root, R.string.donate_cancel_message, Snackbar.LENGTH_LONG).show()
        } else {
            FirebaseCrash.report(IllegalStateException("Unknown purchase error: $responseCode"))
            showError()
        }
    }

    private fun getConsumePurchasesTask(purchases: List<Purchase>): Task<Nothing> {
        val consumptions: MutableList<Task<String>> = ArrayList()

        for (purchase in purchases) {
            if (!purchase.sku.contains("single")) continue

            val consumption = TaskCompletionSource<String>()
            billingClient.consumeAsync(purchase.purchaseToken) { purchaseToken, resultCode ->
                if (resultCode == BillingResponse.OK || resultCode == BillingResponse.ITEM_NOT_OWNED) {
                    consumption.setResult(purchaseToken)
                } else {
                    val ex = IllegalStateException("Consumption failed with code $resultCode")
                    FirebaseCrash.report(ex)
                    consumption.setException(ex)
                }
            }
            consumptions.add(consumption.task)
        }

        return Tasks.whenAll(consumptions).continueWithTask(Continuation<Void, Task<Nothing>> {
            val unsuccessfulConsumptions = consumptions.find { !it.isSuccessful }

            return@Continuation if (unsuccessfulConsumptions == null) {
                Tasks.forResult(null)
            } else {
                Tasks.forException(unsuccessfulConsumptions.exception!!)
            }
        })
    }

    private fun updateProgress(isDoingAsyncWork: Boolean) {
        content.visibility = if (isDoingAsyncWork) View.GONE else View.VISIBLE
        progress.visibility = if (isDoingAsyncWork) View.VISIBLE else View.GONE
    }

    private fun showError() {
        updateProgress(false)
        Snackbar.make(root, R.string.general_error, Snackbar.LENGTH_LONG).show()
    }

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
