package net.dankito.banking.ui.android.views

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.view_floating_action_button_main.view.*
import net.dankito.utils.multiplatform.toFile
import net.dankito.banking.ui.android.R
import net.dankito.banking.ui.model.moneytransfer.ExtractTransferMoneyDataFromPdfResult
import net.dankito.banking.ui.model.moneytransfer.ExtractTransferMoneyDataFromPdfResultType
import net.dankito.banking.ui.presenter.BankingPresenter
import net.dankito.filechooserdialog.FileChooserDialog
import net.dankito.filechooserdialog.model.FileChooserDialogConfig
import net.dankito.utils.android.extensions.asActivity
import net.dankito.utils.android.permissions.IPermissionsService
import java.io.File


open class MainActivityFloatingActionMenuButton(
    floatingActionMenu: FloatingActionMenu,
    protected val permissionsService: IPermissionsService,
    protected val presenter: BankingPresenter
) : FloatingActionMenuButton(floatingActionMenu) {

    protected lateinit var fabTransferMoney: FloatingActionButton

    protected lateinit var fabMoneyTransferFromScanningQrCode: FloatingActionButton

    protected lateinit var fabTransferMoneyFromPdf: FloatingActionButton


    init {
        setupButtons(floatingActionMenu)

        presenter.addBanksChangedListener {
            fabTransferMoney.context.asActivity()?.runOnUiThread {
                checkIfThereAreAccountsThatCanTransferMoney()
            }
        }

        checkIfThereAreAccountsThatCanTransferMoney()
    }

    private fun setupButtons(floatingActionMenu: FloatingActionMenu) {
        (floatingActionMenu.context as? AppCompatActivity)?.let { activity ->
            floatingActionMenu.fabAddAccount.setOnClickListener {
                executeAndCloseMenu { presenter.showAddAccountDialog() }
            }

            fabTransferMoney = floatingActionMenu.fabTransferMoney
            fabMoneyTransferFromScanningQrCode = floatingActionMenu.fabMoneyTransferFromScanningQrCode
            fabTransferMoneyFromPdf = floatingActionMenu.fabTransferMoneyFromPdf

            fabTransferMoney.setOnClickListener {
                executeAndCloseMenu { presenter.showTransferMoneyDialog() }
            }

            fabMoneyTransferFromScanningQrCode.setOnClickListener {
                executeAndCloseMenu { scanQrCode() }
            }

            fabTransferMoneyFromPdf.setOnClickListener {
                executeAndCloseMenu { showTransferMoneyDialogWithDataFromPdf() }
            }
        }
    }


    protected open fun checkIfThereAreAccountsThatCanTransferMoney() {
        fabTransferMoney.isEnabled = presenter.hasAccountsSupportTransferringMoney

        fabMoneyTransferFromScanningQrCode.isEnabled = presenter.hasAccountsSupportTransferringMoney

        fabTransferMoneyFromPdf.isEnabled = presenter.hasAccountsSupportTransferringMoney
    }


    protected open fun scanQrCode() {
        permissionsService.checkPermission(Manifest.permission.CAMERA, R.string.rationale_camera_permission_to_scan_qr_code) { _, isGranted ->
            if (isGranted) {
                floatingActionMenu.context.asActivity()?.let { activity ->
                    val intentIntegrator = IntentIntegrator(activity)
                    intentIntegrator.setOrientationLocked(false)
                    intentIntegrator.initiateScan(listOf(IntentIntegrator.QR_CODE))

                    // parsing decoded QR-code and showing TransferMoneyDialog is done in MainActivity.handleQrCodeScanResult()
                }
            }
        }
    }

    protected open fun showTransferMoneyDialogWithDataFromPdf() {
        (floatingActionMenu.context.asActivity() as? FragmentActivity)?.let { activity ->
            val config = FileChooserDialogConfig(listOf("*.pdf"), presenter.appSettings.lastSelectedOpenPdfFolder.toFile())

            FileChooserDialog().showOpenSingleFileDialog(activity, permissionsService, config) { _, selectedFile ->
                selectedFile?.let {
                    val result = presenter.showTransferMoneyDialogWithDataFromPdf(selectedFile.toFile())

                    if (result.type != ExtractTransferMoneyDataFromPdfResultType.Success) {
                        showTransferMoneyDialogWithDataFromPdfError(activity, selectedFile, result)
                    }
                }
            }
        }
    }

    protected open fun showTransferMoneyDialogWithDataFromPdfError(context: Context, pdfFile: File, result: ExtractTransferMoneyDataFromPdfResult) {
        val errorMessage = when (result.type) {
            ExtractTransferMoneyDataFromPdfResultType.NotASearchablePdf ->
                context.getString(R.string.transfer_money_from_pdf_error_message_not_a_searchable_pdf, pdfFile.absolutePath)
            ExtractTransferMoneyDataFromPdfResultType.CouldNotExtractText ->
                context.getString(R.string.transfer_money_from_pdf_error_message_could_not_extract_text, pdfFile.absolutePath, result.error)
            ExtractTransferMoneyDataFromPdfResultType.CouldNotExtractInvoiceDataFromExtractedText ->
                context.getString(R.string.transfer_money_from_pdf_error_message_could_not_extract_invoice_data, pdfFile.absolutePath, result.error)
            ExtractTransferMoneyDataFromPdfResultType.Success -> "" // will never come to this
        }

        AlertDialog.Builder(context)
            .setMessage(errorMessage)
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

}