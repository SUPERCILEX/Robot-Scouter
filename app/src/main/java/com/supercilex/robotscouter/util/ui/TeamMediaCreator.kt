package com.supercilex.robotscouter.util.ui

import android.Manifest
import android.app.Activity
import android.arch.lifecycle.LiveData
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.data.model.Team
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.data.ViewModelBase
import com.supercilex.robotscouter.util.data.createFile
import com.supercilex.robotscouter.util.data.hide
import com.supercilex.robotscouter.util.data.ioPerms
import com.supercilex.robotscouter.util.data.mediaFolder
import com.supercilex.robotscouter.util.data.unhide
import com.supercilex.robotscouter.util.logTakeMedia
import com.supercilex.robotscouter.util.providerAuthority
import org.jetbrains.anko.longToast
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.util.Collections
import kotlin.properties.Delegates

interface CaptureTeamMediaListener {
    fun startCapture(shouldUploadMediaToTba: Boolean)
}

class TeamMediaCreator : ViewModelBase<PermissionRequestHandler>(), OnActivityResult {
    private val _onMediaCaptured = SingleLiveEvent<Team>()
    val onMediaCaptured: LiveData<Team> get() = _onMediaCaptured
    lateinit var team: Team

    private lateinit var handler: PermissionRequestHandler
    private lateinit var photoFile: File
    private var shouldUploadMediaToTba: Boolean by Delegates.notNull()

    override fun onCreate(args: PermissionRequestHandler) {
        handler = args
    }

    fun capture(host: Fragment, shouldUploadMediaToTba: Boolean? = null) {
        shouldUploadMediaToTba?.let { this.shouldUploadMediaToTba = it }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(RobotScouter.packageManager) == null) {
            RobotScouter.longToast(R.string.fui_general_error)
            return
        }
        if (!EasyPermissions.hasPermissions(RobotScouter, *handler.perms.toTypedArray())) {
            handler.requestPerms(host, R.string.media_write_storage_rationale)
            return
        }

        photoFile = try {
            createFile(
                    team.toString(),
                    "jpg",
                    mediaFolder!!,
                    System.currentTimeMillis().toString()
            ).hide()
        } catch (e: Exception) {
            CrashLogger.onFailure(e)
            RobotScouter.longToast(e.toString())
            return
        }

        team.logTakeMedia()
        val photoUri = FileProvider.getUriForFile(
                RobotScouter, providerAuthority, photoFile)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        host.startActivityForResult(takePictureIntent, TAKE_PHOTO_RC)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != TAKE_PHOTO_RC) return

        if (resultCode == Activity.RESULT_OK) {
            val contentUri = Uri.fromFile(photoFile.unhide())

            _onMediaCaptured.value = team.copy().apply {
                hasCustomMedia = true
                shouldUploadMediaToTba = this@TeamMediaCreator.shouldUploadMediaToTba
                media = contentUri.path
            }

            // Tell gallery that we have a new photo
            RobotScouter.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                this.data = contentUri
            })
        } else {
            photoFile.delete()
        }
    }

    companion object {
        private const val TAKE_PHOTO_RC = 334
        val perms: List<String> = Collections.unmodifiableList(ioPerms.toMutableList().apply {
            this += Manifest.permission.CAMERA
        })
    }
}
