package com.supercilex.robotscouter.feature.templates

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.CachingSharer
import com.supercilex.robotscouter.core.data.getTemplateLink
import com.supercilex.robotscouter.core.data.logShareTemplate
import com.supercilex.robotscouter.core.data.model.shareTemplates
import com.supercilex.robotscouter.core.data.templatesRef
import com.supercilex.robotscouter.core.isOffline
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.ui.longSnackbar
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import com.supercilex.robotscouter.R as RC

internal class TemplateSharer private constructor(
        fragment: Fragment,
        templateId: String,
        templateName: String
) : CachingSharer() {
    private val res = fragment.resources

    init {
        val fragmentRef = fragment.asLifecycleReference(fragment.viewLifecycleOwner)
        launch(UI) {
            val intent = try {
                withContext(CommonPool) { generateIntent(templateId, templateName) }
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                longSnackbar(checkNotNull(fragmentRef().view), RC.string.error_unknown)
                return@launch
            }
            fragmentRef().startActivityForResult(intent, RC_SHARE)
        }
    }

    private suspend fun generateIntent(templateId: String, templateName: String): Intent {
        // Called first to skip token generation if task failed
        val htmlTemplate = loadFile(FILE_NAME)
        val token = listOf(templatesRef.document(templateId)).shareTemplates()

        return getInvitationIntent(
                getTemplateLink(templateId, token),
                templateName,
                htmlTemplate.format(res.getString(R.string.template_share_cta, templateName))
        )
    }

    private fun getInvitationIntent(deepLink: String, templateName: String, html: String) =
            AppInviteInvitation.IntentBuilder(
                    res.getString(R.string.template_share_title, templateName))
                    .setMessage(res.getString(R.string.template_share_message, templateName))
                    .setDeepLink(deepLink.toUri())
                    .setEmailSubject(res.getString(R.string.template_share_cta, templateName))
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
                longSnackbar(checkNotNull(fragment.view), RC.string.no_connection)
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
