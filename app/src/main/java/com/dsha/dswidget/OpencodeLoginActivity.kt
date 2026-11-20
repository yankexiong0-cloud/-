package com.dsha.dswidget

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * 在 App 内置浏览器里登录 opencode.ai/console，登录完成后直接把会话 cookie 捞出来存好。
 *
 * 为什么要这样：openCode 的 billing 接口只认 `__Host-console_session`，而这个 cookie 是
 * HttpOnly 的，网页 JS 读不到；手机浏览器又没有开发者工具。用 WebView 登录后通过
 * CookieManager 读取，是绕开「必须有电脑」的唯一稳妥办法。
 */
class OpencodeLoginActivity : Activity() {

    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "登录 OpenCode Console"

        val pad = (14 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(TextView(this).apply {
            text = "登录后点下面的按钮读取 cookie。验证链接如果发到邮箱，直接在页面里打开输入即可。"
            textSize = 12f
            setPadding(pad, pad, pad, pad / 2)
        })

        root.addView(Button(this).apply {
            text = "我已登录，读取 cookie"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { captureCookie() }
        })

        web = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            loadUrl("https://opencode.ai/console")
        }
        root.addView(web)

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(web, true)

        setContentView(root)
    }

    private fun captureCookie() {
        val cookie = try {
            CookieManager.getInstance().getCookie("https://opencode.ai")
        } catch (e: Exception) {
            null
        }
        if (cookie.isNullOrBlank() || !cookie.contains("console_session")) {
            Toast.makeText(this, "还没登录成功，或页面没加载完，稍等一下再点", Toast.LENGTH_LONG).show()
            return
        }

        // cookie 名是 __Host-console_session，前面有 __Host- 前缀，不能用 startsWith 匹配
        val session = cookie.split(";")
            .map { it.trim() }
            .firstOrNull { it.contains("console_session=") }
            ?.substringAfter("console_session=")
            ?.trim()

        if (session.isNullOrBlank()) {
            Toast.makeText(
                this,
                "没找到会话 cookie。当前拿到的：" + cookie.take(60),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        Prefs.setOcCookie(this, session)
        OpencodeWidget.refreshAsync(applicationContext)
        Toast.makeText(this, "cookie 已保存，正在刷新余额…", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onDestroy() {
        try {
            if (::web.isInitialized) {
                web.stopLoading()
                web.destroy()
            }
        } catch (e: Exception) {
            // WebView 回收失败不该让 App 崩
        }
        super.onDestroy()
    }
}
