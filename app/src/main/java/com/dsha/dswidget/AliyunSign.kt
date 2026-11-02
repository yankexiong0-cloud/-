package com.dsha.dswidget

import android.util.Base64
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 阿里云 RPC 风格 OpenAPI 的签名（SignatureVersion=1.0，HMAC-SHA1）。
 *
 * 规则：
 *  1. 公共参数 + 业务参数按 key 字典序排列，逐个 percentEncode 后拼成 `k=v&k=v`
 *  2. StringToSign = HTTPMethod + "&" + percentEncode("/") + "&" + percentEncode(上面的串)
 *  3. Signature  = Base64(HMAC-SHA1(AccessKeySecret + "&", StringToSign))
 */
object AliyunSign {

    private val HEX = "0123456789ABCDEF".toCharArray()

    /** RFC3986 编码：只放行 A-Z a-z 0-9 - _ . ~，其余按 UTF-8 字节转 %XX（空格是 %20 不是 +） */
    fun percentEncode(value: String): String {
        val sb = StringBuilder()
        for (b in value.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            val keep = (c in 0x41..0x5A) || (c in 0x61..0x7A) || (c in 0x30..0x39) ||
                c == 0x2D || c == 0x5F || c == 0x2E || c == 0x7E
            if (keep) {
                sb.append(c.toChar())
            } else {
                sb.append('%').append(HEX[c shr 4]).append(HEX[c and 0x0F])
            }
        }
        return sb.toString()
    }

    /** HTTP 方法 + 参数（不含 Signature）→ 签名字符串 */
    fun sign(method: String, params: Map<String, String>, accessKeySecret: String): String {
        val canonical = params.entries
            .map { percentEncode(it.key) to percentEncode(it.value) }
            .sortedBy { it.first }
            .joinToString("&") { "${it.first}=${it.second}" }

        val stringToSign = "$method&${percentEncode("/")}&${percentEncode(canonical)}"

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec((accessKeySecret + "&").toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val raw = mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    /** 把参数拼成查询串（含 Signature），key 按字典序 */
    fun toQuery(params: Map<String, String>, signature: String): String {
        val all = LinkedHashMap(params)
        all["Signature"] = signature
        return all.keys.sorted().joinToString("&") { k ->
            percentEncode(k) + "=" + percentEncode(all[k] ?: "")
        }
    }

    fun isoUtcNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    fun nonce(): String = UUID.randomUUID().toString()
}
