
package com.pdfsmart.toolkit

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "PDF Smart Toolkit"
            textSize = 28f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "Your offline PDF utility"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        layout.addView(subtitle)

        val tools = listOf(
            "Merge PDF",
            "Split PDF",
            "Compress PDF",
            "PDF to Images",
            "Images to PDF",
            "Protect PDF"
        )

        tools.forEach { tool ->

            val button = Button(this).apply {
                text = tool
                textSize = 16f
            }

            layout.addView(button)
        }

        setContentView(layout)
    }
}
