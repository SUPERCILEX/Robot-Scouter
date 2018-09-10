package com.supercilex.robotscouter.feature.scouts

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.supercilex.robotscouter.Bridge
import com.supercilex.robotscouter.IntegratedScoutListFragmentCompanion
import com.supercilex.robotscouter.ScoutListFragmentCompanionBase.Companion.TAG
import com.supercilex.robotscouter.TeamSelectionListener
import com.supercilex.robotscouter.core.data.mainHandler
import com.supercilex.robotscouter.core.ui.isInTabletMode
import com.supercilex.robotscouter.core.unsafeLazy
import org.jetbrains.anko.find
import com.supercilex.robotscouter.R as RC

@Bridge
internal class IntegratedScoutListFragment : ScoutListFragmentBase() {
    private val appBar by unsafeLazy { requireActivity().find<ViewGroup>(RC.id.appBar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireContext().isInTabletMode()) {
            val listener = context as TeamSelectionListener
            val bundle = bundle
            mainHandler.post { listener.onTeamSelected(bundle) }

            removeFragment()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val toolbar = LayoutInflater.from(ContextThemeWrapper(
                requireContext(),
                RC.style.ThemeOverlay_AppCompat_Dark_ActionBar
        )).inflate(R.layout.fragment_scout_list_toolbar, appBar, false)
        appBar.apply {
            children.forEach { it.isVisible = false }
            addView(toolbar, 0)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun newViewModel(savedInstanceState: Bundle?) = AppBarViewHolderBase(
            this, savedInstanceState, dataHolder.teamListener, onScoutingReadyTask.task)

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
            val bundle = bundle
            removeFragment()
            startActivity(ScoutListActivity.createIntent(bundle))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        appBar.apply {
            removeViewAt(0)
            children.forEach { it.isVisible = true }
        }
    }

    override fun onStop() {
        // This has to be done in onStop so fragment transactions from the view pager can be
        // committed. Only reset the adapter if the user is switching destinations.
        if (isDetached) pagerAdapter?.reset()
        super.onStop()
    }

    override fun onTeamDeleted() = removeFragment()

    private fun removeFragment() {
        requireFragmentManager().popBackStack()
    }

    companion object : IntegratedScoutListFragmentCompanion {
        override fun newInstance(args: Bundle) =
                IntegratedScoutListFragment().apply { arguments = args }

        override fun getInstance(manager: FragmentManager) =
                manager.findFragmentByTag(TAG) as? IntegratedScoutListFragment
    }
}
