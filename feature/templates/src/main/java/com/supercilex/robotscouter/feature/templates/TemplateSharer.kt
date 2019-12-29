package com.supercilex.robotscouter.feature.templates

import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.firebase.appindexing.Action
import com.google.firebase.appindexing.FirebaseUserActions
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.supercilex.robotscouter.core.CrashLogger
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.asLifecycleReference
import com.supercilex.robotscouter.core.data.CachingSharer
import com.supercilex.robotscouter.core.data.getTemplateLink
import com.supercilex.robotscouter.core.data.logFailures
import com.supercilex.robotscouter.core.data.logShareTemplate
import com.supercilex.robotscouter.core.data.model.shareTemplates
import com.supercilex.robotscouter.core.data.templatesRef
import com.supercilex.robotscouter.core.isOffline
import com.supercilex.robotscouter.core.logBreadcrumb
import com.supercilex.robotscouter.core.ui.longSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.supercilex.robotscouter.R as RC

internal class TemplateSharer private constructor(
        fragment: Fragment,
        templateId: String,
        templateName: String
) : CachingSharer() {
    init {
        val fragmentRef = fragment.asLifecycleReference(fragment.viewLifecycleOwner)
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val intent = Dispatchers.Default { generateIntent(templateId, templateName) }
                fragmentRef().startActivityForResult(intent, RC_SHARE)
            } catch (e: Exception) {
                CrashLogger.onFailure(e)
                fragmentRef().requireView().longSnackbar(RC.string.error_unknown)
                return@launch
            }
        }
    }

    private suspend fun generateIntent(templateId: String, templateName: String): Intent {
        val token = listOf(templatesRef.document(templateId)).shareTemplates()

        return getInvitationIntent(getTemplateLink(templateId, token), templateName)
    }

    private suspend fun getInvitationIntent(deepLink: String, templateName: String): Intent {
        val cta = RobotScouter.getString(R.string.template_share_cta, templateName)
        val message = RobotScouter.getString(R.string.template_share_message, templateName)
        val link = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(deepLink.toUri())
                .setDomainUriPrefix("https://robotscouter.page.link")
                .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle(cta)
                                .setDescription(message)
                                .build()
                )
                .buildShortDynamicLink()
                .await()
        // TODO https://github.com/firebase/firebase-android-sdk/pull/1084
        @Suppress("UselessCallOnNotNull")
        if (link.warnings.orEmpty().isNotEmpty()) {
            val warnings = link.warnings.joinToString { it.message.toString() }
            logBreadcrumb("Dynamic link warnings: $warnings")
        }

        val shareIntent = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, cta)
                .putExtra(Intent.EXTRA_TEXT, message + "\n\n" + link.shortLink)
        return Intent.createChooser(shareIntent, RobotScouter.getString(
                R.string.template_share_title, templateName))
    }

    companion object {
        private const val RC_SHARE = 975

        /**
         * @return true if a share intent was launched, false otherwise
         */
        fun shareTemplate(
                fragment: Fragment,
                templateId: String,
                templateName: String
        ): Boolean {
            if (isOffline) {
                fragment.requireView().longSnackbar(RC.string.no_connection)
                return false
            }

            logShareTemplate(templateId, templateName)
            FirebaseUserActions.getInstance().end(
                    Action.Builder(Action.Builder.SHARE_ACTION)
                            .setObject(templateName, getTemplateLink(templateId))
                            .setActionStatus(Action.Builder.STATUS_TYPE_COMPLETED)
                            .build()
            ).logFailures("shareTemplate:addAction")

            TemplateSharer(fragment, templateId, templateName)

            return true
        }
    }
}
