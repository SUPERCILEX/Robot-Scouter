package com.supercilex.robotscouter.ui.teamlist;

import android.support.annotation.Keep;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.supercilex.robotscouter.R;
import com.supercilex.robotscouter.data.model.Team;
import com.supercilex.robotscouter.ui.DeleteTeamDialog;
import com.supercilex.robotscouter.ui.TeamDetailsDialog;
import com.supercilex.robotscouter.ui.scout.ScoutActivity;

public class TeamHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
    private Fragment mFragment;
    private Team mTeam;

    private ConstraintLayout mRowLayout;
    private TextView mNumber;
    private TextView mName;
    private ImageView mLogo;
    private ImageButton mNewScout;

    @Keep
    public TeamHolder(View itemView) {
        super(itemView);
        mRowLayout = (ConstraintLayout) itemView;
        mNumber = (TextView) itemView.findViewById(R.id.number);
        mName = (TextView) itemView.findViewById(R.id.name);
        mLogo = (ImageView) itemView.findViewById(R.id.logo);
        mNewScout = (ImageButton) itemView.findViewById(R.id.new_scout);
    }

    public void bind(Fragment fragment, Team team) {
        mFragment = fragment;
        mTeam = team;
        setTeamNumber();
        setTeamName();
        setTeamLogo();
        mRowLayout.setOnClickListener(this);
        mRowLayout.setOnCreateContextMenuListener(this);
        mNewScout.setOnClickListener(this);
    }

    private void setTeamNumber() {
        mNumber.setText(mTeam.getNumber());
    }

    private void setTeamName() {
        if (TextUtils.isEmpty(mTeam.getName())) {
            mName.setText(mFragment.getString(R.string.unknown_team));
        } else {
            mName.setText(mTeam.getName());
        }
    }

    private void setTeamLogo() {
        Glide.with(mFragment)
                .load(mTeam.getMedia())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.ic_android_black_24dp)
                .into(mLogo);
    }

    @Override
    public void onClick(View v) {
        ScoutActivity.start(mFragment.getContext(), mTeam, v.getId() == R.id.new_scout);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,
                                    View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        menu.setHeaderTitle(mTeam.getFormattedName());
        menu.add(Menu.NONE,
                 R.id.action_share,
                 Menu.NONE,
                 R.string.share)
                .setOnMenuItemClickListener(this);
        menu.add(Menu.NONE,
                 R.id.action_visit_tba_team_website,
                 Menu.NONE,
                 mFragment.getString(R.string.visit_team_website_on_tba, mTeam.getNumber()))
                .setOnMenuItemClickListener(this);
        if (!TextUtils.isEmpty(mTeam.getWebsite())) {
            menu.add(Menu.NONE,
                     R.id.action_visit_team_website,
                     Menu.NONE,
                     mFragment.getString(R.string.visit_team_website, mTeam.getNumber()))
                    .setOnMenuItemClickListener(this);
        }
        menu.add(Menu.NONE,
                 R.id.action_edit_team_details,
                 Menu.NONE,
                 R.string.edit_team_details)
                .setOnMenuItemClickListener(this);
        menu.add(Menu.NONE,
                 R.id.action_delete,
                 Menu.NONE,
                 R.string.delete)
                .setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                TeamSender.launchInvitationIntent(mFragment.getActivity(), mTeam);
                break;
            case R.id.action_visit_tba_team_website:
                mTeam.visitTbaWebsite(mFragment.getContext());
                break;
            case R.id.action_visit_team_website:
                mTeam.visitTeamWebsite(mFragment.getContext());
                break;
            case R.id.action_edit_team_details:
                TeamDetailsDialog.show(mTeam, mFragment.getActivity().getSupportFragmentManager());
                break;
            case R.id.action_delete:
                DeleteTeamDialog.show(mFragment.getActivity().getSupportFragmentManager(), mTeam);
                break;
            default:
                return false;
        }
        return true;
    }
}
