package com.supercilex.robotscouter

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
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
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.logCrashLog
import com.supercilex.robotscouter.core.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.core.ui.longSnackbar
import com.supercilex.robotscouter.core.ui.longToast
import com.supercilex.robotscouter.core.ui.snackbar
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_donate.*

internal class DonateDialog : BottomSheetDialogFragmentBase(), View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, BillingClientStateListener, PurchasesUpdatedListener {
    override val containerView by unsafeLazy {
        View.inflate(context, R.layout.dialog_donate, null) as ViewGroup
    }

    private val billingClient by unsafeLazy {
        BillingClient.newBuilder(requireContext()).setListener(this).build()
    }
    private val billingClientReadyTask = TaskCompletionSource<@BillingResponse Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billingClient.startConnection(this)
    }

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        donate.setOnClickListener(this)
        amountSeekBar.setOnSeekBarChangeListener(this)
    }

    override fun onShow(dialog: DialogInterface) {
        onProgressChanged(amountSeekBar, 0, false) // Init state
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        amount.text = getString(R.string.donate_amount, progress + 1)

        val width = seekBar.width - (seekBar.paddingLeft + seekBar.paddingRight)
        val thumbPos = seekBar.paddingLeft + (width * progress / seekBar.max)
        val offset =
                seekBar.thumbOffset - if (progress == seekBar.max) 2 * seekBar.max else seekBar.max
        amount.x = (thumbPos + offset).toFloat()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (billingClient.isReady) billingClient.endConnection()
    }

    override fun onClick(v: View) {
        if (!billingClient.isReady) {
            showError()
            return
        }

        updateProgress(true)

        val sku = "${amountSeekBar.progress + 1}$SKU_BASE" +
                if (monthly.isChecked) "monthly" else "single"
        purchaseItem(sku, if (monthly.isChecked) {
            BillingClient.SkuType.SUBS
        } else {
            BillingClient.SkuType.INAPP
        }).addOnFailureListener { showError() }
    }

    private fun handlePurchaseResponse(response: Task<Int>) = response.addOnCompleteListener {
        updateProgress(false)
    }.addOnSuccessListener {
        longToast(R.string.donate_thanks_message)
        dismiss()
    }.addOnFailureListener {
        when ((it as PurchaseException).errorCode) {
            BillingResponse.USER_CANCELED -> longSnackbar(view, R.string.donate_cancel_message)
            BillingResponse.SERVICE_UNAVAILABLE -> longSnackbar(view, R.string.no_connection)
            BillingResponse.ITEM_ALREADY_OWNED, BillingResponse.ERROR -> Unit
            else -> {
                CrashLogger.onFailure(it)
                showError()
            }
        }
    }

    private fun purchaseItem(
            sku: String,
            @BillingClient.SkuType type: String
    ): Task<Unit?> = billingClientReadyTask.task.continueWithTask(Continuation<Int, Task<Unit?>> {
        val purchaseStartTask = TaskCompletionSource<Unit?>()

        logCrashLog("Starting purchase flow for sku: $sku")
        val result = billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder()
                .setSku(sku)
                .setType(type)
                .setAccountId(uid?.hashCode().toString())
                .build())

        if (result == BillingResponse.OK) {
            purchaseStartTask.setResult(null)
        } else if (result == BillingResponse.ITEM_ALREADY_OWNED) {
            billingClient.queryPurchaseHistoryAsync(type) { responseCode, purchasesList ->
                if (responseCode != BillingResponse.OK) {
                    val e = PurchaseException(
                            responseCode,
                            message = "Purchase fetch failed with code $responseCode and sku $sku"
                    )
                    CrashLogger.onFailure(e)
                    purchaseStartTask.setException(e)
                    return@queryPurchaseHistoryAsync
                }

                getConsumePurchasesTask(purchasesList)
                        .continueWithTask { purchaseItem(sku, type) }
                        .addOnSuccessListener { purchaseStartTask.setResult(null) }
            }
        } else if (result == BillingResponse.FEATURE_NOT_SUPPORTED) {
            snackbar(view, R.string.error_unknown)
        } else {
            PurchaseException(result, sku).let {
                CrashLogger.onFailure(it)
                purchaseStartTask.setException(it)
            }
        }

        return@Continuation purchaseStartTask.task
    })

    private fun getConsumePurchasesTask(purchases: List<Purchase>): Task<*> {
        val consumptions = purchases.filter {
            it.sku.contains("single")
        }.map {
            val consumption = TaskCompletionSource<String>()
            billingClient.consumeAsync(it.purchaseToken) { resultCode, purchaseToken ->
                if (resultCode == BillingResponse.OK || resultCode == BillingResponse.ITEM_NOT_OWNED) {
                    consumption.setResult(purchaseToken)
                } else {
                    val e = PurchaseException(
                            resultCode,
                            message = "Consumption failed with code $resultCode and sku ${it.sku}"
                    )
                    CrashLogger.onFailure(e)
                    consumption.setException(e)
                }
            }
            consumption.task
        }

        return Tasks.whenAllSuccess<String>(consumptions)
    }

    private fun updateProgress(isDoingAsyncWork: Boolean) {
        if (isDoingAsyncWork) progress.show(Runnable { main.isVisible = false })
        else progress.hide(true, Runnable { main.isVisible = true })
        donate.isEnabled = !isDoingAsyncWork
    }

    private fun showError() {
        updateProgress(false)
        longSnackbar(view, R.string.error_unknown)
    }

    override fun onBillingSetupFinished(resultCode: Int) {
        billingClientReadyTask.trySetResult(resultCode)
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
