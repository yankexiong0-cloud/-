package com.dsha.dswidget

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BalanceApi {

    data class Result(
        val ok: Boolean,
        val currency: String = "",
        val total: Double = 0.0,
        val granted: Double = 0.0,
        val toppedUp: Double = 0.0,
        val error: String? = null
    )

    private const val ENDPOINT = "https://api.deepseek.com/user/balance"

    fun fetch(key: String): Result {
        if (key.isBlank()) return Result(false, error = "点开填写 API Key")
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Authorization", "Bearer $key")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                Result(false, error = "HTTP $code")
            } else {
                val json = JSONObject(body)
                val infos = json.optJSONArray("balance_infos")
                if (infos == null || infos.length() == 0) {
                    Result(false, error = "没有余额信息")
                } else {
                    val it = infos.getJSONObject(0)
                    Result(
                        ok = true,
                        currency = it.optString("currency", "CNY"),
                        total = it.optString("total_balance", "0").toDoubleOrNull() ?: 0.0,
                        granted = it.optString("granted_balance", "0").toDoubleOrNull() ?: 0.0,
                        toppedUp = it.optString("topped_up_balance", "0").toDoubleOrNull() ?: 0.0
                    )
                }
            }
        } catch (e: Exception) {
            Result(false, error = e.message ?: "网络错误")
        } finally {
            conn?.disconnect()
        }
    }
}
