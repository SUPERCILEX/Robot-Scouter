package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.data.util.Builder;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.TaskFailureLogger;

public class TeamSender {
    private FragmentActivity mActivity;
    private Team mTeam;

    public TeamSender(FragmentActivity activity, Team team) {
        mActivity = activity;
        mTeam = team;
    }

    public static void launchInvitationIntent(FragmentActivity activity, Team team) {
        if (BaseHelper.isOffline(activity)) {
            BaseHelper.showSnackbar(activity,
                                    R.string.no_connection,
                                    Snackbar.LENGTH_LONG);
            return;
        }
        new TeamSender(activity, team).fetchKeysQuery();
    }

    private void fetchKeysQuery() {
        new KeysQueryBuilder(Scout.getIndicesRef()
                                     .orderByValue()
                                     .equalTo(mTeam.getNumberAsLong()),
                             TeamReceiver.SCOUT_QUERY_KEY)
                .build()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String scoutQuery) {
                        String deepLink = mTeam.getDeepLink() + scoutQuery;
                        mActivity.startActivityForResult(getInvitationIntent(deepLink), 9);
                    }
                })
                .addOnFailureListener(new TaskFailureLogger());
    }

    private Intent getInvitationIntent(String deepLink) {
        return new AppInviteInvitation.IntentBuilder(mActivity.getString(R.string.share_title,
                                                                         mTeam.getFormattedName()))
                .setMessage(mActivity.getString(R.string.share_message, mTeam.getFormattedName()))
                .setDeepLink(Uri.parse(deepLink))
                .setEmailSubject(mActivity.getString(R.string.share_call_to_action,
                                                     mTeam.getFormattedName()))
                .setEmailHtmlContent(getFormattedHtml())
                .build();
    }

    private String getFormattedHtml() {
        return String.format(Constants.HTML_IMPORT_TEAM,
                             mTeam.getFormattedName(),
                             mTeam.getFormattedName(),
                             mTeam.getMedia());
    }

    private static class KeysQueryBuilder implements Builder<Task<String>>, ValueEventListener {
        private Query mIndicesQuery;
        private String mKeyName;

        private TaskCompletionSource<String> mKeysTask = new TaskCompletionSource<>();

        public KeysQueryBuilder(Query indicesQuery, String keyName) {
            mIndicesQuery = indicesQuery;
            mKeyName = keyName;
        }

        @Override
        public Task<String> build() {
            mIndicesQuery.addListenerForSingleValueEvent(this);
            return mKeysTask.getTask();
        }

        @Override
        public void onDataChange(DataSnapshot snapshot) {
            StringBuilder builder = new StringBuilder();
            for (DataSnapshot keySnapshot : snapshot.getChildren()) {
                builder.append('&')
                        .append(mKeyName)
                        .append('=')
                        .append(keySnapshot.getKey());
            }
            mKeysTask.setResult(builder.toString());
        }

        @Override
        public void onCancelled(DatabaseError error) {
            mKeysTask.setException(error.toException());
        }
    }
}
