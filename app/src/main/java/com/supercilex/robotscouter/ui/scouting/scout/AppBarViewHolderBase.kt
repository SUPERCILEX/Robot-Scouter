package com.supercilex.robotscouter.ui.scouting.scout

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.ColorInt
import android.support.design.widget.CollapsingToolbarLayout
import android.support.v4.app.ActivityCompat
import android.support.v7.graphics.Palette
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.supercilex.robotscouter.ui.TeamMediaCreator
import com.supercilex.robotscouter.util.data.model.copyMediaInfo
import com.supercilex.robotscouter.util.data.model.forceUpdateTeam
import com.supercilex.robotscouter.util.data.model.isOutdatedMedia

abstract class AppBarViewHolderBase protected constructor(private val fragment: LifecycleFragment,
                                                          rootView: View,
                                                          listener: LiveData<Team>,
                                                          private val onScoutingReadyTask: Task<*>) :
        OnSuccessListener<Void?>, View.OnLongClickListener,
        TeamMediaCreator.StartCaptureListener, ActivityCompat.OnRequestPermissionsResultCallback {
    protected var team: Team = listener.value!!

    protected val toolbar: Toolbar = rootView.findViewById(R.id.toolbar)
    protected val header: CollapsingToolbarLayout = rootView.findViewById(R.id.header)
    private val backdrop: ImageView = rootView.findViewById(R.id.backdrop)
    private val mediaLoadProgress: ProgressBar = rootView.findViewById(R.id.progress)

    private val mediaCaptureListener = OnSuccessListener<Team> {
        team.copyMediaInfo(it)
        team.forceUpdateTeam()
    }
    private var mediaCapture: TeamMediaCreator = TeamMediaCreator.newInstance(fragment, team, mediaCaptureListener)

    private val onMenuReadyTask = TaskCompletionSource<Nothing?>()
    private lateinit var newScoutItem: MenuItem
    private lateinit var addMediaItem: MenuItem
    private lateinit var visitTeamWebsiteItem: MenuItem
    private lateinit var deleteScoutItem: MenuItem
    private var isDeleteScoutItemVisible: Boolean = false

    init {
        backdrop.setOnLongClickListener(this)

        listener.observe(fragment, Observer {
            team = it ?: return@Observer
            bind()
        })
    }

    @CallSuper
    protected open fun bind() {
        toolbar.title = team.toString()
        mediaCapture.setTeam(team)
        loadImages()
        bindMenu()
    }

    private fun loadImages() {
        mediaLoadProgress.visibility = View.VISIBLE

        val media = team.media
        Glide.with(backdrop)
                .asBitmap()
                .load(media)
                .apply(RequestOptions.centerCropTransform().error(R.drawable.ic_memory_grey_48dp))
                .listener(object : RequestListener<Bitmap> {
                    override fun onResourceReady(resource: Bitmap?,
                                                 model: Any?,
                                                 target: Target<Bitmap>,
                                                 dataSource: DataSource,
                                                 isFirstResource: Boolean): Boolean {
                        mediaLoadProgress.visibility = View.GONE

                        if (resource != null && !resource.isRecycled) {
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

                    override fun onLoadFailed(e: GlideException?,
                                              model: Any?,
                                              target: Target<Bitmap>,
                                              isFirstResource: Boolean): Boolean {
                        mediaLoadProgress.visibility = View.GONE
                        return false
                    }
                })
                .into(backdrop)
    }

    @CallSuper
    protected open fun updateScrim(@ColorInt color: Int, bitmap: Bitmap?) =
            header.setContentScrimColor(getTransparentColor(color))

    fun initMenu(menu: Menu) {
        newScoutItem = menu.findItem(R.id.action_new_scout)
        addMediaItem = menu.findItem(R.id.action_add_media)
        visitTeamWebsiteItem = menu.findItem(R.id.action_visit_team_website)
        deleteScoutItem = menu.findItem(R.id.action_delete)

        menu.findItem(R.id.action_visit_tba_website).title =
                fragment.getString(R.string.visit_team_website_on_tba, team.number)
        visitTeamWebsiteItem.title = fragment.getString(R.string.visit_team_website, team.number)
        if (!onScoutingReadyTask.isComplete) newScoutItem.isVisible = false

        onMenuReadyTask.trySetResult(null)
        bindMenu()
    }

    fun setDeleteScoutMenuItemVisible(visible: Boolean) {
        isDeleteScoutItemVisible = visible
        bindMenu()
    }

    private fun bindMenu() =
            Tasks.whenAll(onMenuReadyTask.task, onScoutingReadyTask).addOnSuccessListener(this)

    override fun onSuccess(void: Void?) {
        newScoutItem.isVisible = true
        addMediaItem.isVisible = team.isOutdatedMedia
        visitTeamWebsiteItem.isVisible = !TextUtils.isEmpty(team.website)
        deleteScoutItem.isVisible = isDeleteScoutItemVisible
    }

    private fun getTransparentColor(@ColorInt opaque: Int): Int =
            Color.argb(Math.round(Color.alpha(opaque) * 0.6f),
                    Color.red(opaque),
                    Color.green(opaque),
                    Color.blue(opaque))

    override fun onLongClick(v: View): Boolean = if (v.id == R.id.backdrop) {
        ShouldUploadMediaToTbaDialog.show(fragment)
        true
    } else false

    override fun onStartCapture(shouldUploadMediaToTba: Boolean) =
            mediaCapture.startCapture(shouldUploadMediaToTba)

    @CallSuper
    fun onActivityResult(requestCode: Int, resultCode: Int) =
            mediaCapture.onActivityResult(requestCode, resultCode)

    @CallSuper
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) =
            mediaCapture.onRequestPermissionsResult(requestCode, permissions, grantResults)

    @CallSuper
    fun onSaveInstanceState(outState: Bundle) = outState.putAll(mediaCapture.toBundle())

    @CallSuper
    fun restoreState(savedInstanceState: Bundle) {
        mediaCapture = TeamMediaCreator.get(savedInstanceState, fragment, mediaCaptureListener)
    }
}
