
package com.abuwasam.network

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(24, 24, 24, 24)
        layout.setBackgroundColor(Color.rgb(240, 245, 250))

        val title = TextView(this)
        title.text = "أبو وسام\nفني الشبكات"
        title.textSize = 28f
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.rgb(0, 70, 120))
        layout.addView(title)

        val info = TextView(this)
        info.text = "أداة مساعدة لفحص أجهزة الشبكة"
        info.textSize = 17f
        info.gravity = Gravity.CENTER
        layout.addView(info)

        val address = EditText(this)
        address.hint = "عنوان الراوتر"
        address.setText("192.168.1.1")
        address.textSize = 18f
        address.singleLine = true
        layout.addView(
            address,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val button = Button(this)
        button.text = "فتح صفحة الراوتر"
        layout.addView(button)

        button.setOnClickListener {
            var url = address.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(this, "أدخل عنوان الراوتر", Toast.LENGTH_SHORT).show()
            } else {
                if (!url.startsWith("http://") &&
                    !url.startsWith("https://")) {
                    url = "http://$url"
                }

                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "تعذر فتح العنوان",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        setContentView(layout)
    }
}
