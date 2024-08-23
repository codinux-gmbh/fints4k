package net.codinux.banking.fints4k.android.adapter

import android.view.View
import net.codinux.banking.fints4k.android.Presenter
import net.codinux.banking.fints4k.android.R
import net.codinux.banking.fints4k.android.adapter.viewholder.AccountTransactionsViewHolder
import net.dankito.banking.client.model.AccountTransaction
import net.codinux.banking.fints.util.toBigDecimal
import net.dankito.utils.android.extensions.setTextColorToColorResource
import net.dankito.utils.android.ui.adapter.ListRecyclerAdapter
import org.slf4j.LoggerFactory
import java.math.BigDecimal

class AccountTransactionsListRecyclerAdapter : ListRecyclerAdapter<AccountTransaction, AccountTransactionsViewHolder>() {

    private val presenter = Presenter() // TOOD: inject

    private val log = LoggerFactory.getLogger(AccountTransactionsListRecyclerAdapter::class.java)


    override fun getListItemLayoutId() = R.layout.list_item_account_transaction

    override fun createViewHolder(itemView: View): AccountTransactionsViewHolder {
        return AccountTransactionsViewHolder(itemView)
    }

    override fun bindItemToView(viewHolder: AccountTransactionsViewHolder, item: AccountTransaction) {
        try {
            viewHolder.txtvwBookingText.text = item.bookingText ?: ""

            viewHolder.txtvwOtherPartyName.visibility = if (item.showOtherPartyName) View.VISIBLE else View.GONE
            viewHolder.txtvwOtherPartyName.text = item.otherPartyName ?: ""

            viewHolder.txtvwReference.text = item.reference

            viewHolder.txtvwDate.text = presenter.formatDate(item.valueDate)

            val amount = item.amount.toBigDecimal()
            viewHolder.txtvwAmount.text = presenter.formatAmount(amount)
            viewHolder.txtvwAmount.setTextColorToColorResource(if (amount >= BigDecimal.ZERO) R.color.positiveAmount else R.color.negativeAmount)
        } catch (e: Exception) {
            log.error("Could not display account transaction $item", e)
        }
    }

}