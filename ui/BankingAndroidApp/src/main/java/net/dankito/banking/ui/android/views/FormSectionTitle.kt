package net.dankito.banking.ui.android.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_form_section_title.view.*
import net.dankito.banking.ui.android.R


open class FormSectionTitle @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {


    init {
        setupUi(context, attrs)
    }

    private fun setupUi(context: Context, attrs: AttributeSet?) {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rootView = inflater.inflate(R.layout.view_form_section_title, this, true)

        rootView.apply {

            context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.FormEditText,
                0, 0).apply {

                try {
                    txtvwSectionTitle.text = getString(R.styleable.FormEditText_android_text)
                } finally {
                    recycle()
                }
            }
        }
    }


    open var title: CharSequence
        get() = txtvwSectionTitle.text
        set(value) = txtvwSectionTitle.setText(value)

    open fun setTitle(@StringRes titleResId: Int) {
        this.title = context.getString(titleResId)
    }

}