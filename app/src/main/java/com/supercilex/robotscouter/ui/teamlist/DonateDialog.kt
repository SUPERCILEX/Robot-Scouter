package com.supercilex.robotscouter.ui.teamlist

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.crashlytics.android.Crashlytics
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.isInTestMode
import com.supercilex.robotscouter.util.ui.ManualDismissDialog
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar
import com.supercilex.robotscouter.util.uid
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.longToast

class DonateDialog : ManualDismissDialog(), SeekBar.OnSeekBarChangeListener,
        BillingClientStateListener, PurchasesUpdatedListener {
    private val content: View by bindView(R.id.content)
    private val progress: ContentLoadingProgressBar by bindView(R.id.progress)
    private val amountTextView: TextView by bindView(R.id.amount_textview)
    private val amountSeekBar: SeekBar by bindView(R.id.amount)
    private val monthlyCheckBox: CheckBox by bindView(R.id.monthly)

    private val billingClient by unsafeLazy {
        BillingClient.newBuilder(context!!).setListener(this).build()
    }
    private val billingClientReadyTask = TaskCompletionSource<@BillingResponse Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingClient.startConnection(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context!!)
            .setTitle(R.string.donate_title)
            .setView(View.inflate(context, R.layout.dialog_donate, null))
            .setPositiveButton(R.string.donate_title, null)
            .setNegativeButton(android.R.string.cancel, null)
            .createAndSetup(savedInstanceState)

    override fun onShow(dialog: DialogInterface, savedInstanceState: Bundle?) {
        super.onShow(dialog, savedInstanceState)
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

        val sku = "${amountSeekBar.progress + 1}${DonateDialog.SKU_BASE}" +
                if (monthlyCheckBox.isChecked) "monthly" else "single"
        val type = if (monthlyCheckBox.isChecked) BillingClient.SkuType.SUBS else BillingClient.SkuType.INAPP
        purchaseItem(sku, type).addOnFailureListener { showError() }

        return false
    }

    private fun handlePurchaseResponse(response: Task<Int>) = response.addOnCompleteListener {
        updateProgress(false)
    }.addOnSuccessListener {
        longToast(R.string.donate_thanks_message)
        dismiss()
    }.addOnFailureListener {
        it as PurchaseException
        if (it.errorCode == BillingResponse.USER_CANCELED) {
            longSnackbar(content, R.string.donate_cancel_message)
        } else if (it.errorCode != BillingResponse.ITEM_ALREADY_OWNED // User owns subscription
                && !isInTestMode) {
            FirebaseCrash.report(it)
            Crashlytics.logException(it)
            showError()
        }
    }

    private fun purchaseItem(
            sku: String,
            @BillingClient.SkuType type: String
    ): Task<Nothing> = billingClientReadyTask.task.continueWithTask(Continuation<Int, Task<Nothing>> {
        val purchaseStartTask = TaskCompletionSource<Nothing>()

        val result = billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setSku(sku)
                .setType(type)
                .setAccountId(uid?.hashCode().toString())
                .build())

        if (result != BillingResponse.OK) {
            if (result == BillingResponse.ITEM_ALREADY_OWNED) {
                billingClient.queryPurchaseHistoryAsync(type) { responseCode, purchasesList ->
                    if (responseCode != BillingResponse.OK) {
                        val e = PurchaseException(
                                responseCode,
                                message = "Purchase fetch failed with code $responseCode and sku $sku"
                        )
                        FirebaseCrash.report(e)
                        Crashlytics.logException(e)
                        purchaseStartTask.setException(e)
                        return@queryPurchaseHistoryAsync
                    }

                    getConsumePurchasesTask(purchasesList)
                            .continueWithTask { purchaseItem(sku, type) }
                            .addOnSuccessListener { purchaseStartTask.setResult(null) }
                }
            } else {
                PurchaseException(result, sku).let {
                    FirebaseCrash.report(it)
                    Crashlytics.logException(it)
                    purchaseStartTask.setException(it)
                }
            }
        }

        return@Continuation purchaseStartTask.task
    })

    private fun getConsumePurchasesTask(purchases: List<Purchase>): Task<Nothing> {
        val consumptions: MutableList<Task<String>> = ArrayList()

        for (purchase in purchases) {
            if (!purchase.sku.contains("single")) continue

            val consumption = TaskCompletionSource<String>()
            billingClient.consumeAsync(purchase.purchaseToken) { resultCode, purchaseToken ->
                if (resultCode == BillingResponse.OK || resultCode == BillingResponse.ITEM_NOT_OWNED) {
                    consumption.setResult(purchaseToken)
                } else {
                    val e = PurchaseException(
                            resultCode,
                            message = "Consumption failed with code $resultCode and sku ${purchase.sku}"
                    )
                    FirebaseCrash.report(e)
                    Crashlytics.logException(e)
                    consumption.setException(e)
                }
            }
            consumptions += consumption.task
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
        if (isDoingAsyncWork) progress.show(Runnable { content.visibility = View.GONE })
        else progress.hide(callback = Runnable { content.visibility = View.VISIBLE })

        val dialog = if (dialog == null) return else dialog as AlertDialog
        fun setEnabled(button: Button) {
            button.isEnabled = !isDoingAsyncWork
        }
        setEnabled(dialog.getButton(AlertDialog.BUTTON_POSITIVE))
        setEnabled(dialog.getButton(AlertDialog.BUTTON_NEGATIVE))
    }

    private fun showError() {
        updateProgress(false)
        longSnackbar(content, R.string.fui_general_error)
    }

    override fun onBillingSetupFinished(resultCode: Int) {
        billingClientReadyTask.setResult(resultCode)
    }

    override fun onBillingServiceDisconnected() = billingClient.startConnection(this)

    override fun onPurchasesUpdated(responseCode: Int, purchases: List<Purchase>?) {
        updateProgress(true)
        if (responseCode == BillingResponse.OK && purchases != null) {
            handlePurchaseResponse(getConsumePurchasesTask(purchases).continueWith { responseCode })
        } else {
            handlePurchaseResponse(Tasks.forException(
                    PurchaseException(responseCode, purchases?.map { it.sku }.toString())))
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit

    private class PurchaseException(
            val errorCode: Int,
            sku: String = "",
            message: String = "Purchase failed with error code $errorCode for sku $sku"
    ) : Exception(message)

    companion object {
        private const val TAG = "DonateDialog"
        private const val SKU_BASE = "_donate_"

        fun show(manager: FragmentManager) = DonateDialog().show(manager, TAG)
    }
}
