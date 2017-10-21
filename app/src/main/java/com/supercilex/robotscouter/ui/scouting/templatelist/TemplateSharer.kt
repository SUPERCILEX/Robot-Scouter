package com.supercilex.robotscouter.ui.scouting.templatelist

import android.content.Intent
import android.net.Uri
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.tasks.Continuation
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.firestore.FieldPath
import com.supercilex.robotscouter.R
import com.supercilex.robotscouter.util.AsyncTaskExecutor
import com.supercilex.robotscouter.util.CrashLogger
import com.supercilex.robotscouter.util.FIRESTORE_ACTIVE_TOKENS
import com.supercilex.robotscouter.util.FIRESTORE_TEMPLATES
import com.supercilex.robotscouter.util.data.CachingSharer
import com.supercilex.robotscouter.util.data.generateToken
import com.supercilex.robotscouter.util.data.getTemplateLink
import com.supercilex.robotscouter.util.isOffline
import com.supercilex.robotscouter.util.logShareTemplateEvent
import java.util.Date

class TemplateSharer private constructor(private val activity: FragmentActivity) :
        CachingSharer(activity) {
    fun share(templateId: String, templateName: String) {
        loadFile(FILE_NAME).continueWith(AsyncTaskExecutor, Continuation<String, Intent> {
            val token = generateToken
            FIRESTORE_TEMPLATES.document(templateId)
                    .update(FieldPath.of(FIRESTORE_ACTIVE_TOKENS, token), Date())

            getInvitationIntent(
                    getTemplateLink(templateId, token),
                    templateName,
                    it.result.format(activity.getString(R.string.cta_share_template, templateName)))
        }).addOnSuccessListener {
            activity.startActivityForResult(it, RC_SHARE)
        }.addOnFailureListener(CrashLogger).addOnFailureListener {
            FirebaseCrash.report(it)
            Snackbar.make(activity.findViewById(R.id.root),
                          R.string.fui_general_error,
                          Snackbar.LENGTH_LONG)
                    .show()
        }
    }

    private fun getInvitationIntent(deepLink: String, templateName: String, html: String) =
            AppInviteInvitation.IntentBuilder(
                    activity.getString(R.string.title_share_template, templateName))
                    .setMessage(activity.getString(R.string.message_share_template, templateName))
                    .setDeepLink(Uri.parse(deepLink))
                    .setEmailSubject(activity.getString(R.string.cta_share_template, templateName))
                    .setEmailHtmlContent(html)
                    .build()

    companion object {
        private const val RC_SHARE = 975
        private const val FILE_NAME = "share_template_template.html"

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTemplate(activity: FragmentActivity,
                          templateId: String,
                          templateName: String): Boolean {
            if (isOffline()) {
                Snackbar.make(activity.findViewById(R.id.root),
                              R.string.no_connection,
                              Snackbar.LENGTH_LONG)
                        .show()
                return false
            }

            TemplateSharer(activity).share(templateId, templateName)
            logShareTemplateEvent(templateId)
            return true
        }
    }
}
