package com.dsha.dswidget

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
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
    private lateinit var tierView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "DeepSeek 余额"

        Notifications.ensureChannel(this)
        askNotificationPermission()

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

        tierView = TextView(this).apply {
            text = tierText()
            textSize = 13f
            setPadding(0, pad, 0, pad)
        }
        root.addView(tierView)

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
                tierView.text = tierText()
            }
        })

        root.addView(Button(this).apply {
            text = "立即刷新一次"
            setOnClickListener {
                BalanceWidget.refreshAsync(this@MainActivity)
                status.text = "已触发刷新。"
                tierView.text = tierText()
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

    override fun onResume() {
        super.onResume()
        if (::tierView.isInitialized) tierView.text = tierText()
    }

    private fun tierText(): String {
        val t = Billing.now()
        return "当前计费：${t.text}\n（谷价期＝北京时间 00:30–08:30，进入时会通知你）"
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }
}
