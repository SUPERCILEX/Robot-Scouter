package com.supercilex.robotscouter.feature.scouts

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.postDelayed
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.google.android.material.appbar.AppBarLayout
import com.supercilex.robotscouter.ActivityViewCreationListener
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.IntegratedScoutListFragmentCompanion
import com.supercilex.robotscouter.ScoutListFragmentCompanionBase.Companion.TAG
import com.supercilex.robotscouter.TeamSelectionListener
import com.supercilex.robotscouter.core.ui.animateRawColorChange
import com.supercilex.robotscouter.core.ui.colorPrimaryDark
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.ui.transitionAnimationDuration
import com.supercilex.robotscouter.core.unsafeLazy
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

@Bridge
internal class IntegratedScoutListFragment : ScoutListFragmentBase(), ActivityViewCreationListener {
    private val appBar by unsafeLazy { requireActivity().find<AppBarLayout>(RC.id.appBar) }
    private val drawer by unsafeLazy { requireActivity().find<DrawerLayout>(RC.id.drawerLayout) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (sharedElementEnterTransition != null) postponeEnterTransition()
    }

    override fun onActivityViewCreated(listener: TeamSelectionListener) {
        if (!requireContext().isInTabletMode()) return

        listener.onTeamSelected(bundle)
        removeFragment(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.activity_scout_list, container, false)
        root.find<FrameLayout>(R.id.scoutList).addView(
                inflater.inflate(R.layout.fragment_scout_list, container, false))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (sharedElementEnterTransition == null) {
            appBar.post { appBar.setExpanded(false) }
        }
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()
        appBar.postDelayed(transitionAnimationDuration) {
            appBar.setExpanded(false)
        }
    }

    override fun newViewModel(savedInstanceState: Bundle?) = object : AppBarViewHolderBase(
            this@IntegratedScoutListFragment,
            savedInstanceState,
            dataHolder.teamListener,
            onScoutingReadyTask.task
    ) {
        override fun bind() {
            super.bind()
            updateStatusBarColor(colorPrimaryDark)
        }

        override fun updateScrim(color: Int) {
            super.updateScrim(color)
            updateStatusBarColor(color)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity as AppCompatActivity
        viewHolder.toolbar.apply {
            navigationIcon = checkNotNull(activity.drawerToggleDelegate).themeUpIndicator
            setNavigationOnClickListener { requireFragmentManager().popBackStack() }
            setOnMenuItemClickListener {
                forceRecursiveMenuItemSelection(it) || onOptionsItemSelected(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.integrated_scout_list_menu, viewHolder.toolbar.menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_move_window) {
            startActivity(ScoutListActivity.createIntent(bundle))
            removeFragment(true)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedElementEnterTransition = null
        appBar.setExpanded(true)
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED)
        updateStatusBarColor(colorPrimaryDark)
    }

    override fun onStop() {
        // This has to be done in onStop so fragment transactions from the view pager can be
        // committed. Only reset the adapter if the user is switching destinations.
        if (isDetached) pagerAdapter?.reset()
        super.onStop()
    }

    override fun onTeamDeleted() = removeFragment()

    private fun removeFragment(now: Boolean = false) {
        // This one is super tricky. If the user tries to open a team, but it is invalid for
        // whatever reason, the fragment will be immediately removed. However, there's a bug in the
        // Fragments API where removing a fragment doesn't get rid of the pending animations.
        // Consequentially, the next state restoration will cause these pending transactions to get
        // executed and thus break a ton of stuff. Here, we ensure that the pending animations are
        // executed.
        super.startPostponedEnterTransition()

        val manager = requireFragmentManager()
        if (now) manager.popBackStackImmediate() else manager.popBackStack()
    }

    private fun updateStatusBarColor(@ColorInt color: Int) {
        val current = (drawer.statusBarBackgroundDrawable as? ColorDrawable)?.color
                ?: colorPrimaryDark
        if (color == current) return

        animateRawColorChange(current, color) {
            drawer.setStatusBarBackgroundColor(it.animatedValue as Int)
        }
    }

    companion object : IntegratedScoutListFragmentCompanion {
        override fun newInstance(args: Bundle) =
                IntegratedScoutListFragment().apply { arguments = args }

        override fun getInstance(manager: FragmentManager) =
                manager.findFragmentByTag(TAG) as? IntegratedScoutListFragment
    }
}
