package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.support.annotation.CallSuper
import android.support.annotation.ColorInt
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.ActivityCompat
import android.support.v7.graphics.Palette
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.ShouldUploadMediaToTbaDialog
import com.supercilex.robotscouter.util.data.model.copyMediaInfo
import com.supercilex.robotscouter.util.data.model.forceUpdate
import com.supercilex.robotscouter.util.data.model.isOutdatedMedia
import com.supercilex.robotscouter.util.ui.CaptureTeamMediaListener
import com.supercilex.robotscouter.util.ui.OnActivityResult
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import com.supercilex.robotscouter.util.ui.TeamMediaCreator
import com.supercilex.robotscouter.util.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar
import org.jetbrains.anko.find
import org.jetbrains.anko.support.v4.findOptional
import kotlin.math.roundToInt

open class AppBarViewHolderBase(
        private val fragment: ScoutListFragmentBase,
        rootView: View,
        listener: LiveData<Team>,
        private val onScoutingReadyTask: Task<*>
) : OnSuccessListener<List<Void?>>, View.OnLongClickListener,
        CaptureTeamMediaListener, ActivityCompat.OnRequestPermissionsResultCallback,
        OnActivityResult {
    protected var team: Team = listener.value!!

    val toolbar: Toolbar = rootView.find(R.id.toolbar)
    protected val header: CollapsingToolbarLayout = rootView.find(R.id.header)
    private val backdrop: ImageView = rootView.find(R.id.backdrop)
    private val mediaLoadProgress: ContentLoadingProgressBar = rootView.find(R.id.progress)

    private val permissionHandler = ViewModelProviders.of(fragment)
            .get(PermissionRequestHandler::class.java).apply {
                init(TeamMediaCreator.perms)
            }
    private val mediaCapture = ViewModelProviders.of(fragment)
            .get(TeamMediaCreator::class.java).apply {
                init(permissionHandler)
                onMediaCaptured.observe(fragment, Observer {
                    team.copyMediaInfo(it!!)
                    team.forceUpdate()
                })
            }

    private val onMenuReadyTask = TaskCompletionSource<Nothing?>()
    private lateinit var newScoutItem: MenuItem
    private lateinit var addMediaItem: MenuItem
    private lateinit var visitTeamWebsiteItem: MenuItem

    init {
        backdrop.setOnLongClickListenerCompat(this)

        permissionHandler.onGranted.observe(fragment, Observer { mediaCapture.capture(fragment) })
        listener.observe(fragment, Observer {
            team = it ?: return@Observer
            bind()
        })
    }

    @CallSuper
    protected open fun bind() {
        toolbar.title = team.toString()
        mediaCapture.team = team
        loadImages()
        bindMenu()
    }

    private fun loadImages() {
        mediaLoadProgress.show()

        val media = team.media
        Glide.with(backdrop)
                .asBitmap()
                .load(media)
                .apply(RequestOptions.centerCropTransform().error(R.drawable.ic_person_grey_96dp))
                .listener(object : RequestListener<Bitmap> {
                    override fun onResourceReady(
                            resource: Bitmap?,
                            model: Any?,
                            target: Target<Bitmap>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                    ): Boolean {
                        mediaLoadProgress.hide(true)

                        if (resource?.isRecycled == false) {
                            Palette.from(resource).generate { palette ->
                                palette.vibrantSwatch?.let {
                                    updateScrim(it.rgb, resource)
                                    return@generate
                                }

                                palette.dominantSwatch?.let { updateScrim(it.rgb, resource) }
                            }
                        }
                        return false
                    }

                    override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Bitmap>,
                            isFirstResource: Boolean
                    ): Boolean {
                        mediaLoadProgress.hide(true)
                        return false
                    }
                })
                .into(backdrop)
    }

    @CallSuper
    protected open fun updateScrim(@ColorInt color: Int, bitmap: Bitmap?) =
            header.setContentScrimColor(getTransparentColor(color))

    fun initMenu() {
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.scout_list_menu)
        val menu = toolbar.menu

        newScoutItem = menu.findItem(R.id.action_new_scout)
        addMediaItem = menu.findItem(R.id.action_add_media)
        visitTeamWebsiteItem = menu.findItem(R.id.action_visit_team_website)

        menu.findItem(R.id.action_visit_tba_website).title =
                fragment.getString(R.string.scout_visit_team_tba_website_title, team.number)
        visitTeamWebsiteItem.title =
                fragment.getString(R.string.scout_visit_team_website_title, team.number)
        if (!onScoutingReadyTask.isComplete) newScoutItem.isVisible = false
        toolbar.post {
            fragment.findOptional<View>(R.id.action_new_scout)?.setOnLongClickListenerCompat(this)
        }

        onMenuReadyTask.trySetResult(null)
        bindMenu()
    }

    private fun bindMenu() {
        Tasks.whenAllSuccess<Void?>(onMenuReadyTask.task, onScoutingReadyTask)
                .addOnSuccessListener(this)
    }

    override fun onSuccess(voids: List<Void?>) {
        newScoutItem.isVisible = true
        addMediaItem.isVisible = team.isOutdatedMedia
        visitTeamWebsiteItem.isVisible = team.website?.isNotEmpty() == true
    }

    private fun getTransparentColor(@ColorInt opaque: Int): Int = Color.argb(
            (Color.alpha(opaque) * 0.6f).roundToInt(),
            Color.red(opaque),
            Color.green(opaque),
            Color.blue(opaque)
    )

    override fun onLongClick(v: View): Boolean {
        when {
            v.id == R.id.backdrop -> ShouldUploadMediaToTbaDialog.show(fragment)
            v.id == R.id.action_new_scout ->
                ScoutTemplateSelectorDialog.show(fragment.childFragmentManager)
            else -> return false
        }
        return true
    }

    override fun startCapture(shouldUploadMediaToTba: Boolean) =
        mediaCapture.capture(fragment, shouldUploadMediaToTba)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        permissionHandler.onActivityResult(requestCode, resultCode, data)
        mediaCapture.onActivityResult(requestCode, resultCode, data)
    }

    @CallSuper
    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) = permissionHandler.onRequestPermissionsResult(
            fragment, requestCode, permissions, grantResults)
}
