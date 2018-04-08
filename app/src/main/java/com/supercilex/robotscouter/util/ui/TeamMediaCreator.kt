package com.supercilex.robotscouter.util.ui

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.LiveData
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.view.Gravity
import androidx.core.net.toUri
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.asLifecycleReference
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.hidden
import com.supercilex.robotscouter.util.data.ioPerms
import com.supercilex.robotscouter.util.data.mediaFolder
import com.supercilex.robotscouter.util.data.unhide
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logTakeMedia
import com.supercilex.robotscouter.util.providerAuthority
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.longToast
import org.jetbrains.anko.runOnUiThread
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.IOException
import java.util.Calendar
import java.util.Collections

interface CaptureTeamMediaListener {
    fun startCapture(shouldUploadMediaToTba: Boolean)
}

class TeamMediaCreator : ViewModelBase<Pair<PermissionRequestHandler, Bundle?>>(),
        OnActivityResult, Saveable {
    private val _onMediaCaptured = SingleLiveEvent<Team>()
    val onMediaCaptured: LiveData<Team> get() = _onMediaCaptured
    lateinit var team: Team

    private lateinit var handler: PermissionRequestHandler
    private var photoFile: File? = null
    private var shouldUploadMediaToTba: Boolean? = null

    override fun onCreate(args: Pair<PermissionRequestHandler, Bundle?>) {
        handler = args.first
        args.second?.let {
            photoFile = it.getSerializable(PHOTO_FILE_KEY) as File?
            shouldUploadMediaToTba = it.getBoolean(PHOTO_SHOULD_UPLOAD)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.apply {
            putSerializable(PHOTO_FILE_KEY, photoFile)
            shouldUploadMediaToTba?.let { putBoolean(PHOTO_SHOULD_UPLOAD, it) }
        }
    }

    fun capture(host: Fragment, shouldUploadMediaToTba: Boolean? = null) {
        shouldUploadMediaToTba?.let { this.shouldUploadMediaToTba = it }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(RobotScouter.packageManager) == null) {
            RobotScouter.longToast(R.string.fui_error_unknown)
            return
        }
        if (!EasyPermissions.hasPermissions(RobotScouter, *handler.perms.toTypedArray())) {
            handler.requestPerms(host, R.string.media_write_storage_rationale)
            return
        }

        team.logTakeMedia()

        val ref = host.asLifecycleReference()
        async(UI) {
            val file = async {
                try {
                    File(
                            mediaFolder ?: throw IOException("Couldn't create folder"),
                            "${team}_${System.currentTimeMillis()}.jpg"
                    ).hidden().apply {
                        if (!createNewFile()) throw IOException("Unable to create temporary file")
                    }
                } catch (e: IOException) {
                    CrashLogger.onFailure(e)
                    RobotScouter.runOnUiThread { longToast(e.toString()) }
                    null
                }
            }.await() ?: return@async

            photoFile = file
            val photoUri = FileProvider.getUriForFile(RobotScouter, providerAuthority, file)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            ref().startActivityForResult(takePictureIntent, TAKE_PHOTO_RC)

            if (this@TeamMediaCreator.shouldUploadMediaToTba == true) {
                RobotScouter.longToast(R.string.media_upload_reminder)
                        .setGravity(Gravity.CENTER, 0, 0)
            }
        }.logFailures()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != TAKE_PHOTO_RC) return

        val photoFile = photoFile!!
        if (resultCode == Activity.RESULT_OK) {
            async(UI) {
                val contentUri = async { photoFile.unhide()?.toUri() }.await()
                if (contentUri == null) {
                    RobotScouter.longToast(R.string.fui_error_unknown)
                    return@async
                }

                _onMediaCaptured.value = team.copy().apply {
                    media = contentUri.path
                    hasCustomMedia = true
                    shouldUploadMediaToTba = this@TeamMediaCreator.shouldUploadMediaToTba!!
                    mediaYear = Calendar.getInstance().get(Calendar.YEAR)
                }

                // Tell gallery that we have a new photo
                RobotScouter.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    this.data = contentUri
                })
            }.logFailures()
        } else {
            async { photoFile.delete() }.logFailures()
        }
    }

    companion object {
        private const val TAKE_PHOTO_RC = 334
        private const val PHOTO_FILE_KEY = "photo_file_key"
        private const val PHOTO_SHOULD_UPLOAD = "photo_should_upload"

        val perms: List<String> = Collections.unmodifiableList(ioPerms.toMutableList().apply {
            this += Manifest.permission.CAMERA
        })
    }
}
