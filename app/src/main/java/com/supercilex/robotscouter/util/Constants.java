package com.supercilex.robotscouter.util;

import com.firebase.ui.database.ObservableSnapshotArray;
import com.google.firebase.database.DataSnapshot;
import com.supercilex.robotscouter.data.model.Scout;
import com.supercilex.robotscouter.data.model.Team;

public enum Constants {;
    public static ObservableSnapshotArray<Team> sFirebaseTeams;

    public static DataSnapshot sDefaultTemplate;
    public static ObservableSnapshotArray<Scout> sFirebaseScoutTemplates;
}
