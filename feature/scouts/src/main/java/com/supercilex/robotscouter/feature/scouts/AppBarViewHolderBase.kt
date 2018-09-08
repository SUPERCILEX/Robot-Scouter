package com.supercilex.robotscouter.feature.scouts

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.palette.graphics.Palette
import androidx.palette.graphics.get
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
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.isTemplateEditingAllowed
import com.supercilex.robotscouter.core.data.model.copyMediaInfo
import com.supercilex.robotscouter.core.data.model.forceUpdate
import com.supercilex.robotscouter.core.data.model.isOutdatedMedia
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.OnActivityResult
import com.supercilex.robotscouter.core.ui.Saveable
import com.supercilex.robotscouter.core.ui.observeNonNull
import com.supercilex.robotscouter.core.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.shared.CaptureTeamMediaListener
import com.supercilex.robotscouter.shared.PermissionRequestHandler
import com.supercilex.robotscouter.shared.ShouldUploadMediaToTbaDialog
import com.supercilex.robotscouter.shared.TeamMediaCreator
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_scout_list_toolbar.*
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.findOptional
import kotlin.math.roundToInt
import androidx.palette.graphics.Target as PaletteTarget
import com.supercilex.robotscouter.R as RC

internal open class AppBarViewHolderBase(
        private val fragment: ScoutListFragmentBase,
        savedInstanceState: Bundle?,
        listener: LiveData<Team?>,
        private val onScoutingReadyTask: Task<*>
) : LayoutContainer, OnSuccessListener<List<Void?>>, View.OnLongClickListener,
        CaptureTeamMediaListener, ActivityCompat.OnRequestPermissionsResultCallback,
        OnActivityResult, Saveable, RequestListener<Bitmap> {
    protected lateinit var team: Team

    final override val containerView = fragment.requireActivity().find<View>(R.id.header)
    val toolbar: Toolbar = scoutsToolbar
    private val toolbarHeight =
            fragment.resources.getDimensionPixelSize(RC.dimen.scout_toolbar_height)

    private val permissionHandler = ViewModelProviders.of(fragment).get<PermissionRequestHandler>()
            .apply { init(TeamMediaCreator.perms) }
    private val mediaCapture = ViewModelProviders.of(fragment).get<TeamMediaCreator>().apply {
        init(permissionHandler to savedInstanceState)
        onMediaCaptured.observeNonNull(fragment) {
            team.copyMediaInfo(it)
            team.forceUpdate()
        }
    }

    private val onMenuReadyTask = TaskCompletionSource<Nothing?>()
    private lateinit var newScoutItem: MenuItem
    private lateinit var addMediaItem: MenuItem
    private lateinit var editTemplateItem: MenuItem

    init {
        backdrop.setOnLongClickListenerCompat(this)

        permissionHandler.onGranted.observe(fragment, Observer { mediaCapture.capture(fragment) })
        listener.observe(fragment.viewLifecycleOwner, Observer {
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
                .apply(RequestOptions.centerCropTransform().error(RC.drawable.ic_person_grey_96dp))
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
            val holderRef = asLifecycleReference(fragment.viewLifecycleOwner)
            launch(UI) {
                val palette = withContext(DefaultDispatcher) { Palette.from(resource).generate() }

                val holder = holderRef()
                val update: Palette.Swatch.() -> Unit = { holder.updateScrim(rgb) }
                palette.vibrantSwatch?.update() ?: palette.dominantSwatch?.update()
            }

            launch(UI) {
                val swatch = withContext(DefaultDispatcher) {
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
                            .generate()[paletteTarget]
                } ?: return@launch

                // Find backgrounds that are pretty white and then display the scrim to ensure the
                // text is visible.
                if (swatch.hsl.first() == 0f) {
                    holderRef().header.post { header.scrimVisibleHeightTrigger = Int.MAX_VALUE }
                }
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
        progress.hide(true)
        return false
    }

    @CallSuper
    protected open fun updateScrim(@ColorInt color: Int) =
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
