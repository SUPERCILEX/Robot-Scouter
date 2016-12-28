package com.supercilex.robotscouter.ui.teamlist;

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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.model.User;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.TaskFailureLogger;

public class AuthHelper {
    private static final int RC_SIGN_IN = 100;

    private static FirebaseAuth sAuth;

    private TeamReceiver mLinkReceiver;

    private FragmentActivity mActivity;
    private TeamListFragment mTeamsFragment;
    private MenuItem mActionSignIn;
    private MenuItem mActionSignOut;

    private AuthHelper(TeamListActivity activity) {
        mActivity = activity;
        mTeamsFragment = (TeamListFragment) mActivity.getSupportFragmentManager()
                .findFragmentByTag("team_list_fragment");
    }

    public static FirebaseAuth getAuth() {
        synchronized (BaseHelper.class) {
            if (sAuth == null) {
                sAuth = FirebaseAuth.getInstance();
            }
        }
        return sAuth;
    }

    @Nullable
    public static FirebaseUser getUser() {
        return getAuth().getCurrentUser();
    }

    @Nullable
    public static String getUid() {
        return getUser() == null ? null : getUser().getUid();
    }

    public static boolean isSignedIn() {
        return getUser() != null;
    }

    public static AuthHelper init(TeamListActivity activity) {
        AuthHelper helper = new AuthHelper(activity);
        if (isSignedIn()) {
            helper.initDeepLinkReceiver();
        } else {
            helper.signInAnonymously();
        }
        return helper;
    }

    public void initMenu(Menu menu) {
        mActionSignIn = menu.findItem(R.id.action_sign_in);
        mActionSignOut = menu.findItem(R.id.action_sign_out);
        if (isSignedIn() && !getUser().isAnonymous()) {
            toggleMenuSignIn(true);
        } else {
            toggleMenuSignIn(false);
        }
    }

    private void initDeepLinkReceiver() {
        if (mLinkReceiver == null) {
            mLinkReceiver = TeamReceiver.init(mActivity);
        }
    }

    public void signIn() {
        mActivity.startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setProviders(Constants.ALL_PROVIDERS)
                        .setTheme(R.style.RobotScouter)
                        .setLogo(R.drawable.launch_logo)
                        .setShouldLinkAccounts(true)
                        .setTosUrl("https://supercilex.github.io/privacy-policy/")
                        .build(),
                RC_SIGN_IN);
    }

    private void signInAnonymously() {
        getAuth().signInAnonymously()
                .addOnSuccessListener(mActivity, new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult result) {
                        mTeamsFragment.resetAdapter();
                        initDeepLinkReceiver();
                        DatabaseInitializer.init();
                    }
                })
                .addOnFailureListener(mActivity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        BaseHelper.showSnackbar(mActivity,
                                                R.string.anonymous_sign_in_failed,
                                                Snackbar.LENGTH_LONG,
                                                R.string.sign_in,
                                                new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        signIn();
                                                    }
                                                });
                    }
                })
                .addOnFailureListener(new TaskFailureLogger());
    }

    public void signOut() {
        AuthUI.getInstance()
                .signOut(mActivity)
                .addOnSuccessListener(mActivity, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        mTeamsFragment.cleanup();
                        toggleMenuSignIn(false);
                        signInAnonymously();
                    }
                })
                .addOnFailureListener(new TaskFailureLogger());
    }

    public void startSignInResolution() {
        BaseHelper.showSnackbar(mActivity,
                                R.string.sign_in_required,
                                Snackbar.LENGTH_LONG,
                                R.string.sign_in,
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        signIn();
                                    }
                                });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == ResultCodes.OK) {
                mTeamsFragment.resetAdapter();
                BaseHelper.showSnackbar(mActivity, R.string.signed_in, Snackbar.LENGTH_LONG);
                toggleMenuSignIn(true);

                initDeepLinkReceiver();

                User user = new User.Builder(getUser().getUid())
                        .setEmail(getUser().getEmail())
                        .setName(getUser().getDisplayName())
                        .setPhotoUrl(getUser().getPhotoUrl())
                        .build();
                user.add();
                if (response != null) user.transferData(response.getPrevUid());
            } else {
                if (response == null) return; // User cancelled sign in

                if (response.getErrorCode() == ErrorCodes.NO_NETWORK) {
                    BaseHelper.showSnackbar(mActivity,
                                            R.string.no_connection,
                                            Snackbar.LENGTH_LONG,
                                            R.string.try_again,
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    signIn();
                                                }
                                            });
                    return;
                }

                BaseHelper.showSnackbar(mActivity,
                                        R.string.sign_in_failed,
                                        Snackbar.LENGTH_LONG,
                                        R.string.try_again,
                                        new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                signIn();
                                            }
                                        });
            }
        }
    }

    private void toggleMenuSignIn(boolean isSignedIn) {
        mActionSignIn.setVisible(!isSignedIn);
        mActionSignOut.setVisible(isSignedIn);
    }

    private static final class DatabaseInitializer implements ValueEventListener {
        private DatabaseInitializer() {
            Team.getIndicesRef().addListenerForSingleValueEvent(this);
            Scout.getIndicesRef().addListenerForSingleValueEvent(this);
            Constants.FIREBASE_DEFAULT_TEMPLATE.addListenerForSingleValueEvent(this);
            Constants.FIREBASE_SCOUT_TEMPLATES.addListenerForSingleValueEvent(this);
        }

        public static void init() {
            new DatabaseInitializer();
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            // This allows the database to work offline
        }

        @Override
        public void onCancelled(DatabaseError error) {
            FirebaseCrash.report(error.toException());
        }
    }
}
