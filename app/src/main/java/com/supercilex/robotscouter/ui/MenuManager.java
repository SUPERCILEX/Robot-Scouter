package com.supercilex.robotscouter.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public interface MenuManager {
    void onCreateOptionsMenu(Menu menu);

    /**
     * @see android.app.Activity#onOptionsItemSelected(MenuItem)
     */
    boolean onOptionsItemSelected(MenuItem item);

    /**
     * @return true if the back press was consumed, false otherwise.
     */
    boolean onBackPressed();

    void resetMenu();

    void saveState(Bundle outState);

    void restoreState(Bundle savedInstanceState);
}
