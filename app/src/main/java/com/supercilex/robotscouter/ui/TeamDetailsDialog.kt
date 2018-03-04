package com.supercilex.robotscouter.ui

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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.ui.teamlist.TeamListFragment
import com.supercilex.robotscouter.util.asLifecycleReference
import com.supercilex.robotscouter.util.data.getTeam
import com.supercilex.robotscouter.util.data.model.TeamHolder
import com.supercilex.robotscouter.util.data.model.copyMediaInfo
import com.supercilex.robotscouter.util.data.model.forceUpdateAndRefresh
import com.supercilex.robotscouter.util.data.model.formatAsTeamUrl
import com.supercilex.robotscouter.util.data.model.isValidTeamUrl
import com.supercilex.robotscouter.util.data.model.launchTba
import com.supercilex.robotscouter.util.data.model.launchWebsite
import com.supercilex.robotscouter.util.data.nullOrFull
import com.supercilex.robotscouter.util.data.toBundle
import com.supercilex.robotscouter.util.logEditDetails
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.ui.BottomSheetDialogFragmentBase
import com.supercilex.robotscouter.util.ui.CaptureTeamMediaListener
import com.supercilex.robotscouter.util.ui.PermissionRequestHandler
import com.supercilex.robotscouter.util.ui.TeamMediaCreator
import com.supercilex.robotscouter.util.ui.animateCircularReveal
import com.supercilex.robotscouter.util.ui.setImeOnDoneListener
import com.supercilex.robotscouter.util.ui.show
import com.supercilex.robotscouter.util.ui.views.ContentLoadingProgressBar
import com.supercilex.robotscouter.util.unsafeLazy
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotterknife.bindView
import org.jetbrains.anko.coroutines.experimental.asReference
import java.util.Calendar
import kotlin.math.hypot

class TeamDetailsDialog : BottomSheetDialogFragmentBase(), CaptureTeamMediaListener,
        View.OnClickListener, View.OnFocusChangeListener {
    private lateinit var team: Team

    private val permHandler: PermissionRequestHandler by unsafeLazy {
        ViewModelProviders.of(this).get(PermissionRequestHandler::class.java).apply {
            init(TeamMediaCreator.perms)
            onGranted.observe(this@TeamDetailsDialog, Observer {
                mediaCreator.capture(this@TeamDetailsDialog)
            })
        }
    }
    private val mediaCreator by unsafeLazy {
        ViewModelProviders.of(this).get(TeamMediaCreator::class.java)
    }

    private val content by unsafeLazy {
        View.inflate(context, R.layout.dialog_team_details, null) as ViewGroup
    }

    private val media: ImageView by bindView(R.id.media)
    private val mediaLoadProgress: ContentLoadingProgressBar by bindView(R.id.progress)
    private val name: TextView by bindView(R.id.name)
    private val editNameButton: ImageButton by bindView(R.id.edit_name_button)

    private val launchTbaButton: Button by bindView(R.id.link_tba)
    private val launchWebsiteButton: Button by bindView(R.id.link_website)

    private val nameInputLayout: TextInputLayout by bindView(R.id.name_layout)
    private val mediaInputLayout: TextInputLayout by bindView(R.id.media_layout)
    private val websiteInputLayout: TextInputLayout by bindView(R.id.website_layout)
    private val nameEditText: EditText by bindView(R.id.name_edit)
    private val mediaEditText: EditText by bindView(R.id.media_edit)
    private val websiteEditText: EditText by bindView(R.id.website_edit)

    private val save: View by bindView(R.id.save)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        team = savedInstanceState?.getTeam() ?: arguments!!.getTeam()
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
        dialog.setContentView(content)

        media.setOnClickListener(this)
        editNameButton.setOnClickListener(this)
        launchTbaButton.setOnClickListener(this)
        launchWebsiteButton.setOnClickListener(this)
        save.setOnClickListener(this)

        mediaEditText.onFocusChangeListener = this
        websiteEditText.onFocusChangeListener = this
        websiteEditText.setImeOnDoneListener { save() }

        updateUi()
    }

    private fun updateUi() {
        launchWebsiteButton.isEnabled = !team.website.isNullOrBlank()

        TransitionManager.beginDelayedTransition(content)

        mediaLoadProgress.show()
        Glide.with(content.context)
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
                        mediaLoadProgress.hide(true)
                        return false
                    }

                    override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                    ): Boolean {
                        mediaLoadProgress.hide(true)
                        return false
                    }
                })
                .into(media)
        name.text = team.toString()

        nameEditText.setText(team.name)
        mediaEditText.setText(team.media)
        websiteEditText.setText(team.website)
    }

    override fun onClick(v: View) = when (v.id) {
        R.id.media -> ShouldUploadMediaToTbaDialog.show(this)
        R.id.edit_name_button -> revealNameEditor()
        R.id.link_tba -> team.launchTba(content.context)
        R.id.link_website -> team.launchWebsite(content.context)
        R.id.save -> save()
        else -> error("Unknown id: ${v.id}")
    }

    private fun revealNameEditor() {
        val editNameAnimator = editNameButton.animateCircularReveal(
                false, 0, editNameButton.height / 2, editNameButton.width.toFloat())
        val nameAnimator = name.animateCircularReveal(
                false, 0, name.height / 2, name.width.toFloat())
        val nameLayoutAnimator = (editNameButton.left + editNameButton.width / 2).let {
            nameInputLayout.animateCircularReveal(
                    true, it, 0, hypot(it.toFloat(), nameInputLayout.height.toFloat()))
        }

        if (editNameAnimator != null && nameAnimator != null && nameLayoutAnimator != null) {
            AnimatorSet().apply {
                playTogether(editNameAnimator, nameAnimator, nameLayoutAnimator)
                start()
            }
        }
    }

    private fun save() {
        val name = nameEditText.text
        val media = mediaEditText.text
        val website = websiteEditText.text

        val isMediaValid = validateUrl(media, mediaInputLayout)
        val isWebsiteValid = validateUrl(website, websiteInputLayout)

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
            (ref().parentFragment as? TeamListFragment)?.resetMenu()

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

        validateUrl(mediaEditText.text, mediaInputLayout)
        validateUrl(websiteEditText.text, websiteInputLayout)
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
