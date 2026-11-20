package com.dsha.dswidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * OpenCode Console 余额。
 *
 * 说明：opencode 的 API key（oc_sk_...）**查不了余额**——`/console/api/billing/status`
 * 只认浏览器会话 cookie（`__Host-console_session`），带 Bearer key 会被 403 拒掉。
 * 所以这里用「从浏览器复制出来的会话 cookie」来读，cookie 失效时卡片会提示重新贴。
 *
 *   GET https://opencode.ai/console/api/billing/status
 *   Cookie: __Host-console_session=...
 *
 * 金额单位是 micro-cents（1 USD = 100,000,000 micro-cents）。
 */
object OpencodeApi {

    data class Result(
        val ok: Boolean,
        val balanceUsd: Double = 0.0,     // 剩余充值余额
        val availableUsd: Double = 0.0,   // 可用（含额度）
        val mode: String = "",            // 计费模式，如 pay-as-you-go
        val error: String? = null
    )

    private const val ENDPOINT = "https://opencode.ai/console/api/billing/status"

    fun fetch(cookie: String, orgId: String = ""): Result {
        val raw = normalizeCookie(cookie)
        if (raw.isBlank()) return Result(false, error = "点开填 Console 会话 cookie")

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                // 不带 User-Agent 会被 Cloudflare 拦（error code: 1010）
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
                )
                setRequestProperty("Referer", "https://opencode.ai/console/")
                setRequestProperty("Cookie", raw)
                // 浏览器会把 localStorage 里的 org 作为 x-org-id 发出去，不带就回 OrgRequired
                if (orgId.isNotBlank()) setRequestProperty("x-org-id", orgId)
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (code == 401) return Result(false, error = "会话已过期，重新贴 cookie")
            val tagEarly = try { JSONObject(body).optString("_tag") } catch (e: Exception) { "" }
            if (tagEarly == "OrgRequired") return Result(false, error = "缺 org，重新登录一次")
            if (code == 403) return Result(false, error = "cookie 无 billing 权限")

            val json = try {
                JSONObject(body)
            } catch (e: Exception) {
                return Result(false, error = "HTTP $code")
            }

            if (code !in 200..299) {
                val tag = json.optString("_tag").ifBlank { json.optString("message") }
                return Result(false, error = (tag.ifBlank { "HTTP $code" }).take(40))
            }

            Result(
                ok = true,
                balanceUsd = microCentsToUsd(json.optLong("balanceMicroCents", 0)),
                availableUsd = microCentsToUsd(json.optLong("availableMicroCents", 0)),
                mode = json.optString("billingMode").ifBlank { json.optString("mode") }
            )
        } catch (e: Exception) {
            Result(false, error = e.message?.take(40) ?: "网络错误")
        } finally {
            conn?.disconnect()
        }
    }

    /** 用户可能贴整段 "Cookie: xxx"，也可能只贴值；顺手补齐名字 */
    private fun normalizeCookie(input: String): String {
        var s = input.trim()
        if (s.startsWith("Cookie:", ignoreCase = true)) s = s.substring(7).trim()
        if (s.isBlank()) return ""
        return if (s.contains("=")) s else "__Host-console_session=$s"
    }

    private fun microCentsToUsd(v: Long): Double = v / 100_000_000.0

    fun formatUsd(value: Double): String = "$" + String.format(Locale.US, "%.2f", value)
}
