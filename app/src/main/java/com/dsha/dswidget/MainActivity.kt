package com.dsha.dswidget

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var keyInput: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "DeepSeek 余额"

        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "填入 DeepSeek API Key"
            textSize = 18f
        })

        root.addView(TextView(this).apply {
            text = "Key 只保存在本应用私有目录，不会上传到任何地方。"
            textSize = 12f
            setPadding(0, pad / 2, 0, pad / 2)
        })

        keyInput = EditText(this).apply {
            hint = "sk-..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(Prefs.apiKey(this@MainActivity))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(keyInput)

        root.addView(Button(this).apply {
            text = "保存并刷新"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                Prefs.setApiKey(this@MainActivity, keyInput.text.toString())
                Toast.makeText(this@MainActivity, "已保存，正在刷新…", Toast.LENGTH_SHORT).show()
                RefreshScheduler.schedule(this@MainActivity)
                BalanceWidget.refreshAsync(this@MainActivity)
                status.text = "已保存。等几秒后回桌面看小组件。"
            }
        })

        root.addView(Button(this).apply {
            text = "立即刷新一次"
            setOnClickListener {
                BalanceWidget.refreshAsync(this@MainActivity)
                status.text = "已触发刷新。"
            }
        })

        status = TextView(this).apply {
            text = "添加小组件：长按桌面空白 → 小部件 → DeepSeek 余额"
            textSize = 12f
            gravity = Gravity.START
            setPadding(0, pad, 0, 0)
        }
        root.addView(status)

        setContentView(root)
    }
}
