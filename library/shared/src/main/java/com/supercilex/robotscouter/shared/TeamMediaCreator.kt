package com.supercilex.robotscouter.shared

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.view.Gravity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.viewModelScope
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.SimpleViewModelBase
import com.supercilex.robotscouter.core.data.SingleLiveEvent
import com.supercilex.robotscouter.core.data.hidden
import com.supercilex.robotscouter.core.data.ioPerms
import com.supercilex.robotscouter.core.data.logTakeMedia
import com.supercilex.robotscouter.core.data.mediaFolder
import com.supercilex.robotscouter.core.data.safeCreateNewFile
import com.supercilex.robotscouter.core.data.unhide
import com.supercilex.robotscouter.core.longToast
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.providerAuthority
import com.supercilex.robotscouter.core.ui.OnActivityResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.util.Calendar
import java.util.Collections

interface CaptureTeamMediaListener {
    fun startCapture(shouldUploadMediaToTba: Boolean)
}

class TeamMediaCreator(state: SavedStateHandle) : SimpleViewModelBase(state), OnActivityResult {
    private val _onMediaCaptured = SingleLiveEvent<Team>()
    val onMediaCaptured: LiveData<Team> get() = _onMediaCaptured
    lateinit var team: Team

    private var photoFile: File? = state.get(PHOTO_FILE_KEY)
        set(value) {
            field = value
            state.set(PHOTO_FILE_KEY, value)
        }
    private var shouldUpload: Boolean? = state.get(PHOTO_SHOULD_UPLOAD)
        set(value) {
            field = value
            state.set(PHOTO_SHOULD_UPLOAD, value)
        }

    fun capture(host: Fragment, shouldUploadMediaToTba: Boolean? = null) {
        shouldUploadMediaToTba?.let { shouldUpload = it }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(RobotScouter.packageManager) == null) {
            longToast(R.string.error_unknown)
            return
        }
        val handler = ViewModelProvider(host).get<PermissionRequestHandler>()
        if (!EasyPermissions.hasPermissions(RobotScouter, *handler.perms.toTypedArray())) {
            handler.requestPerms(host, R.string.media_write_storage_rationale)
            return
        }

        team.logTakeMedia()

        val ref = host.asLifecycleReference()
        viewModelScope.launch {
            val file = try {
                Dispatchers.IO {
                    File(mediaFolder, "${team}_${System.currentTimeMillis()}.jpg")
                            .hidden()
                            .safeCreateNewFile()
                }
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                longToast(e.toString())
                null
            } ?: return@launch

            photoFile = file
            val photoUri = FileProvider.getUriForFile(RobotScouter, providerAuthority, file)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            ref().startActivityForResult(takePictureIntent, TAKE_PHOTO_RC)

            if (shouldUpload == true) {
                longToast(RobotScouter.getText(R.string.media_upload_reminder).trim())
                        .setGravity(Gravity.CENTER, 0, 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != TAKE_PHOTO_RC) return

        val photoFile = checkNotNull(photoFile)
        if (resultCode == Activity.RESULT_OK) {
            viewModelScope.launch {
                val contentUri = Dispatchers.IO { photoFile.unhide()?.toUri() }
                if (contentUri == null) {
                    longToast(R.string.error_unknown)
                    return@launch
                }

                _onMediaCaptured.value = team.copy().apply {
                    media = contentUri.path
                    hasCustomMedia = true
                    shouldUploadMediaToTba = checkNotNull(shouldUpload)
                    mediaYear = Calendar.getInstance().get(Calendar.YEAR)
                }

                // Tell gallery that we have a new photo
                RobotScouter.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                    this.data = contentUri
                })
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) { photoFile.delete() }
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
