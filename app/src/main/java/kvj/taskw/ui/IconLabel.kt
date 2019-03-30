package kvj.taskw.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kvj.taskw.R

import kotlinx.android.synthetic.main.icon_label.view.*

class IconLabel @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {
    init {
        LayoutInflater.from(context).inflate(R.layout.icon_label, this, true)

        attrs?.let {
            val a = context.obtainStyledAttributes(attrs, R.styleable.IconLabel, defStyle, 0)

            icon.apply {
                setImageResource(a.getResourceId(R.styleable.IconLabel_android_src, 0))

                val size = a.getDimensionPixelSize(R.styleable.IconLabel_iconWidth, 0)
                layoutParams.width = size
                layoutParams.height = size
            }

            value.apply {
                setTextAppearance(context, a.getResourceId(R.styleable.IconLabel_android_textAppearance, android.R.attr.textAppearanceMedium))
                setTextColor(a.getColor(R.styleable.IconLabel_android_textColor, android.R.attr.textColorPrimary))
            }

            a.recycle()
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (index > 1)
            value.visibility = GONE

        super.addView(child, index, params)
    }
}
