package com.supercilex.robotscouter.ui.teamlist

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import com.android.vending.billing.IInAppBillingService
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.crash.FirebaseCrash
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.createAndListen
import com.supercilex.robotscouter.util.show
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Callable

class DonateDialog : DialogFragment(), ServiceConnection, AdapterView.OnItemClickListener, OnCompleteListener<Void> {
    private var service: IInAppBillingService? = null
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent("com.android.vending.billing.InAppBillingService.BIND")
        serviceIntent.`package` = "com.android.vending"
        context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = AlertDialog.Builder(context)
            .setTitle(R.string.donate)
            .setItems(R.array.donate_items, null)
            .setNegativeButton(android.R.string.cancel, null)
            .createAndListen {
                listView.onItemClickListener = this@DonateDialog
                if (arguments.getBoolean(KEY_IS_PROGRESS_SHOWING)) initProgressDialog()
            }

    override fun onDestroy() {
        super.onDestroy()
        if (service != null) context.unbindService(this)
        destroyProgressDialog()
        RobotScouter.getRefWatcher(activity).watch(this)
    }

    private fun initProgressDialog() {
        progressDialog = ProgressDialog.show(
                context,
                "",
                getString(R.string.progress_dialog_loading),
                true)

        arguments.putBoolean(KEY_IS_PROGRESS_SHOWING, true)
    }

    private fun destroyProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_PURCHASE && resultCode == Activity.RESULT_OK) {
            try {
                val purchaseData = JSONObject(data!!.getStringExtra("INAPP_PURCHASE_DATA"))
                val sku = purchaseData.getString("productId")
                if (sku.contains("donate_single")) {
                    initProgressDialog()

                    val purchaseToken = purchaseData.getString("purchaseToken")
                    AsyncTaskExecutor.execute(PurchaseConsumer(
                            service!!,
                            dialog,
                            context.packageName,
                            purchaseToken))
                            .addOnCompleteListener(this)
                }
            } catch (e: JSONException) {
                FirebaseCrash.report(e)
            }

        }
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        if (service == null) {
            showError()
            return
        }

        val buyIntentBundle: Bundle
        try {
            buyIntentBundle = service!!.getBuyIntent(
                    3,
                    context.packageName,
                    ITEM_SKUS[position],
                    if (ITEM_SKUS[position].contains("subscription")) "subs" else "inapp",
                    null)
        } catch (e: RemoteException) {
            showError()
            return
        }

        val pendingIntent: PendingIntent? = buyIntentBundle.getParcelable<PendingIntent>("BUY_INTENT")

        if (pendingIntent == null) {
            showError()
            return
        }

        try {
            startIntentSenderForResult(
                    pendingIntent.intentSender,
                    RC_PURCHASE,
                    Intent(),
                    0,
                    0,
                    0,
                    null)
        } catch (e: IntentSender.SendIntentException) {
            FirebaseCrash.report(e)
        }
    }

    private fun showError() =
            Toast.makeText(context, R.string.general_error, Toast.LENGTH_SHORT).show()

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        this.service = IInAppBillingService.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        service = null
    }

    override fun onComplete(task: Task<Void>) = destroyProgressDialog()

    private class PurchaseConsumer(
            private val mService: IInAppBillingService,
            private val mDialog: Dialog,
            private val mPackageName: String,
            private val mPurchaseToken: String) : Callable<Void> {
        @Throws(Exception::class)
        override fun call(): Void? {
            mService.consumePurchase(3, mPackageName, mPurchaseToken)
            mDialog.dismiss()
            return null
        }
    }

    companion object {
        private val TAG = "DonateDialog"
        private val KEY_IS_PROGRESS_SHOWING = "is_progress_showing_key"

        private val RC_PURCHASE = 1001
        private val ITEM_SKUS: List<String> = listOf(
                "1.00_donate_single",
                "1.00_donate_subscription",
                "2.00_donate_single",
                "2.00_donate_subscription",
                "5.00_donate_single",
                "5.00_donate_subscription",
                "10.00_donate_single",
                "10.00_donate_subscription")

        fun show(manager: FragmentManager) =
                DonateDialog().show(manager, TAG) { putBoolean(KEY_IS_PROGRESS_SHOWING, false) }
    }
}
