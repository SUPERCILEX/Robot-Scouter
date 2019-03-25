package com.supercilex.robotscouter.feature.scouts

import android.graphics.Bitmap
import android.graphics.Color
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.view.postDelayed
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.palette.graphics.Palette
import androidx.palette.graphics.get
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.isTemplateEditingAllowed
import com.supercilex.robotscouter.core.data.model.displayableMedia
import com.supercilex.robotscouter.core.data.model.isOutdatedMedia
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.setOnLongClickListenerCompat
import com.supercilex.robotscouter.shared.ShouldUploadMediaToTbaDialog
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_scout_list_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.find
import org.jetbrains.anko.findOptional
import org.jetbrains.anko.support.v4.findOptional
import kotlin.math.roundToInt
import androidx.palette.graphics.Target as PaletteTarget
import com.supercilex.robotscouter.R as RC

internal open class AppBarViewHolderBase(
        private val fragment: ScoutListFragmentBase,
        listener: LiveData<Team?>
) : LayoutContainer, View.OnLongClickListener, RequestListener<Bitmap> {
    protected lateinit var team: Team

    final override val containerView = fragment.findOptional<View>(R.id.header)
            ?: fragment.requireActivity().find(R.id.header)
    val toolbar: Toolbar = scoutsToolbar
    private val toolbarHeight =
            fragment.resources.getDimensionPixelSize(RC.dimen.scout_toolbar_height)

    private lateinit var newScoutItem: MenuItem
    private lateinit var addMediaItem: MenuItem
    private lateinit var editTemplateItem: MenuItem

    init {
        backdrop.setOnLongClickListenerCompat(this)
        listener.observe(fragment.viewLifecycleOwner) {
            team = it ?: return@observe
            bind()
        }
    }

    @CallSuper
    protected open fun bind() {
        toolbar.title = team.toString()
        loadImages()
        bindMenu()
    }

    private fun loadImages() {
        progress.show()
        Glide.with(fragment)
                .asBitmap()
                .load(team.displayableMedia)
                .centerCrop()
                .error(RC.drawable.ic_person_grey_96dp)
                .apply {
                    if (fragment.sharedElementEnterTransition == null) {
                        transition(BitmapTransitionOptions.withCrossFade())
                    }
                }
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
        toolbar.postDelayed(ARTIFICIAL_POSTPONE_DELAY) { fragment.startPostponedEnterTransition() }

        if (resource?.isRecycled == false) {
            val holderRef = asLifecycleReference(fragment.viewLifecycleOwner)
            GlobalScope.launch {
                val palette = Palette.from(resource).generate()

                val update: suspend Palette.Swatch.() -> Unit = {
                    withContext(Dispatchers.Main) { holderRef().updateScrim(rgb) }
                }
                palette.vibrantSwatch?.update() ?: palette.dominantSwatch?.update()
            }

            GlobalScope.launch {
                val targetPalette = PaletteTarget.Builder()
                        .setTargetLightness(1f)
                        .setMinimumLightness(0.95f)
                        .setLightnessWeight(1f)
                        .build()
                val paletteBase = Palette.from(resource)
                        .clearFilters()
                        .clearTargets()
                        .addTarget(targetPalette)

                val topPalette = paletteBase
                        .setRegion(0, 0, resource.width, toolbarHeight)
                        .generate()
                val bottomPalette = paletteBase
                        .setRegion(
                                0, resource.height - toolbarHeight, resource.width, resource.height)
                        .generate()

                val top = topPalette[targetPalette]
                val bottom = bottomPalette[targetPalette]
                val isMostlyWhite: Palette.Swatch?.() -> Boolean = {
                    this != null && population > 2500
                }

                // Find backgrounds that are pretty white and then display the scrim to ensure the
                // text is visible.
                if (top.isMostlyWhite() || bottom.isMostlyWhite()) {
                    withContext(Dispatchers.Main) {
                        holderRef().header.post { header.scrimVisibleHeightTrigger = Int.MAX_VALUE }
                    }
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
        toolbar.postDelayed(ARTIFICIAL_POSTPONE_DELAY) { fragment.startPostponedEnterTransition() }
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

        newScoutItem.isVisible = ::team.isInitialized
        toolbar.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            v.findOptional<View>(R.id.action_new_scout)?.setOnLongClickListenerCompat(this)
        }

        bindMenu()
    }

    private fun bindMenu() {
        // Just check one of the menu items
        if (!::newScoutItem.isInitialized || !::team.isInitialized) return

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

    private companion object {
        /**
         * Because the scout list fragment is too beefy and takes too long to load, we have to
         * introduce an artificial delay so the animation appears smooth. In the end, this feels
         * faster than the stuttery mess. As performance is improved, this number should be lowered
         * and eventually removed.
         */
        const val ARTIFICIAL_POSTPONE_DELAY = 100L
    }
}
