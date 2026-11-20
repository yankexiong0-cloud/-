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

    /** 页面自己去请求 billing 接口时，把它的请求头原样记下来（org 就藏在这里） */
    private var lastHeaders: Map<String, String> = emptyMap()

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
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?
                ): android.webkit.WebResourceResponse? {
                    val u = request?.url?.toString() ?: ""
                    if (u.contains("/console/api/billing")) {
                        lastHeaders = request.requestHeaders
                    }
                    return null   // 返回 null = 不拦截，请求照常发出去
                }
            }
            loadUrl("https://opencode.ai/console")
        }
        root.addView(web)

        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)
        cm.setAcceptThirdPartyCookies(web, true)

        setContentView(root)
    }

    private fun captureCookie() {
        // 优先用「页面自己发过的请求头」：里面带着 x-org-id 和完整 Cookie
        val hdrOrg = lastHeaders.entries
            .firstOrNull { it.key.equals("x-org-id", ignoreCase = true) }
            ?.value
            ?.trim()
            .orEmpty()

        if (lastHeaders.isNotEmpty()) {
            saveAll(hdrOrg)
            return
        }

        // 没拦到就退回读 localStorage（页面还没发过 billing 请求时会走这里）
        web.evaluateJavascript(
            "(function(){try{return localStorage.getItem('opencode-console.org-id')||''}catch(e){return ''}})()"
        ) { quoted ->
            val org = (quoted ?: "").trim().trim('"')
            saveAll(org)
        }
    }

    private fun saveAll(org: String) {
        val cookie = try {
            CookieManager.getInstance().getCookie("https://opencode.ai")
        } catch (e: Exception) {
            null
        }
        if (cookie.isNullOrBlank() || !cookie.contains("console_session")) {
            Toast.makeText(this, "还没登录成功，或页面没加载完，稍等一下再点", Toast.LENGTH_LONG).show()
            return
        }

        Prefs.setOcCookie(this, cookie)
        Prefs.setOcOrg(this, org)
        OpencodeWidget.refreshAsync(applicationContext)
        Toast.makeText(
            this,
            if (org.isBlank()) "已保存 cookie，但没拿到 org —— 回页面点一下 Activity 刷新再来" else "cookie 与 org 已保存，正在刷新…",
            Toast.LENGTH_SHORT
        ).show()
        if (org.isNotBlank()) finish()
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
