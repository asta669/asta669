package com.asta669.wakeup

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** Small native layout vocabulary shared by the alarm and its setup screens. */
object ScreenUi {
    fun dp(c: Context, value: Int) = (value * c.resources.displayMetrics.density).toInt()
    fun screen(a: Activity, title: String, subtitle: String): LinearLayout {
        val scroll = ScrollView(a).apply {
            setBackgroundColor(ContextCompat.getColor(a, R.color.bg))
            isFillViewport = true
        }
        val column = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(a, 22), dp(a, 24), dp(a, 22), dp(a, 32))
        }
        scroll.addView(column)
        a.setContentView(scroll)
        text(column, "WAKE UP  /  VOTRE RITUEL", 11, secondary = true).letterSpacing = .12f
        text(column, title, 32, bold = true)
        text(column, subtitle, 15, secondary = true)
        return column
    }
    fun text(parent: LinearLayout, value: String, size: Int = 16,
             bold: Boolean = false, secondary: Boolean = false): TextView = TextView(parent.context).also {
        it.text = value
        it.textSize = size.toFloat()
        it.setTextColor(ContextCompat.getColor(parent.context,
            if (secondary) R.color.text_secondary else R.color.text_primary))
        it.setLineSpacing(dp(parent.context, 3).toFloat(), 1f)
        if (bold) it.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        parent.addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(parent.context, 10) })
    }
    fun card(parent: LinearLayout): LinearLayout {
        val c = parent.context
        val card = MaterialCardView(c).apply {
            radius = dp(c, 26).toFloat()
            cardElevation = dp(c, 4).toFloat()
            setCardBackgroundColor(ContextCompat.getColor(c, R.color.card))
            strokeColor = ContextCompat.getColor(c, R.color.white)
            strokeWidth = dp(c, 1)
        }
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(c, 14); bottomMargin = dp(c, 8)
        })
        val content = LinearLayout(c).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(c, 22), dp(c, 22), dp(c, 22), dp(c, 18))
        }
        card.addView(content)
        return content
    }
    fun button(parent: LinearLayout, title: String, secondary: Boolean = false,
               click: (View) -> Unit): MaterialButton {
        val c = parent.context
        return MaterialButton(c).also {
            it.text = title
            it.isAllCaps = false
            it.cornerRadius = dp(c, 18)
            it.minHeight = dp(c, 56)
            if (secondary) {
                it.backgroundTintList = ContextCompat.getColorStateList(c, R.color.accent_soft)
                it.setTextColor(ContextCompat.getColor(c, R.color.accent))
            }
            it.setOnClickListener(click)
            parent.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(c, 4) })
        }
    }
}
