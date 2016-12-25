package com.supercilex.robotscouter.ui.teamlist;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.TaskFailureLogger;

public class DeepLinkBuilder {
    private static final String APP_LINK_START = "https://supercilex.github.io/?" + DeepLinkHandler.TEAM_KEY + "=";
    private static final String APP_LINK_END = "&" + DeepLinkHandler.UTM_SOURCE + "=" + DeepLinkHandler.UTM_SOURCE_VALUE;

    public static void launchInvitationIntent(final FragmentActivity activity, final Team team) {
        new KeysQueryBuilder(Scout.getIndicesRef(), DeepLinkHandler.SCOUT_KEY)
                .build()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String scoutQuery) {
                        String deepLink = APP_LINK_START +
                                team.getKey() +
                                ":" +
                                team.getNumber() +
                                scoutQuery +
                                APP_LINK_END;
                        activity.startActivityForResult(getInvitationIntent(activity,
                                                                            team,
                                                                            deepLink), 9);
                    }
                })
                .addOnFailureListener(new TaskFailureLogger());
    }

    private static Intent getInvitationIntent(Context context, Team team, String deepLink) {
        return new AppInviteInvitation.IntentBuilder(String.format(context.getString(R.string.share_title),
                                                                   team.getFormattedName()))
                .setMessage(String.format(context.getString(R.string.share_message),
                                          team.getFormattedName()))
                .setDeepLink(Uri.parse(deepLink))
                .setEmailSubject(String.format(context.getString(R.string.share_call_to_action),
                                               team.getFormattedName()))
                .setEmailHtmlContent(getFormattedHtml(team))
                .build();
    }

    private static String getFormattedHtml(Team team) {
        return String.format(Constants.HTML_IMPORT_TEAM,
                             team.getFormattedName(),
                             team.getFormattedName(),
                             team.getMedia());
    }

    private static class KeysQueryBuilder implements Builder<Task<String>>, ValueEventListener {
        private Query mIndicesQuery;
        private String mKeyName;

        private TaskCompletionSource<String> mKeysTask;

        public KeysQueryBuilder(Query indicesQuery, String keyName) {
            mIndicesQuery = indicesQuery;
            mKeyName = keyName;
            mKeysTask = new TaskCompletionSource<>();
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
                builder.append("&")
                        .append(mKeyName)
                        .append("=")
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
