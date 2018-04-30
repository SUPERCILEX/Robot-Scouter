package com.supercilex.robotscouter.shared

import android.animation.AnimatorSet
import android.app.Dialog
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.transition.TransitionManager
import android.support.v4.app.FragmentManager
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.logEditDetails
import com.supercilex.robotscouter.core.data.model.TeamHolder
import com.supercilex.robotscouter.core.data.model.copyMediaInfo
import com.supercilex.robotscouter.core.data.model.forceUpdateAndRefresh
import com.supercilex.robotscouter.core.data.model.formatAsTeamUrl
import com.supercilex.robotscouter.core.data.model.isValidTeamUrl
import com.supercilex.robotscouter.core.data.nullOrFull
import com.supercilex.robotscouter.core.data.toBundle
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.core.ui.OnBackPressedListener
import com.supercilex.robotscouter.core.ui.animateCircularReveal
import com.supercilex.robotscouter.core.ui.setImeOnDoneListener
import com.supercilex.robotscouter.core.ui.show
import com.supercilex.robotscouter.core.unsafeLazy
import kotlinx.android.synthetic.main.dialog_team_details.*
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.coroutines.experimental.asReference
import java.util.Calendar
import kotlin.math.hypot

class TeamDetailsDialog : BottomSheetDialogFragmentBase(), CaptureTeamMediaListener,
        View.OnClickListener, View.OnFocusChangeListener {
    private lateinit var team: Team

    private val permHandler by unsafeLazy {
        ViewModelProviders.of(this).get(PermissionRequestHandler::class.java)
    }
    private val mediaCreator by unsafeLazy {
        ViewModelProviders.of(this).get(TeamMediaCreator::class.java)
    }

    override val containerView by unsafeLazy {
        View.inflate(context, R.layout.dialog_team_details, null) as ViewGroup
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        team = savedInstanceState?.getTeam() ?: arguments!!.getTeam()
        permHandler.apply {
            init(TeamMediaCreator.perms)
            onGranted.observe(this@TeamDetailsDialog, Observer {
                mediaCreator.capture(this@TeamDetailsDialog)
            })
        }
        mediaCreator.apply {
            init(permHandler to savedInstanceState)
            onMediaCaptured.observe(this@TeamDetailsDialog, Observer {
                team.copyMediaInfo(it!!)
                updateUi()
            })
        }
        ViewModelProviders.of(this).get(TeamHolder::class.java).apply {
            init(team.toBundle())
            var firstOverwrite = savedInstanceState
            teamListener.observe(this@TeamDetailsDialog, Observer {
                if (it == null) {
                    dismiss()
                } else {
                    team = it
                    mediaCreator.team = it

                    // Skip the first UI update if this fragment is being restored since the views
                    // will know how to restore themselves.
                    if (firstOverwrite == null) updateUi()
                    firstOverwrite = null
                }
            })
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
        Glide.with(media)
                .load(team.media)
                .apply(RequestOptions.circleCropTransform().error(R.drawable.ic_person_grey_96dp))
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
        mediaEdit.setText(team.media)
        websiteEdit.setText(team.website)
    }

    override fun onClick(v: View) = when (v.id) {
        R.id.media -> ShouldUploadMediaToTbaDialog.show(this)
        R.id.editNameButton -> revealNameEditor()
        R.id.linkTba -> team.launchTba(view.context)
        R.id.linkWebsite -> team.launchWebsite(view.context)
        R.id.save -> save()
        else -> error("Unknown id: ${v.id}")
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
        val name = nameEdit.text
        val media = mediaEdit.text
        val website = websiteEdit.text

        val isMediaValid = validateUrl(media, mediaLayout)
        val isWebsiteValid = validateUrl(website, websiteLayout)

        val ref = asLifecycleReference()
        async(UI) {
            if (!isWebsiteValid.await() || !isMediaValid.await()) return@async

            name.nullOrFull()?.toString().also {
                if (it != team.name) {
                    team.name = it
                    team.hasCustomName = it?.isNotBlank() == true
                }
            }

            async { media.toString().formatAsTeamUrl() }.await().also {
                if (it != team.media) {
                    team.media = it
                    team.hasCustomMedia = it?.isNotBlank() == true
                    team.mediaYear = Calendar.getInstance().get(Calendar.YEAR)
                }
            }

            async { website.toString().formatAsTeamUrl() }.await().also {
                if (it != team.website) {
                    team.website = it
                    team.hasCustomWebsite = it?.isNotBlank() == true
                }
            }

            team.forceUpdateAndRefresh()

            // If we are being called from TeamListFragment, reset the menu if the click was consumed
            (ref().parentFragment as? OnBackPressedListener)?.onBackPressed()

            ref().dismiss()
        }.logFailures()
    }

    override fun onSaveInstanceState(outState: Bundle) = super.onSaveInstanceState(outState.apply {
        putAll(team.toBundle())
        mediaCreator.onSaveInstanceState(this)
    })

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

    private fun validateUrl(url: CharSequence, inputLayout: TextInputLayout): Deferred<Boolean> {
        val inputRef = inputLayout.asReference()
        return async(UI) {
            val isValid = async { url.isValidTeamUrl() }.await()
            inputRef().error =
                    if (isValid) null else getString(R.string.details_malformed_url_error)
            isValid
        }.logFailures()
    }

    companion object {
        private const val TAG = "TeamDetailsDialog"

        fun show(manager: FragmentManager, team: Team) {
            team.logEditDetails()
            TeamDetailsDialog().show(manager, TAG, team.copy().toBundle())
        }
    }
}
