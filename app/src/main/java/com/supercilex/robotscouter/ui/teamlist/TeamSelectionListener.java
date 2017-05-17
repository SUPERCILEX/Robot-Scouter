package com.supercilex.robotscouter.ui.teamlist;

import android.os.Bundle;

public interface TeamSelectionListener {
    void onTeamSelected(Bundle args, boolean restoreOnConfigChange);
}
