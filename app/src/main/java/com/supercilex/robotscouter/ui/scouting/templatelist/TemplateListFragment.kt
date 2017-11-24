package com.supercilex.robotscouter.ui.scouting.templatelist

import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.IdRes
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.common.ChangeEventType
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.TemplateType
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.FIRESTORE_PENDING_APPROVALS
import com.supercilex.robotscouter.util.async
import com.supercilex.robotscouter.util.data.TAB_KEY
import com.supercilex.robotscouter.util.data.TOKEN_EXPIRATION_DAYS
import com.supercilex.robotscouter.util.data.batch
import com.supercilex.robotscouter.util.data.defaultTemplateId
import com.supercilex.robotscouter.util.data.getTabId
import com.supercilex.robotscouter.util.data.getTabIdBundle
import com.supercilex.robotscouter.util.data.model.addTemplate
import com.supercilex.robotscouter.util.isSingleton
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.FragmentBase
import com.supercilex.robotscouter.util.ui.OnBackPressedListener
import com.supercilex.robotscouter.util.ui.RecyclerPoolHolder
import com.supercilex.robotscouter.util.unsafeLazy
import kotterknife.bindView
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.find
import java.util.Calendar
import java.util.Date

class TemplateListFragment : FragmentBase(),
        View.OnClickListener, OnBackPressedListener, RecyclerPoolHolder,
        FirebaseAuth.AuthStateListener {
    override val recyclerPool = RecyclerView.RecycledViewPool()

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val fam: FloatingActionMenu by bindView(R.id.fab_menu)
    private val pagerAdapter by unsafeLazy {
        val tabLayout: TabLayout = find(R.id.tabs)
        val viewPager: ViewPager = find(R.id.viewpager)
        val adapter = object : TemplatePagerAdapter(this@TemplateListFragment, tabLayout) {
            override fun onDataChanged() {
                super.onDataChanged()
                if (currentScouts.isEmpty()) {
                    fam.hideMenuButton(true)
                } else {
                    fam.showMenuButton(true)
                }
            }

            override fun onChildChanged(
                    type: ChangeEventType,
                    snapshot: DocumentSnapshot,
                    newIndex: Int,
                    oldIndex: Int
            ) {
                if (type == ChangeEventType.ADDED || type == ChangeEventType.CHANGED) {
                    @Suppress("UNCHECKED_CAST")
                    val activeTokens = holder.scouts.getSnapshot(newIndex)
                            .get(FIRESTORE_ACTIVE_TOKENS) as Map<String, Date>? ?: emptyMap()

                    if (activeTokens.isEmpty()) return

                    async {
                        val newTokens = activeTokens.filter {
                            it.value.after(Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_MONTH, -TOKEN_EXPIRATION_DAYS)
                            }.time)
                        }

                        if (newTokens != activeTokens) {
                            snapshot.reference.batch {
                                update(it, FIRESTORE_PENDING_APPROVALS, FieldValue.delete())
                                update(it, FIRESTORE_ACTIVE_TOKENS, if (newTokens.isEmpty()) {
                                    FieldValue.delete()
                                } else {
                                    newTokens
                                })
                            }.logFailures()
                        }
                    }.logFailures()
                }
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                super.onTabSelected(tab)
                fam.close(true)
            }
        }

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        adapter
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? = View.inflate(context, R.layout.fragment_template_list, null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun initFab(@IdRes id: Int, @DrawableRes icon: Int) =
                view.find<FloatingActionButton>(id).let {
                    it.setOnClickListener(this)
                    it.setImageDrawable(AppCompatResources.getDrawable(it.context, icon))
                }
        initFab(R.id.add_header, R.drawable.ic_title_white_24dp)
        initFab(R.id.add_checkbox, R.drawable.ic_done_white_24dp)
        initFab(R.id.add_stopwatch, R.drawable.ic_timer_white_24dp)
        initFab(R.id.add_note, R.drawable.ic_note_white_24dp)
        initFab(R.id.add_counter, R.drawable.ic_count_white_24dp)
        initFab(R.id.add_spinner, R.drawable.ic_list_white_24dp)

        fam.hideMenuButton(false)

        pagerAdapter
        handleArgs(arguments!!, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }

    fun handleArgs(args: Bundle, savedInstanceState: Bundle? = null) {
        val templateId = getTabId(args)
        if (templateId != null) {
            pagerAdapter.currentTabId = TemplateType.coerce(templateId)?.let {
                longSnackbar(fam, R.string.template_added_message)
                addTemplate(it).also { defaultTemplateId = it }
            } ?: templateId

            args.remove(TAB_KEY)
        } else {
            savedInstanceState?.let { pagerAdapter.currentTabId = getTabId(it) }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) =
            inflater.inflate(R.menu.template_list_menu, menu)

    override fun onSaveInstanceState(outState: Bundle) = pagerAdapter.onSaveInstanceState(outState)

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_new_template -> NewTemplateDialog.show(childFragmentManager)
            R.id.action_share -> TemplateSharer.shareTemplate(
                    activity!!,
                    pagerAdapter.currentTabId!!,
                    pagerAdapter.currentTab?.text?.toString()!!
            )
            else -> return false
        }
        return true
    }

    override fun onClick(v: View) {
        childFragmentManager.fragments
                .filterIsInstance<TemplateFragment>()
                .filter { pagerAdapter.currentTabId == it.metricsRef.parent.id }
                .also {
                    check(it.isSingleton) {
                        "Multiple fragments found with id ${it[0].metricsRef.parent.id}"
                    }
                    it[0].onClick(v)
                }
    }

    fun onTemplateCreated(id: String) {
        pagerAdapter.currentTabId = id
        longSnackbar(fam, R.string.template_added_title, R.string.template_set_default_title) {
            defaultTemplateId = id
        }
    }

    override fun onBackPressed(): Boolean =
            childFragmentManager.fragments.any { it is OnBackPressedListener && it.onBackPressed() }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null) activity!!.finish()
    }

    companion object {
        const val TAG = "TemplateListFragment"

        fun newInstance(templateId: String?) =
                TemplateListFragment().apply { arguments = getTabIdBundle(templateId) }
    }
}
