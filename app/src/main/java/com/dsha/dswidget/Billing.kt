package com.dsha.dswidget

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * DeepSeek 计费时段：北京时间 00:30 - 08:30 为谷价期（优惠）。
 * 一律按 UTC+8 计算，与设备本地时区无关。
 */
object Billing {

    private val BJ: TimeZone = TimeZone.getTimeZone("GMT+8")
    const val OFFPEAK_START = 0.5f
    const val OFFPEAK_END = 8.5f

    data class Tier(val offPeak: Boolean, val text: String)

    /** 当前北京时间的小时数（含小数） */
    fun beijingHour(): Float {
        val cal = Calendar.getInstance(BJ)
        return cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60f
    }

    /** 北京时间的日期串，用于「今天是否已提醒过」 */
    fun beijingDate(): String {
        val cal = Calendar.getInstance(BJ)
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun now(): Tier {
        val h = beijingHour()
        return if (h >= OFFPEAK_START && h <= OFFPEAK_END) {
            Tier(true, String.format(Locale.US, "谷价期 · 还剩 %.1fh", OFFPEAK_END - h))
        } else {
            val next = if (h < OFFPEAK_START) OFFPEAK_START - h else 24f - h + OFFPEAK_START
            Tier(false, String.format(Locale.US, "标准价 · %.1fh 后谷价", next))
        }
    }
}
