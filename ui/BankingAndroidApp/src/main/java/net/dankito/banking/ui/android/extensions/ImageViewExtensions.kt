package net.dankito.banking.ui.android.extensions

import android.widget.ImageView
import net.dankito.banking.ui.model.IBankData
import net.dankito.utils.android.extensions.hide
import net.dankito.utils.android.extensions.show


fun ImageView.setIcon(bank: IBankData<*, *>) {
    try {
        val iconData = bank.iconData

        if (iconData != null) {
            this.show()
            this.setImageFromBytes(iconData)
        }
        else {
            this.hide()
            this.setImageURI(null)
        }
    } catch (e: Exception) {
        this.hide()
    }
}

fun ImageView.setImageFromBytes(data: ByteArray) {
    this.setImageBitmap(data.toBitmap())
}