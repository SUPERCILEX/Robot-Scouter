package com.supercilex.robotscouter.shared

import android.animation.AnimatorSet
import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.SavedStateVMFactory
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.lifecycle.observe
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.textfield.TextInputLayout
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.logEditDetails
import com.supercilex.robotscouter.core.data.model.TeamHolder
import com.supercilex.robotscouter.core.data.model.copyMediaInfo
import com.supercilex.robotscouter.core.data.model.displayableMedia
import com.supercilex.robotscouter.core.data.model.forceUpdate
import com.supercilex.robotscouter.core.data.model.formatAsTeamUri
import com.supercilex.robotscouter.core.data.model.isValidTeamUri
import com.supercilex.robotscouter.core.data.model.processPotentialMediaUpload
import com.supercilex.robotscouter.core.data.nullOrFull
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.core.ui.animateCircularReveal
import com.supercilex.robotscouter.core.ui.setImeOnDoneListener
import com.supercilex.robotscouter.core.ui.show
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_team_details.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.hypot

class TeamDetailsDialog : BottomSheetDialogFragmentBase(), CaptureTeamMediaListener,
        View.OnClickListener, View.OnFocusChangeListener {
    private lateinit var team: Team

    private val permHandler by stateViewModels<PermissionRequestHandler>()
    private val mediaCreator by stateViewModels<TeamMediaCreator>()

    override val containerView by unsafeLazy {
        View.inflate(context, R.layout.dialog_team_details, null) as ViewGroup
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        team = requireArguments().getTeam()
        permHandler.apply {
            init(TeamMediaCreator.perms)
            onGranted.observe(this@TeamDetailsDialog) {
                mediaCreator.capture(this@TeamDetailsDialog)
            }
        }
        mediaCreator.apply {
            init()
            onMediaCaptured.observe(this@TeamDetailsDialog) {
                team.copyMediaInfo(it)
                updateUi()
            }
        }
        ViewModelProviders.of(this, SavedStateVMFactory(this)).get<TeamHolder>().apply {
            init(team)
            var firstOverwrite = savedInstanceState == null
            teamListener.observe(this@TeamDetailsDialog) {
                if (it == null) {
                    dismiss()
                } else {
                    team = it
                    mediaCreator.team = it

                    // Skip the first UI update if this fragment is being restored since the views
                    // will know how to restore themselves.
                    if (firstOverwrite) updateUi()
                    firstOverwrite = true
                }
            }
        }
    }

    override fun onDialogCreated(dialog: Dialog, savedInstanceState: Bundle?) {
        media.setOnClickListener(this)
        editNameButton.setOnClickListener(this)
        linkTba.setOnClickListener(this)
        linkWebsite.setOnClickListener(this)
        save.setOnClickListener(this)

        mediaEdit.onFocusChangeListener = this
        websiteEdit.onFocusChangeListener = this
        websiteEdit.setImeOnDoneListener { save() }

        updateUi()
    }

    private fun updateUi() {
        linkWebsite.isEnabled = !team.website.isNullOrBlank()

        TransitionManager.beginDelayedTransition(containerView)

        progress.show()
        Glide.with(this)
                .load(team.displayableMedia)
                .circleCrop()
                .error(R.drawable.ic_person_grey_96dp)
                .transition(DrawableTransitionOptions.withCrossFade())
                .listener(object : RequestListener<Drawable> {
                    override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                    ): Boolean {
                        progress.hide(true)
                        return false
                    }

                    override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                    ): Boolean {
                        progress.hide(true)
                        return false
                    }
                })
                .into(media)
        name.text = team.toString()

        nameEdit.setText(team.name)
        mediaEdit.setText(team.displayableMedia)
        websiteEdit.setText(team.website)
    }

    override fun onClick(v: View) = when (val id = v.id) {
        R.id.media -> ShouldUploadMediaToTbaDialog.show(this)
        R.id.editNameButton -> revealNameEditor()
        R.id.linkTba -> team.launchTba(view.context)
        R.id.linkWebsite -> team.launchWebsite(view.context)
        R.id.save -> save()
        else -> error("Unknown id: $id")
    }

    private fun revealNameEditor() {
        val editNameAnimator = editNameButton.animateCircularReveal(
                false, 0, editNameButton.height / 2, editNameButton.width.toFloat())
        val nameAnimator = name.animateCircularReveal(
                false, 0, name.height / 2, name.width.toFloat())
        val nameLayoutAnimator = (editNameButton.left + editNameButton.width / 2).let {
            nameLayout.animateCircularReveal(
                    true, it, 0, hypot(it.toFloat(), nameLayout.height.toFloat()))
        }

        if (editNameAnimator != null && nameAnimator != null && nameLayoutAnimator != null) {
            AnimatorSet().apply {
                playTogether(editNameAnimator, nameAnimator, nameLayoutAnimator)
                start()
            }
        }
    }

    private fun save() {
        val name = nameEdit.text?.toString()
        val media = mediaEdit.text?.toString()
        val website = websiteEdit.text?.toString()

        val isMediaValid = validateUrl(media, mediaLayout)
        val isWebsiteValid = validateUrl(website, websiteLayout)

        val ref = asLifecycleReference()
        GlobalScope.launch {
            if (!isWebsiteValid.await() || !isMediaValid.await()) return@launch

            name.nullOrFull().also {
                if (it != team.name) {
                    team.name = it
                    team.hasCustomName = !it.isNullOrBlank()
                }
            }

            media?.formatAsTeamUri().also {
                if (it != team.media) {
                    team.media = it
                    team.hasCustomMedia = !it.isNullOrBlank()
                    team.mediaYear = Calendar.getInstance().get(Calendar.YEAR)
                }
            }

            website?.formatAsTeamUri().also {
                if (it != team.website) {
                    team.website = it
                    team.hasCustomWebsite = !it.isNullOrBlank()
                }
            }

            team.processPotentialMediaUpload()
            team.forceUpdate(true)

            withContext(Dispatchers.Main) {
                // If we are being called from TeamListFragment, reset the menu if the click was consumed
                (ref().parentFragment as? OnBackPressedCallback)?.handleOnBackPressed()

                ref().dismiss()
            }
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        permHandler.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        permHandler.onActivityResult(requestCode, resultCode, data)
        mediaCreator.onActivityResult(requestCode, resultCode, data)
    }

    override fun startCapture(shouldUploadMediaToTba: Boolean) {
        mediaCreator.capture(this, shouldUploadMediaToTba)
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) return // Only consider views losing focus

        validateUrl(mediaEdit.text, mediaLayout)
        validateUrl(websiteEdit.text, websiteLayout)
    }

    private fun validateUrl(url: CharSequence?, inputLayout: TextInputLayout): Deferred<Boolean> {
        if (url == null) return CompletableDeferred(true)

        val inputRef = inputLayout.asLifecycleReference(this)
        return GlobalScope.async {
            val isValid = url.isValidTeamUri()
            withContext(Dispatchers.Main) {
                inputRef().error =
                        if (isValid) null else getString(R.string.details_malformed_url_error)
            }
            isValid
        }
    }

    companion object {
        private const val TAG = "TeamDetailsDialog"

        fun show(manager: FragmentManager, team: Team) {
            team.logEditDetails()
            TeamDetailsDialog().show(manager, TAG, team.copy().toBundle())
        }
    }
}
