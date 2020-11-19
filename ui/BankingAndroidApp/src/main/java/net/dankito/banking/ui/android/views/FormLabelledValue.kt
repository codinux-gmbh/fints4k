package net.dankito.banking.ui.android.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.view_form_labelled_value.view.*
import net.dankito.banking.ui.android.R
import net.dankito.banking.ui.android.extensions.setTextColorForAmount
import net.dankito.utils.android.extensions.setVisibility
import net.dankito.banking.ui.presenter.BankingPresenter
import net.dankito.utils.multiplatform.BigDecimal


open class FormLabelledValue @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    protected lateinit var txtLabel: TextView

    protected lateinit var txtValue: TextView


    init {
        setupUi(context, attrs)
    }

    private fun setupUi(context: Context, attrs: AttributeSet?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rootView = inflater.inflate(R.layout.view_form_labelled_value, this, true)

        rootView.apply {
            txtLabel = findViewById(R.id.txtLabel)
            txtValue = findViewById(R.id.txtValue)

            context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FormLabelledValue,
                0, 0).apply {

                try {
                    txtLabel.text = getString(R.styleable.FormLabelledValue_android_label)
                    txtValue.text = getString(R.styleable.FormLabelledValue_android_value)
                } finally {
                    recycle()
                }
            }
        }
    }


    open var label: CharSequence
        get() = txtLabel.text
        set(value) {
            txtLabel.text = value
        }

    open var value: CharSequence
        get() = txtValue.text
        set(value) {
            txtValue.text = value
        }

    open fun setValueAndVisibilityIfValueIsSet(value: CharSequence?) {
        this.setVisibility(value != null)
        this.value = value ?: ""
    }

    open fun showAmount(presenter: BankingPresenter, amount: BigDecimal, currencyIsoCode: String? = null) {
        value = presenter.formatAmount(amount, currencyIsoCode)
        txtValue.setTextColorForAmount(amount)
    }

}