package com.supercilex.robotscouter.ui.scouting.templatelist

import android.content.Intent
import android.net.Uri
import android.support.v4.app.FragmentActivity
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.tasks.Continuation
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.firestore.FieldPath
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.data.CachingSharer
import com.supercilex.robotscouter.util.data.generateToken
import com.supercilex.robotscouter.util.data.getTemplateLink
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logFailures
import com.supercilex.robotscouter.util.logShareTemplate
import com.supercilex.robotscouter.util.templates
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.find
import java.util.Date

class TemplateSharer private constructor(
        private val activity: FragmentActivity
) : CachingSharer(activity) {
    fun share(templateId: String, templateName: String) {
        loadFile(FILE_NAME).continueWith(AsyncTaskExecutor, Continuation<String, Intent> {
            it.result // Skip token generation if task failed

            val token = generateToken
            templates.document(templateId)
                    .update(FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token), Date())
                    .logFailures()

            getInvitationIntent(
                    getTemplateLink(templateId, token),
                    templateName,
                    it.result.format(activity.getString(R.string.template_share_cta, templateName)))
        }).addOnSuccessListener(activity) {
            activity.startActivityForResult(it, RC_SHARE)
        }.logFailures().addOnFailureListener(activity) {
            longSnackbar(activity.find(R.id.root), R.string.fui_general_error)
        }
    }

    private fun getInvitationIntent(deepLink: String, templateName: String, html: String) =
            AppInviteInvitation.IntentBuilder(
                    activity.getString(R.string.template_share_title, templateName))
                    .setMessage(activity.getString(R.string.template_share_message, templateName))
                    .setDeepLink(Uri.parse(deepLink))
                    .setEmailSubject(activity.getString(R.string.template_share_cta, templateName))
                    .setEmailHtmlContent(html)
                    .build()

    companion object {
        private const val RC_SHARE = 975
        private const val FILE_NAME = "share_template_template.html"

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTemplate(
                activity: FragmentActivity,
                templateId: String,
                templateName: String
        ): Boolean {
            if (isOffline()) {
                longSnackbar(activity.find(R.id.root), R.string.no_connection)
                return false
            }

            logShareTemplate(templateId, templateName)
            FirebaseUserActions.getInstance().end(
                    Action.Builder(Action.Builder.SHARE_ACTION)
                            .setObject(templateName, getTemplateLink(templateId))
                            .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                            .build()
            ).logFailures()

            TemplateSharer(activity).share(templateId, templateName)

            return true
        }
    }
}
