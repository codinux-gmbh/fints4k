package net.dankito.banking.ui.android.dialogs

import android.os.Bundle
import android.view.*
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.dialog_account_transaction_details.view.*
import net.dankito.banking.ui.android.R
import net.dankito.banking.ui.android.dialogs.settings.SettingsDialogBase
import net.dankito.banking.ui.android.views.FormLabelledValue
import net.dankito.banking.ui.model.IAccountTransaction
import net.dankito.utils.multiplatform.BigDecimal


open class AccountTransactionDetailsDialog : SettingsDialogBase() {

    companion object {
        const val DialogTag = "AccountTransactionDetailsDialog"
    }


    protected lateinit var transaction: IAccountTransaction



    fun show(transaction: IAccountTransaction, activity: FragmentActivity) {
        this.transaction = transaction

        show(activity, DialogTag)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.dialog_account_transaction_details, container, false)

        setupUI(rootView)

        return rootView
    }

    protected open fun setupUI(rootView: View) {
        rootView.apply {
            toolbar.apply {
                setupToolbar(this, context.getString(R.string.dialog_account_transaction_details_title), false)
            }

            sender_or_recipient_section_title.setTitle(if (transaction.amount.isPositive) R.string.dialog_account_transaction_details_sender else R.string.dialog_account_transaction_details_recipient)
            lvlOtherPartyName.value = transaction.otherPartyName ?: ""
            lvlOtherPartyAccountId.value = transaction.otherPartyAccountId ?: ""
            lvlOtherPartyBankCode.value = transaction.otherPartyBankCode ?: ""

            lvlAmount.showAmount(presenter, transaction.amount, transaction.currency)
            lvlReference.value = transaction.reference

            lvlBookingText.value = transaction.bookingText ?: ""
            lvlBookingDate.value = presenter.formatToMediumDate(transaction.bookingDate)
            lvlValueDate.value = presenter.formatToMediumDate(transaction.valueDate)

            showAmountIfSet(lvlOpeningBalance, transaction.openingBalance, transaction.account.currency)
            showAmountIfSet(lvlClosingBalance, transaction.closingBalance, transaction.account.currency)
        }
    }

    protected open fun showAmountIfSet(labelledValue: FormLabelledValue, amount: BigDecimal?, currencyCode: String) {
        if (amount != null) {
            labelledValue.showAmount(presenter, amount, currencyCode)
        }
        else {
            labelledValue.visibility = View.GONE
        }
    }


    override val hasUnsavedChanges: Boolean
        get() = false

    override fun saveChanges() {

    }

}