package com.dsha.dswidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * 千问（阿里云百炼 / DashScope）本身只提供 `sk-` 推理 key，没有余额查询接口，
 * 但它的费用是走阿里云账户的，所以用费用中心的开放接口查：
 *
 *   Action  : QueryAccountBalance
 *   Version : 2017-12-14
 *   Endpoint: https://business.aliyuncs.com/
 *
 * 需要阿里云 AccessKey（建议 RAM 子账号，只给 `bss:DescribeAccount` 只读权限）。
 */
object QwenApi {

    data class Result(
        val ok: Boolean,
        val currency: String = "CNY",
        val available: Double = 0.0,   // 可用额度
        val cash: Double = 0.0,        // 现金余额
        val credit: Double = 0.0,      // 信控额度
        val error: String? = null
    )

    private const val ENDPOINT = "https://business.aliyuncs.com/"

    fun fetch(akId: String, akSecret: String): Result {
        if (akId.isBlank() || akSecret.isBlank()) {
            return Result(false, error = "点开填阿里云 AccessKey")
        }

        val params = linkedMapOf(
            "Action" to "QueryAccountBalance",
            "Version" to "2017-12-14",
            "Format" to "JSON",
            "AccessKeyId" to akId,
            "SignatureMethod" to "HMAC-SHA1",
            "SignatureVersion" to "1.0",
            "SignatureNonce" to AliyunSign.nonce(),
            "Timestamp" to AliyunSign.isoUtcNow()
        )

        val url = ENDPOINT + "?" + AliyunSign.toQuery(params, AliyunSign.sign("GET", params, akSecret))

        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

            val json = try {
                JSONObject(body)
            } catch (e: Exception) {
                return Result(false, error = "HTTP $code")
            }

            if (!json.optBoolean("Success", false)) {
                val msg = json.optString("Message").ifBlank { json.optString("Code") }
                return Result(false, error = friendly(msg.ifBlank { "HTTP $code" }))
            }

            val data = json.optJSONObject("Data") ?: return Result(false, error = "没有余额数据")
            Result(
                ok = true,
                currency = data.optString("Currency", "CNY").ifBlank { "CNY" },
                available = data.optDoubleFlexible("AvailableAmount"),
                cash = data.optDoubleFlexible("AvailableCashAmount"),
                credit = data.optDoubleFlexible("CreditAmount")
            )
        } catch (e: Exception) {
            Result(false, error = e.message ?: "网络错误")
        } finally {
            conn?.disconnect()
        }
    }

    /** 余额字段可能是字符串也可能是数字，统一转 Double */
    private fun JSONObject.optDoubleFlexible(name: String): Double {
        if (isNull(name)) return 0.0
        val raw = opt(name)
        val text = when (raw) {
            is Number -> return raw.toDouble()
            is String -> raw
            else -> return 0.0
        }
        return text.trim().toDoubleOrNull() ?: 0.0
    }

    private fun friendly(msg: String): String = when {
        msg.contains("InvalidAccessKeyId.NotFound") -> "AccessKeyId 不存在"
        msg.contains("SignatureDoesNotMatch") -> "Secret 不对（签名不匹配）"
        msg.contains("Forbidden") || msg.contains("NotAuthorized") -> "这个 Key 没有 bss 只读权限"
        msg.contains("InvalidTimeStamp") -> "手机时间不准，校准后再试"
        msg.length > 40 -> msg.take(40)
        else -> msg
    }

    fun formatMoney(currency: String, value: Double): String {
        val symbol = when (currency.uppercase(Locale.US)) {
            "CNY" -> "¥"
            "USD" -> "$"
            "JPY" -> "¥"
            else -> currency + " "
        }
        return symbol + String.format(Locale.US, "%.2f", value)
    }
}
