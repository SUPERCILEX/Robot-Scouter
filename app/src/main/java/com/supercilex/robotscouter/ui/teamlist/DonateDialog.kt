package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
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
import com.supercilex.robotscouter.ui.ManualDismissDialog
import com.supercilex.robotscouter.util.uid
import java.lang.ref.WeakReference

class DonateDialog : ManualDismissDialog(), SeekBar.OnSeekBarChangeListener, BillingClientStateListener {
    private val rootView by lazy { View.inflate(context, R.layout.dialog_donate, null) }
    private val content by lazy { rootView.findViewById<View>(R.id.content) }
    private val progress by lazy { rootView.findViewById<ProgressBar>(R.id.progress) }
    private val amountTextView by lazy { rootView.findViewById<TextView>(R.id.amount_textview) }
    private val amountSeekBar by lazy { rootView.findViewById<SeekBar>(R.id.amount) }
    private val monthlyCheckBox by lazy { rootView.findViewById<CheckBox>(R.id.monthly) }

    private val billingClient by lazy {
        BillingClient.Builder(context).setListener(purchaseListener).build()
    }
    private val billingClientReadyTask = TaskCompletionSource<@BillingResponse Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingClient.startConnection(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.donate)
            .setView(rootView)
            .setPositiveButton(R.string.donate, null)
            .setNegativeButton(android.R.string.cancel, null)
            .createAndSetup()

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
    }

    override fun onAttemptDismiss(): Boolean {
        updateProgress(true)

        val sku = "${amountSeekBar.progress + 1}${DonateDialog.SKU_BASE}${if (monthlyCheckBox.isChecked) "monthly" else "single"}"
        val type = if (monthlyCheckBox.isChecked) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP
        purchaseItem(sku, type).addOnFailureListener { showError() }

        return false
    }

    private fun handlePurchaseResponse(response: Task<Int>) = response.addOnCompleteListener {
        updateProgress(false)
    }.addOnSuccessListener {
        Toast.makeText(context, R.string.donate_thanks, Toast.LENGTH_LONG).show()
        dismiss()
    }.addOnFailureListener {
        it as PurchaseException
        if (it.errorCode == BillingResponse.USER_CANCELED) {
            Snackbar.make(rootView, R.string.donate_cancel_message, Snackbar.LENGTH_LONG).show()
        } else if (it.errorCode != BillingResponse.ITEM_ALREADY_OWNED) { // User owns subscription
            FirebaseCrash.report(it)
            showError()
        }
    }

    private fun purchaseItem(sku: String, type: String):
            Task<Nothing> = billingClientReadyTask.task.continueWithTask(Continuation<Int, Task<Nothing>> {
        val purchaseStartTask = TaskCompletionSource<Nothing>()

        val result = billingClient.launchBillingFlow(activity, BillingFlowParams.Builder()
                .setSku(sku)
                .setType(type)
                .setAccountId(uid?.hashCode().toString())
                .build())

        if (result != BillingResponse.OK) {
            if (result == BillingResponse.ITEM_ALREADY_OWNED) {
                billingClient.queryPurchaseHistoryAsync(type) {
                    if (it.responseCode != BillingResponse.OK) {
                        val ex = PurchaseException(it.responseCode,
                                message = "Purchase fetch failed with code ${it.responseCode} and sku $sku")
                        FirebaseCrash.report(ex)
                        purchaseStartTask.setException(ex)
                        return@queryPurchaseHistoryAsync
                    }

                    getConsumePurchasesTask(it.purchasesList)
                            .continueWithTask { purchaseItem(sku, type) }
                            .addOnSuccessListener { purchaseStartTask.setResult(null) }
                }
            } else {
                val ex = PurchaseException(result, sku)
                FirebaseCrash.report(ex)
                purchaseStartTask.setException(ex)
            }
        }

        return@Continuation purchaseStartTask.task
    })

    private fun getConsumePurchasesTask(purchases: List<Purchase>): Task<Nothing> {
        val consumptions: MutableList<Task<String>> = ArrayList()

        for (purchase in purchases) {
            if (!purchase.sku.contains("single")) continue

            val consumption = TaskCompletionSource<String>()
            billingClient.consumeAsync(purchase.purchaseToken) { purchaseToken, resultCode ->
                if (resultCode == BillingResponse.OK || resultCode == BillingResponse.ITEM_NOT_OWNED) {
                    consumption.setResult(purchaseToken)
                } else {
                    val ex = PurchaseException(resultCode,
                            message = "Consumption failed with code $resultCode and sku ${purchase.sku}")
                    FirebaseCrash.report(ex)
                    consumption.setException(ex)
                }
            }
            consumptions.add(consumption.task)
        }

        return Tasks.whenAll(consumptions).continueWithTask(Continuation<Void, Task<Nothing>> {
            val unsuccessfulConsumption = consumptions.find { !it.isSuccessful }

            return@Continuation if (unsuccessfulConsumption == null) {
                Tasks.forResult(null)
            } else {
                Tasks.forException(unsuccessfulConsumption.exception!!)
            }
        })
    }

    private fun updateProgress(isDoingAsyncWork: Boolean) {
        content.visibility = if (isDoingAsyncWork) View.GONE else View.VISIBLE
        progress.visibility = if (isDoingAsyncWork) View.VISIBLE else View.GONE

        val dialog = if (dialog == null) return else dialog as AlertDialog
        fun setEnabled(button: Button) {
            button.isEnabled = !isDoingAsyncWork
        }
        setEnabled(dialog.getButton(AlertDialog.BUTTON_POSITIVE))
        setEnabled(dialog.getButton(AlertDialog.BUTTON_NEGATIVE))
    }

    private fun showError() {
        updateProgress(false)
        Snackbar.make(rootView, R.string.general_error, Snackbar.LENGTH_LONG).show()
    }

    override fun onBillingSetupFinished(resultCode: Int) {
        purchaseListener.continuePurchase(this)
        billingClientReadyTask.setResult(resultCode)
    }

    override fun onBillingServiceDisconnected() = billingClient.startConnection(this)

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit

    companion object {
        private const val TAG = "DonateDialog"
        private const val SKU_BASE = "_donate_"

        private val purchaseListener = object : PurchasesUpdatedListener {
            private var dialog = WeakReference<DonateDialog>(null)
            private var savedSate: Pair<Int, List<Purchase>?>? = null

            fun continuePurchase(dialog: DonateDialog) {
                this.dialog = WeakReference(dialog)
                if (savedSate != null) onPurchasesUpdated(savedSate!!.first, savedSate!!.second)
            }

            override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {
                val dialog = dialog.get()
                if (dialog == null) {
                    savedSate = responseCode to purchases
                    return
                } else {
                    savedSate = null
                    dialog.updateProgress(true)
                }

                if (responseCode == BillingResponse.OK && purchases != null) {
                    dialog.handlePurchaseResponse(
                            dialog.getConsumePurchasesTask(purchases).continueWith { responseCode })
                } else {
                    dialog.handlePurchaseResponse(Tasks.forException(
                            PurchaseException(responseCode, purchases?.map { it.sku }.toString())))
                }
            }
        }

        fun show(manager: FragmentManager) = DonateDialog().show(manager, TAG)
    }
}

private class PurchaseException(val errorCode: Int,
                                sku: String = "",
                                message: String = "Purchase failed with error code $errorCode for sku $sku") :
        Exception(message)
