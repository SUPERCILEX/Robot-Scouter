package com.supercilex.robotscouter.data.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class TeamCache {
    private static final Object TEAM_NAMES_LOCK = new Object();

    private final List<TeamHelper> mTeamHelpers;
    private String mTeamNames;

    public TeamCache(Collection<TeamHelper> teamHelpers) {
        List<TeamHelper> sortedTeamHelpers = new ArrayList<>(teamHelpers);
        Collections.sort(sortedTeamHelpers);
        mTeamHelpers = Collections.unmodifiableList(sortedTeamHelpers);
    }

    public List<TeamHelper> getTeamHelpers() {
        return mTeamHelpers;
    }

    public String getTeamNames() {
        synchronized (TEAM_NAMES_LOCK) {
            if (mTeamNames == null) {
                mTeamNames = TeamHelper.getTeamNames(getTeamHelpers());
            }
            return mTeamNames;
        }
    }
}
