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

import java.util.List;

public final class TeamSender {
    private FragmentActivity mActivity;
    private List<Team> mTeams;

    private TeamSender(FragmentActivity activity, List<Team> teams) {
        mActivity = activity;
        mTeams = teams;

        StringBuilder deepLinkBuilder = new StringBuilder(TeamReceiver.APP_LINK_BASE);
        for (Team team : teams) {
            deepLinkBuilder.append(team.getLinkKeyNumberPair());
        }

        mActivity.startActivityForResult(getInvitationIntent(deepLinkBuilder.toString()), 9);
    }

    public static void launchInvitationIntent(FragmentActivity activity, List<Team> teams) {
        if (BaseHelper.isOffline(activity)) {
            BaseHelper.showSnackbar(activity, R.string.no_connection, Snackbar.LENGTH_LONG);
            return;
        }
        if (!teams.isEmpty()) new TeamSender(activity, teams);
    }

    private Intent getInvitationIntent(String deepLink) {
        return new AppInviteInvitation.IntentBuilder(mActivity.getString(R.string.share_title,
                                                                         getFormattedTeamName()))
                .setMessage(mActivity.getString(R.string.share_message, getFormattedTeamName()))
                .setDeepLink(Uri.parse(deepLink))
                .setEmailSubject(mActivity.getString(R.string.share_call_to_action,
                                                     getFormattedTeamName()))
                .setEmailHtmlContent(getFormattedHtml())
                .build();
    }

    private String getFormattedHtml() {
        return String.format(Constants.HTML_IMPORT_TEAM,
                             getFormattedTeamName(),
                             getFormattedTeamName(),
                             mTeams.get(0).getMedia());
    }

    private String getFormattedTeamName() {
        String formattedName = mTeams.get(0).getFormattedName();
        return mTeams.size() == 1 ? formattedName : formattedName + " and more";
    }
}
