package com.supercilex.robotscouter.shared

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.TEAM_KEY
import com.supercilex.robotscouter.core.data.mimeType
import com.supercilex.robotscouter.core.data.safeCreateNewFile
import com.supercilex.robotscouter.core.data.shouldAskToUploadMediaToTba
import com.supercilex.robotscouter.core.data.shouldUploadMediaToTba
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.isInTestMode
import com.supercilex.robotscouter.core.longToast
import com.supercilex.robotscouter.core.model.Team
import com.supercilex.robotscouter.core.providerAuthority
import com.supercilex.robotscouter.core.ui.OnActivityResult
import com.supercilex.robotscouter.core.ui.StateHolder
import com.supercilex.robotscouter.core.ui.hasPerms
import com.supercilex.robotscouter.core.ui.hasPermsOnActivityResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import java.io.File

class TeamMediaCreator(private val savedState: SavedStateHandle) : ViewModel(), OnActivityResult {
    private val _state = StateHolder(State())
    val state: LiveData<State> get() = _state.liveData
    private val _viewActions = Channel<ViewAction>(Channel.CONFLATED)
    val viewActions: Flow<ViewAction> = flow { for (e in _viewActions) emit(e) }

    private var photoFile: File? = savedState.get(PHOTO_FILE_KEY)
        set(value) {
            field = value
            savedState.set(PHOTO_FILE_KEY, value)
        }
    private var shouldUpload: Boolean? = savedState.get(PHOTO_SHOULD_UPLOAD)
        set(value) {
            field = value
            savedState.set(PHOTO_SHOULD_UPLOAD, value)
        }

    fun reset() {
        photoFile = null
        shouldUpload = null
        _state.update { State() }
    }

    fun capture() {
        if (!hasPerms(perms)) {
            _viewActions.offer(ViewAction.RequestPermissions(
                    perms.toList(), R.string.media_write_storage_rationale))
            return
        }

        if (isInTestMode) {
            capture(false)
            return
        }

        if (shouldAskToUploadMediaToTba) {
            _viewActions.offer(ViewAction.ShowTbaUploadDialog)
        } else {
            capture(shouldUploadMediaToTba)
        }
    }

    fun capture(shouldUploadMediaToTba: Boolean) {
        shouldUpload = shouldUploadMediaToTba

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(RobotScouter.packageManager) == null) {
            longToast(R.string.error_unknown)
            return
        }

        viewModelScope.launch {
            val file = createImageFile() ?: return@launch
            photoFile = file

            val photoUri = FileProvider.getUriForFile(RobotScouter, providerAuthority, file)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            val choosePictureIntent = Intent(Intent.ACTION_GET_CONTENT)
            choosePictureIntent.type = "image/*"
            choosePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            val chooser = Intent.createChooser(
                    choosePictureIntent, RobotScouter.getString(R.string.media_picker_title))
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePictureIntent))

            if (Build.VERSION.SDK_INT < 21) {
                val infos = RobotScouter.packageManager
                        .queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolveInfo in infos) {
                    val packageName = resolveInfo.activityInfo.packageName
                    RobotScouter.grantUriPermission(
                            packageName,
                            photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }

            _viewActions.offer(ViewAction.StartIntentForResult(chooser, TAKE_PHOTO_RC))

            if (shouldUpload == true) {
                longToast(RobotScouter.getText(R.string.media_upload_reminder).trim())
                        .setGravity(Gravity.CENTER, 0, 0)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (hasPermsOnActivityResult(perms, requestCode)) {
            capture()
            return
        }
        if (requestCode != TAKE_PHOTO_RC) return

        val photoFile = checkNotNull(photoFile)
        if (resultCode == Activity.RESULT_OK) {
            viewModelScope.launch {
                val getContentUri = data?.data
                if (getContentUri != null) {
                    handleRemoteUri(getContentUri, photoFile)
                    return@launch
                }

                handleInternalUri(photoFile)
            }
        } else {
            GlobalScope.launch(Dispatchers.IO) { photoFile.delete() }
        }
    }

    private suspend fun createImageFile(): File? = Dispatchers.IO {
        val teamId = savedState.get<Team>(TEAM_KEY)?.id
        val teamName = teams.find { it.id == teamId }?.toString()
        val fileName = "${teamName}_${System.currentTimeMillis()}.jpg"

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                File(RobotScouter.filesDir, "Pictures/$fileName")
            } else {
                @Suppress("DEPRECATION")
                val mediaDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES)
                File(mediaDir, "Robot Scouter/$fileName")
            }.safeCreateNewFile()
        } catch (e: Exception) {
            CrashLogger.onFailure(e)
            longToast(e.toString())
            null
        }
    }

    private suspend fun handleRemoteUri(uri: Uri, photoFile: File) {
        copyUriToFile(uri, photoFile)
        handleInternalUri(photoFile, false)
    }

    private suspend fun handleInternalUri(photoFile: File, notifyWorld: Boolean = true) {
        if (notifyWorld) {
            insertFileIntoMediaStore(photoFile)
        }

        _state.update { copy(image = State.Image(photoFile.toUri(), checkNotNull(shouldUpload))) }
    }

    private suspend fun insertFileIntoMediaStore(photoFile: File) = Dispatchers.IO {
        val imageDetails = ContentValues().apply {
            put(MediaStore.MediaColumns.MIME_TYPE, photoFile.mimeType())
            put(MediaStore.MediaColumns.TITLE, photoFile.name)
            put(MediaStore.MediaColumns.DISPLAY_NAME, photoFile.nameWithoutExtension)
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Robot Scouter")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = RobotScouter.contentResolver
        val photoUri = checkNotNull(resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails))

        if (Build.VERSION.SDK_INT >= 29) {
            checkNotNull(resolver.openOutputStream(photoUri)).use { output ->
                photoFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            resolver.update(photoUri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        }
    }

    private suspend fun copyUriToFile(uri: Uri, photoFile: File) = Dispatchers.IO {
        val stream = checkNotNull(RobotScouter.contentResolver.openInputStream(uri))
        stream.use { input ->
            photoFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    data class State(
            val image: Image? = null
    ) {
        data class Image(val media: Uri, val shouldUploadMediaToTba: Boolean)
    }

    sealed class ViewAction {
        data class RequestPermissions(
                val perms: List<String>,
                @StringRes val rationaleId: Int
        ) : ViewAction()

        data class StartIntentForResult(val intent: Intent, val rc: Int) : ViewAction()

        object ShowTbaUploadDialog : ViewAction()
    }

    private companion object {
        const val TAKE_PHOTO_RC = 334
        const val PHOTO_FILE_KEY = "photo_file_key"
        const val PHOTO_SHOULD_UPLOAD = "photo_should_upload"

        val perms = if (Build.VERSION.SDK_INT >= 29) {
            arrayOf(Manifest.permission.CAMERA)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}
