package com.supercilex.robotscouter.ui;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.appinvite.AppInviteInvitation;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.util.TeamHelper;
import com.supercilex.robotscouter.ui.teamlist.TeamReceiver;
import com.supercilex.robotscouter.util.Constants;
import com.supercilex.robotscouter.util.MiscellaneousHelper;

import java.util.List;

public class TeamSender {
    private FragmentActivity mActivity;
    private List<TeamHelper> mTeamHelpers;

    protected TeamSender(FragmentActivity activity, List<TeamHelper> teamHelpers) {
        mActivity = activity;
        mTeamHelpers = teamHelpers;

        StringBuilder deepLinkBuilder = new StringBuilder(TeamReceiver.APP_LINK_BASE);
        for (TeamHelper teamHelper : teamHelpers) {
            deepLinkBuilder.append(teamHelper.getLinkKeyNumberPair());
        }

        mActivity.startActivityForResult(getInvitationIntent(deepLinkBuilder.toString()), 9);
    }

    /**
     * @return true if a share intent was launched, false otherwise
     */
    public static boolean launchInvitationIntent(FragmentActivity activity,
                                                 List<TeamHelper> teamHelpers) {
        if (MiscellaneousHelper.isOffline(activity)) {
            Snackbar.make(activity.findViewById(R.id.root),
                          R.string.no_connection,
                          Snackbar.LENGTH_LONG)
                    .show();
            return false;
        }
        if (teamHelpers.isEmpty()) return false;

        new TeamSender(activity, teamHelpers);
        return true;
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
                             mTeamHelpers.get(0).getTeam().getMedia());
    }

    private String getFormattedTeamName() {
        String formattedName = mTeamHelpers.get(0).getFormattedName();
        return mTeamHelpers.size() == 1 ? formattedName : formattedName + " and more";
    }
}
