package net.codinux.banking.fints4k.android.adapter.viewholder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.codinux.banking.fints4k.android.R

class AccountTransactionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val txtvwBookingText: TextView = itemView.findViewById(R.id.txtvwBookingText)

    val txtvwOtherPartyName: TextView = itemView.findViewById(R.id.txtvwOtherPartyName)

    val txtvwReference: TextView = itemView.findViewById(R.id.txtvwReference)

    val txtvwAmount: TextView = itemView.findViewById(R.id.txtvwAmount)

    val txtvwDate: TextView = itemView.findViewById(R.id.txtvwDate)

}