package com.supercilex.robotscouter.ui.teamlist;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.util.BaseHelper;
import com.supercilex.robotscouter.util.Constants;

public class TeamSender {
    private FragmentActivity mActivity;
    private Team mTeam;

    public TeamSender(FragmentActivity activity, Team team) {
        mActivity = activity;
        mTeam = team;

        String deepLink = mTeam.getDeepLink();
        mActivity.startActivityForResult(getInvitationIntent(deepLink), 9);
    }

    public static void launchInvitationIntent(FragmentActivity activity, Team team) {
        if (BaseHelper.isOffline(activity)) {
            BaseHelper.showSnackbar(activity, R.string.no_connection, Snackbar.LENGTH_LONG);
            return;
        }
        new TeamSender(activity, team);
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
}
