package com.supercilex.robotscouter.ui.scouting.scoutlist

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.annotation.ColorInt
import android.support.v4.app.ActivityCompat
import android.support.v7.graphics.Palette
import android.view.MenuItem
import android.view.View
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
import com.supercilex.robotscouter.util.data.isTemplateEditingAllowed
import com.supercilex.robotscouter.util.data.model.copyMediaInfo
import com.supercilex.robotscouter.util.data.model.forceUpdate
import com.supercilex.robotscouter.util.data.model.isOutdatedMedia
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.CaptureTeamMediaListener
import com.supercilex.robotscouter.util.ui.OnActivityResult
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import com.supercilex.robotscouter.util.ui.Saveable
import com.supercilex.robotscouter.util.ui.TeamMediaCreator
import com.supercilex.robotscouter.util.ui.setOnLongClickListenerCompat
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_scout_list.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.findOptional
import kotlin.math.roundToInt
import android.support.v7.graphics.Target as PaletteTarget

open class AppBarViewHolderBase(
        private val fragment: ScoutListFragmentBase,
        savedInstanceState: Bundle?,
        listener: LiveData<Team?>,
        private val onScoutingReadyTask: Task<*>
) : LayoutContainer, OnSuccessListener<List<Void?>>, View.OnLongClickListener,
        CaptureTeamMediaListener, ActivityCompat.OnRequestPermissionsResultCallback,
        OnActivityResult, Saveable, RequestListener<Bitmap> {
    protected lateinit var team: Team

    override val containerView = fragment.view
    private val toolbarHeight =
            fragment.resources.getDimensionPixelSize(R.dimen.scout_toolbar_height)
    private val permissionHandler = ViewModelProviders.of(fragment)
            .get(PermissionRequestHandler::class.java).apply {
                init(TeamMediaCreator.perms)
            }
    private val mediaCapture = ViewModelProviders.of(fragment)
            .get(TeamMediaCreator::class.java).apply {
                init(permissionHandler to savedInstanceState)
                onMediaCaptured.observe(fragment, Observer {
                    team.copyMediaInfo(it!!)
                    team.forceUpdate()
                })
            }

    private val onMenuReadyTask = TaskCompletionSource<Nothing?>()
    private lateinit var newScoutItem: MenuItem
    private lateinit var addMediaItem: MenuItem
    private lateinit var editTemplateItem: MenuItem

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
        progress.show()
        Glide.with(backdrop)
                .asBitmap()
                .load(team.media)
                .apply(RequestOptions.centerCropTransform().error(R.drawable.ic_person_grey_96dp))
                .listener(this)
                .into(backdrop)
    }

    override fun onResourceReady(
            resource: Bitmap?,
            model: Any?,
            target: Target<Bitmap>,
            dataSource: DataSource,
            isFirstResource: Boolean
    ): Boolean {
        progress.hide(true)

        if (resource?.isRecycled == false) {
            async(UI) {
                val palette = async { Palette.from(resource).generate() }.await()

                val update: Palette.Swatch.() -> Unit = {
                    updateScrim(rgb, resource)
                }
                palette.vibrantSwatch?.update() ?: palette.dominantSwatch?.update()
            }.logFailures()

            async(UI) {
                val swatch = async {
                    val paletteTarget = PaletteTarget.Builder()
                            .setExclusive(false)
                            .setTargetLightness(1f)
                            .setMinimumLightness(0.8f)
                            .setLightnessWeight(1f)
                            .setTargetSaturation(0f)
                            .setMaximumSaturation(0f)
                            .build()
                    Palette.from(resource)
                            .addTarget(paletteTarget)
                            .setRegion(0, 0, resource.width, toolbarHeight)
                            .generate()
                            .getSwatchForTarget(paletteTarget)
                }.await() ?: return@async

                // Find backgrounds that are pretty white and then display the scrim to ensure the
                // text is visible.
                if (swatch.hsl.first() == 0f) {
                    header.post { header.scrimVisibleHeightTrigger = Int.MAX_VALUE }
                }
            }.logFailures()
        }

        return false
    }

    override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Bitmap>,
            isFirstResource: Boolean
    ): Boolean {
        progress.hide(true)
        return false
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
        editTemplateItem = menu.findItem(R.id.action_edit_template)

        if (!onScoutingReadyTask.isComplete) newScoutItem.isVisible = false
        toolbar.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            v.findOptional<View>(R.id.action_new_scout)?.setOnLongClickListenerCompat(this)
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
        editTemplateItem.isVisible = isTemplateEditingAllowed
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

    override fun onSaveInstanceState(outState: Bundle) = mediaCapture.onSaveInstanceState(outState)

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
