package net.codinux.banking.fints4k.android.dialogs

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.text.InputFilter
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import net.codinux.banking.fints4k.android.Presenter
import net.codinux.banking.fints4k.android.R
import net.codinux.banking.fints.model.FlickerCodeTanChallenge
import net.codinux.banking.fints.model.ImageTanChallenge
import net.codinux.banking.fints.model.TanChallenge
import net.dankito.utils.android.extensions.getSpannedFromHtml
import net.dankito.utils.android.extensions.show


open class EnterTanDialog : DialogFragment() {

    companion object {
        const val DialogTag = "EnterTanDialog"
    }


    protected lateinit var tanChallenge: TanChallenge


    open fun show(tanChallenge: TanChallenge, activity: FragmentActivity) {
        this.tanChallenge = tanChallenge

        setStyle(STYLE_NORMAL, R.style.FullscreenDialogWithStatusBar)

        show(activity.supportFragmentManager, DialogTag)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_enter_tan, container, false)

        setupUI(rootView)

        return rootView
    }

    protected open fun setupUI(rootView: View) {
        setupTanView(rootView)

        setupEnteringTan(rootView)

        rootView.findViewById<TextView>(R.id.txtvwMessageToShowToUser).text = tanChallenge.messageToShowToUser.getSpannedFromHtml()

        rootView.findViewById<Button>(R.id.btnCancel).setOnClickListener { enteringTanDone(null) }

        rootView.findViewById<Button>(R.id.btnEnteringTanDone).setOnClickListener {
            enteringTanDone(rootView.findViewById<EditText>(R.id.edtxtEnteredTan).text.toString())
        }
    }

    protected open fun setupTanView(rootView: View) {
        if (tanChallenge is FlickerCodeTanChallenge) {
//            setupFlickerCodeTanView(rootView)
        }
        else if (tanChallenge is ImageTanChallenge) {
            setupImageTanView(rootView)
        }
    }

    protected open fun setupEnteringTan(rootView: View) {
        val edtxtEnteredTan = rootView.findViewById<EditText>(R.id.edtxtEnteredTan)

        if (tanChallenge.tanMethod.isNumericTan) {
            edtxtEnteredTan.inputType = InputType.TYPE_CLASS_NUMBER
        }

        tanChallenge.tanMethod.maxTanInputLength?.let { maxInputLength ->
            edtxtEnteredTan.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(maxInputLength))
        }

        edtxtEnteredTan.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                enteringTanDone(edtxtEnteredTan.text.toString())
                return@setOnKeyListener true
            }
            false
        }
    }

    protected open fun setupImageTanView(rootView: View) {
        val tanImageView = rootView.findViewById<ImageView>(R.id.tanImageView)
        tanImageView.show()

        val decodedImage = (tanChallenge as ImageTanChallenge).image
        if (decodedImage.decodingSuccessful) {
            val bitmap = BitmapFactory.decodeByteArray(decodedImage.imageBytes, 0, decodedImage.imageBytes.size)
            tanImageView.setImageBitmap(bitmap)
        }
        else {
            showDecodingTanChallengeFailedErrorDelayed(decodedImage.decodingError)
        }
    }


    /**
     * This method gets called right on start up before dialog is shown -> Alert would get displayed before dialog and
     * therefore covered by dialog -> delay displaying alert.
     */
    protected open fun showDecodingTanChallengeFailedErrorDelayed(error: Exception?) {
        val handler = Handler()

        handler.postDelayed({ showDecodingTanChallengeFailedError(error) }, 500)
    }

    protected open fun showDecodingTanChallengeFailedError(error: Exception?) {
        activity?.let { context ->
            AlertDialog.Builder(context)
                .setMessage(context.getString(R.string.dialog_enter_tan_error_could_not_decode_tan_image, error?.localizedMessage))
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }


    protected open fun enteringTanDone(enteredTan: String?) {
        if (enteredTan != null) {
            tanChallenge.userEnteredTan(enteredTan)
        } else {
            tanChallenge.userDidNotEnterTan()
        }

        dismiss()
    }

}