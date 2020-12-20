package net.dankito.banking.ui.android.dialogs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.dialog_send_message_log.view.*
import net.dankito.banking.ui.android.R
import net.dankito.banking.ui.android.di.BankingComponent
import net.dankito.banking.ui.presenter.BankingPresenter
import net.dankito.utils.android.extensions.hide
import net.dankito.utils.android.extensions.show
import net.dankito.utils.multiplatform.getInnerExceptionMessage
import net.dankito.utils.multiplatform.os.DeviceInfo
import net.dankito.banking.ui.model.issues.CreateTicketRequestDto
import net.dankito.banking.ui.model.issues.IssueDescriptionFormat
import net.dankito.utils.android.extensions.asActivity
import net.dankito.utils.serialization.JacksonJsonSerializer
import net.dankito.utils.web.client.OkHttpWebClient
import net.dankito.utils.web.client.RequestParameters
import net.dankito.utils.web.client.WebClientResponse
import javax.inject.Inject


open class SendMessageLogDialog : DialogFragment() {

    companion object {
        const val DialogTag = "SendMessageLogDialog"
    }


    @Inject
    protected lateinit var presenter: BankingPresenter


    init {
        BankingComponent.component.inject(this)
    }


    fun show(activity: AppCompatActivity, fullscreen: Boolean = true) {
        val style = if(fullscreen) R.style.FullscreenDialogWithStatusBar else R.style.FloatingDialog
        setStyle(STYLE_NORMAL, style)

        show(activity.supportFragmentManager, DialogTag)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_send_message_log, container, false)

        rootView?.let {
            setupUI(rootView)
        }

        return rootView
    }

    protected open fun setupUI(rootView: View) {
        val messageLog = presenter.getFormattedMessageLogForAccounts(presenter.allBanksSortedByDisplayIndex)

        if (messageLog.isBlank()) {
            rootView.txtvwInfoNoMessageLogEntriesYet.show()
            rootView.lytMessageLog.hide()

            rootView.btnSendMessageLogDirectly.isEnabled = false
            rootView.btnSendMessageLogViaEMail.isEnabled = false
        }
        else {
            rootView.edtxtMessageLog.setText(context?.getString(R.string.dialog_send_message_courteously_add_error_description, messageLog))
        }

        rootView.btnSendMessageLogDirectly.setOnClickListener { sendMessageLogDirectly(rootView.edtxtMessageLog.text.toString()) }
        rootView.btnSendMessageLogViaEMail.setOnClickListener { sendMessageLogViaEMail(rootView.edtxtMessageLog.text.toString()) }

        rootView.btnCancel.setOnClickListener { dismiss() }
    }

    protected open fun sendMessageLogViaEMail(messageLog: String) {
        // TODO: check if message log exceeds 120.000 characters

        val sendMailActivity = Intent(Intent.ACTION_SEND)
        sendMailActivity.type = "message/rfc822"
        sendMailActivity.putExtra(Intent.EXTRA_EMAIL, arrayOf("panta.rhei@dankito.net"))
        sendMailActivity.putExtra(Intent.EXTRA_SUBJECT, context?.getString(R.string.dialog_send_message_log_mail_subject))
        sendMailActivity.putExtra(Intent.EXTRA_TEXT, messageLog)

        try {
            startActivity(Intent.createChooser(sendMailActivity, context?.getString(R.string.dialog_send_message_log_action_send_chooser_title)))

            showSuccessfullySentMessageLog()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, context?.getString(R.string.dialog_send_message_log_error_no_app_to_send_message_found), Toast.LENGTH_LONG).show()
        }
    }

    protected open fun sendMessageLogDirectly(messageLog: String) {
        val deviceInfo = DeviceInfo(Build.MANUFACTURER.capitalize(), Build.MODEL, "Android", Build.VERSION.RELEASE, System.getProperty("os.arch") ?: "")

        // TODO: sending with Ktor did not work
        //presenter.sendMessageLogDirectly(messageLog, deviceInfo)

        val requestBodyDto = CreateTicketRequestDto(messageLog, "Bankmeister", IssueDescriptionFormat.PlainText,
            deviceInfo.osName, deviceInfo.osVersion, deviceInfo.manufacturer, deviceInfo.deviceModel)
        val requestBody = JacksonJsonSerializer().serializeObject(requestBodyDto)

        OkHttpWebClient().postAsync(RequestParameters("https://codinux.uber.space/issues", requestBody, "application/json")) { response ->
            context?.asActivity()?.runOnUiThread {
                handleSendMessageLogDirectlyResponseOnUiThread(response)
            }
        }
    }

    protected open fun handleSendMessageLogDirectlyResponseOnUiThread(response: WebClientResponse) {
        if (response.isSuccessResponse) {
            showSuccessfullySentMessageLog()
        }
        else {
            Toast.makeText(context, context?.getString(R.string.dialog_send_message_log_error_could_not_sent_message_log, response.error?.getInnerExceptionMessage()),
                Toast.LENGTH_LONG).show()
        }
    }

    protected open fun showSuccessfullySentMessageLog() {
        Toast.makeText(context, context?.getString(R.string.dialog_send_message_log_thanks_for_helping_making_app_better), Toast.LENGTH_LONG).show()

        dismiss()
    }

}