package com.supercilex.robotscouter.ui;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.User;
import com.supercilex.robotscouter.data.util.UserHelper;
import com.supercilex.robotscouter.ui.teamlist.IntentReceiver;

import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.logLoginEvent;
import static com.supercilex.robotscouter.util.AnalyticsUtilsKt.updateAnalyticsUserId;
import static com.supercilex.robotscouter.util.ConstantsKt.ALL_PROVIDERS;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_SCOUT_INDICES;
import static com.supercilex.robotscouter.util.ConstantsKt.FIREBASE_SCOUT_TEMPLATES;
import static com.supercilex.robotscouter.util.RemoteConfigUtilsKt.fetchAndActivate;

public final class AuthHelper implements View.OnClickListener {
    private static final int RC_SIGN_IN = 100;

    private final FragmentActivity mActivity;

    private IntentReceiver mLinkReceiver;
    private MenuItem mActionSignIn;
    private MenuItem mActionSignOut;

    public AuthHelper(FragmentActivity activity) {
        mActivity = activity;

        if (isSignedIn()) {
            initDeepLinkReceiver();
        } else {
            signInAnonymously();
        }
    }

    @Nullable
    public static FirebaseUser getUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    @Nullable
    public static String getUid() {
        return getUser() == null ? null : getUser().getUid();
    }

    public static boolean isSignedIn() {
        return getUser() != null;
    }

    public static boolean isFullUser() {
        return isSignedIn() && !getUser().isAnonymous();
    }

    public static Task<FirebaseAuth> onSignedIn() {
        final TaskCompletionSource<FirebaseAuth> signInTask = new TaskCompletionSource<>();
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
                if (auth.getCurrentUser() == null) {
                    signInAnonymouslyDbInit();
                } else {
                    signInTask.trySetResult(auth);
                    FirebaseAuth.getInstance().removeAuthStateListener(this);
                }
            }
        });
        return signInTask.getTask();
    }

    private static Task<AuthResult> signInAnonymouslyInitBasic() {
        return FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(result -> updateAnalyticsUserId());
    }

    private static Task<AuthResult> signInAnonymouslyDbInit() {
        return signInAnonymouslyInitBasic().addOnSuccessListener(result -> DatabaseInitializer.init());
    }

    public void initMenu(Menu menu) {
        mActionSignIn = menu.findItem(R.id.action_sign_in);
        mActionSignOut = menu.findItem(R.id.action_sign_out);
        toggleMenuSignIn(isSignedIn() && !getUser().isAnonymous());
    }

    private void initDeepLinkReceiver() {
        if (mLinkReceiver == null) {
            mLinkReceiver = IntentReceiver.init(mActivity);
        }
    }

    public void signIn() {
        mActivity.startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(ALL_PROVIDERS)
                        .setLogo(R.drawable.ic_logo)
                        .setTosUrl("https://supercilex.github.io/privacy-policy/")
                        .setIsAccountLinkingEnabled(true)
                        .build(),
                RC_SIGN_IN);
    }

    private void signInAnonymously() {
        signInAnonymouslyDbInit()
                .addOnSuccessListener(mActivity, result -> initDeepLinkReceiver())
                .addOnFailureListener(mActivity, e ->
                        Snackbar.make(mActivity.findViewById(R.id.root),
                                      R.string.anonymous_sign_in_failed,
                                      Snackbar.LENGTH_LONG)
                                .setAction(R.string.sign_in, this)
                                .show());
    }

    public void signOut() {
        AuthUI.getInstance()
                .signOut(mActivity)
                .addOnSuccessListener(aVoid -> {
                    signInAnonymouslyInitBasic();
                    FirebaseAppIndex.getInstance().removeAll();
                })
                .addOnSuccessListener(mActivity, aVoid -> toggleMenuSignIn(false));
    }

    public void showSignInResolution() {
        Snackbar.make(mActivity.findViewById(R.id.root),
                      R.string.sign_in_required,
                      Snackbar.LENGTH_LONG)
                .setAction(R.string.sign_in, this)
                .show();
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == ResultCodes.OK) {
                Snackbar.make(mActivity.findViewById(R.id.root),
                              R.string.signed_in,
                              Snackbar.LENGTH_LONG)
                        .show();
                toggleMenuSignIn(true);

                initDeepLinkReceiver();

                UserHelper userHelper = new User.Builder(getUid())
                        .setEmail(getUser().getEmail())
                        .setName(getUser().getDisplayName())
                        .setPhotoUrl(getUser().getPhotoUrl())
                        .build()
                        .getHelper();
                userHelper.add();
                if (response != null) userHelper.transferData(response.getPrevUid());

                logLoginEvent();

                return true;
            } else {
                if (response == null) return false; // User cancelled sign in

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Snackbar.make(mActivity.findViewById(R.id.root),
                                  R.string.no_connection,
                                  Snackbar.LENGTH_LONG)
                            .setAction(R.string.try_again, this)
                            .show();
                    return false;
                }

                Snackbar.make(mActivity.findViewById(R.id.root),
                              R.string.sign_in_failed,
                              Snackbar.LENGTH_LONG)
                        .setAction(R.string.try_again, this)
                        .show();
            }
        }

        return false;
    }

    private void toggleMenuSignIn(boolean isSignedIn) {
        mActionSignIn.setVisible(!isSignedIn);
        mActionSignOut.setVisible(isSignedIn);
    }

    @Override
    public void onClick(View v) {
        signIn();
    }

    private static final class DatabaseInitializer implements ValueEventListener, OnSuccessListener<Void> {
        private static final String SHOULD_CACHE_DB = "should_cache_db";

        private DatabaseInitializer() {
            fetchAndActivate().addOnSuccessListener(this);
            FIREBASE_SCOUT_INDICES.addListenerForSingleValueEvent(this);
        }

        public static void init() {
            new DatabaseInitializer();
        }

        @Override
        public void onSuccess(Void aVoid) {
            if (FirebaseRemoteConfig.getInstance().getBoolean(SHOULD_CACHE_DB)) {
                FIREBASE_SCOUT_TEMPLATES.addListenerForSingleValueEvent(this);
            }
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            // This allows the database to work offline without any setup
        }

        @Override
        public void onCancelled(DatabaseError error) {
            FirebaseCrash.report(error.toException());
        }
    }
}
