package com.supercilex.robotscouter.ui.scouting.templatelist

import android.content.Intent
import android.support.v4.app.Fragment
import androidx.core.net.toUri
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.SetOptions
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.RobotScouter
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.asLifecycleReference
import com.supercilex.robotscouter.util.data.CachingSharer
import com.supercilex.robotscouter.util.data.QueuedDeletion
import com.supercilex.robotscouter.util.data.firestoreBatch
import com.supercilex.robotscouter.util.data.generateToken
import com.supercilex.robotscouter.util.data.getTemplateLink
import com.supercilex.robotscouter.util.data.model.userDeletionQueue
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logShareTemplate
import com.supercilex.robotscouter.util.templatesRef
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.support.v4.find
import java.util.Date

class TemplateSharer private constructor(
        fragment: Fragment,
        templateId: String,
        templateName: String
) : CachingSharer() {
    init {
        val fragmentRef = fragment.asLifecycleReference()
        async(UI) {
            val intent = try {
                async { generateIntent(templateId, templateName) }.await()
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                longSnackbar(fragmentRef().find(R.id.root), R.string.fui_error_unknown)
                return@async
            }
            fragmentRef().startActivityForResult(intent, RC_SHARE)
        }.logFailures()
    }

    private suspend fun generateIntent(templateId: String, templateName: String): Intent {
        // Called first to skip token generation if task failed
        val htmlTemplate = loadFile(FILE_NAME)

        val token = generateToken
        val templateRef = templatesRef.document(templateId)
        firestoreBatch {
            update(templateRef, FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token), Date())
            set(userDeletionQueue,
                QueuedDeletion.ShareToken.Template(token, templateId).data,
                SetOptions.merge())
        }.logFailures(templateRef)

        return getInvitationIntent(
                getTemplateLink(templateId, token),
                templateName,
                htmlTemplate.format(
                        RobotScouter.getString(R.string.template_share_cta, templateName))
        )
    }

    private fun getInvitationIntent(deepLink: String, templateName: String, html: String) =
            AppInviteInvitation.IntentBuilder(
                    RobotScouter.getString(R.string.template_share_title, templateName))
                    .setMessage(
                            RobotScouter.getString(R.string.template_share_message, templateName))
                    .setDeepLink(deepLink.toUri())
                    .setEmailSubject(
                            RobotScouter.getString(R.string.template_share_cta, templateName))
                    .setEmailHtmlContent(html)
                    .build()

    companion object {
        private const val RC_SHARE = 975
        private const val FILE_NAME = "share_template_template.html"

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTemplate(
                fragment: Fragment,
                templateId: String,
                templateName: String
        ): Boolean {
            if (isOffline) {
                longSnackbar(fragment.find(R.id.root), R.string.no_connection)
                return false
            }

            logShareTemplate(templateId, templateName)
            FirebaseUserActions.getInstance().end(
                    Action.Builder(Action.Builder.SHARE_ACTION)
                            .setObject(templateName, getTemplateLink(templateId))
                            .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                            .build()
            ).logFailures()

            TemplateSharer(fragment, templateId, templateName)

            return true
        }
    }
}
