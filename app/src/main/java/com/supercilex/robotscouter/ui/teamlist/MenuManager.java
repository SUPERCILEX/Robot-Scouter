package com.supercilex.robotscouter.ui.teamlist;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.supercilex.robotscouter.ui.common.OnBackPressedListener;

public interface MenuManager extends OnBackPressedListener {
    void onCreateOptionsMenu(Menu menu);

    /**
     * @see android.app.Activity#onOptionsItemSelected(MenuItem)
     */
    boolean onOptionsItemSelected(MenuItem item);

    void resetMenu();

    void saveState(Bundle outState);

    void restoreState(Bundle savedInstanceState);
}
