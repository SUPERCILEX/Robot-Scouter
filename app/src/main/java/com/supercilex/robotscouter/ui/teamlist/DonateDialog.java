package com.supercilex.robotscouter.ui.teamlist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.crash.FirebaseCrash;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.util.AsyncTaskExecutor;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Callable;

public class DonateDialog extends DialogFragment
        implements ServiceConnection, DialogInterface.OnShowListener, AdapterView.OnItemClickListener, OnCompleteListener<Void> {
    private static final String TAG = "DonateDialog";
    private static final String IS_PROGRESS_SHOWING_KEY = "is_progress_showing_key";

    private static final int RC_PURCHASE = 1001;
    private static final String[] ITEM_SKUS = {
            "1.00_donate_single", "1.00_donate_subscription",
            "2.00_donate_single", "2.00_donate_subscription",
            "5.00_donate_single", "5.00_donate_subscription",
            "10.00_donate_single", "10.00_donate_subscription"};

    private IInAppBillingService mService;
    private ProgressDialog mProgressDialog;

    public static void show(FragmentManager manager) {
        DonateDialog dialog = new DonateDialog();

        Bundle args = new Bundle();
        args.putBoolean(IS_PROGRESS_SHOWING_KEY, false);
        dialog.setArguments(args);

        dialog.show(manager, TAG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        getContext().bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.donate)
                .setItems(R.array.donate_items, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ((AlertDialog) dialog).getListView().setOnItemClickListener(this);
        if (getArguments().getBoolean(IS_PROGRESS_SHOWING_KEY)) initProgressDialog();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) getContext().unbindService(this);
        destroyProgressDialog();
    }

    private void initProgressDialog() {
        mProgressDialog = ProgressDialog.show(
                getContext(),
                "",
                getString(R.string.progress_dialog_loading),
                true);

        getArguments().putBoolean(IS_PROGRESS_SHOWING_KEY, true);
    }

    private void destroyProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PURCHASE && resultCode == Activity.RESULT_OK) {
            try {
                JSONObject purchaseData = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
                String sku = purchaseData.getString("productId");
                if (sku.contains("donate_single")) {
                    initProgressDialog();

                    String purchaseToken = purchaseData.getString("purchaseToken");
                    AsyncTaskExecutor.execute(new PurchaseConsumer(mService,
                                                                   getDialog(),
                                                                   getContext().getPackageName(),
                                                                   purchaseToken))
                            .addOnCompleteListener(this);
                }
            } catch (JSONException e) {
                FirebaseCrash.report(e);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mService == null) {
            showError();
            return;
        }

        try {
            Bundle buyIntentBundle = mService.getBuyIntent(
                    3,
                    getContext().getPackageName(),
                    ITEM_SKUS[position],
                    ITEM_SKUS[position].contains("subscription") ? "subs" : "inapp",
                    null);

            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent == null) {
                showError();
                return;
            }

            startIntentSenderForResult(
                    pendingIntent.getIntentSender(),
                    RC_PURCHASE,
                    new Intent(),
                    0,
                    0,
                    0,
                    null);
        } catch (RemoteException | IntentSender.SendIntentException e) {
            FirebaseCrash.report(e);
        }
    }

    private void showError() {
        Toast.makeText(getContext(), R.string.general_error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mService = IInAppBillingService.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mService = null;
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        destroyProgressDialog();
    }

    private static final class PurchaseConsumer implements Callable<Void> {
        private IInAppBillingService mService;
        private Dialog mDialog;
        private String mPackageName;
        private String mPurchaseToken;

        private PurchaseConsumer(IInAppBillingService service,
                                 Dialog dialog,
                                 String packageName,
                                 String purchaseToken) {
            mService = service;
            mDialog = dialog;
            mPackageName = packageName;
            mPurchaseToken = purchaseToken;
        }

        @Override
        public Void call() throws Exception {
            mService.consumePurchase(3, mPackageName, mPurchaseToken);
            mDialog.dismiss();
            return null;
        }
    }
}
