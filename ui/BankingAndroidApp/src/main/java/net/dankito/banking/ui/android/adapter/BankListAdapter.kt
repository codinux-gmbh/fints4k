package net.dankito.banking.ui.android.adapter

import android.view.View
import net.dankito.banking.ui.android.R
import net.dankito.banking.ui.android.adapter.viewholder.BankInfoViewHolder
import net.dankito.banking.bankfinder.BankInfo
import net.dankito.utils.android.extensions.setTintColor
import net.dankito.utils.android.ui.adapter.ListRecyclerAdapter


open class BankListAdapter(protected val itemClicked: ((BankInfo) -> Unit)? = null) : ListRecyclerAdapter<BankInfo, BankInfoViewHolder>() {

    override fun getListItemLayoutId() = R.layout.list_item_bank_info

    override fun createViewHolder(itemView: View): BankInfoViewHolder {
        return BankInfoViewHolder(itemView)
    }

    override fun bindItemToView(viewHolder: BankInfoViewHolder, item: BankInfo) {
        if (item.supportsFinTs3_0) {
            viewHolder.imgSupportsFints30.setImageResource(R.drawable.ic_check_circle_white_48dp)
            viewHolder.imgSupportsFints30.setTintColor(R.color.list_item_bank_info_bank_supported)
        }
        else {
            viewHolder.imgSupportsFints30.setImageResource(R.drawable.ic_clear_white_48dp)
            viewHolder.imgSupportsFints30.setTintColor(R.color.list_item_bank_info_bank_not_supported)
        }

        viewHolder.txtvwBankName.text = item.name
        viewHolder.txtvwBankName.isEnabled = item.supportsFinTs3_0

        viewHolder.txtvwBankCode.text = item.bankCode
        viewHolder.txtvwBankCode.isEnabled = item.supportsFinTs3_0

        viewHolder.txtvwBankAddress.text = item.postalCode + " " + item.city
        viewHolder.txtvwBankAddress.isEnabled = item.supportsFinTs3_0

        viewHolder.itemView.setOnClickListener {
            itemClicked?.invoke(item)
        }
    }

}