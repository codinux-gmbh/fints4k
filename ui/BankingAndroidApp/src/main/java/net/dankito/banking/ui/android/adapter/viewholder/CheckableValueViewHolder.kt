package net.dankito.banking.ui.android.adapter.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.mikepenz.fastadapter.FastAdapter
import net.dankito.banking.ui.android.R
import net.dankito.banking.ui.android.adapter.CheckableValueAdapterItem
import net.dankito.utils.android.extensions.setVisibleOrInvisible


open class CheckableValueViewHolder(view: View) : FastAdapter.ViewHolder<CheckableValueAdapterItem>(view) {

    protected var imgCheckmark: ImageView = view.findViewById(R.id.imgCheckmark)

    protected var txtValue: TextView = view.findViewById(R.id.txtValue)


    override fun bindView(item: CheckableValueAdapterItem, payloads: List<Any>) {
        imgCheckmark.setVisibleOrInvisible(item.isChecked)

        txtValue.text = item.text
    }

    override fun unbindView(item: CheckableValueAdapterItem) {
        imgCheckmark.setImageURI(null)

        txtValue.text = null
    }

}