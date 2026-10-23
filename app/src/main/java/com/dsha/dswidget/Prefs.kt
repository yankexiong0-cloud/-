package com.dsha.dswidget

import android.content.Context

object Prefs {
    private const val NAME = "dswidget"
    private const val KEY_API = "api_key"
    private const val KEY_LAST_NOTICE = "last_offpeak_notice"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun apiKey(ctx: Context): String = sp(ctx).getString(KEY_API, "") ?: ""

    fun setApiKey(ctx: Context, value: String) {
        sp(ctx).edit().putString(KEY_API, value.trim()).apply()
    }

    fun lastOffPeakNotice(ctx: Context): String = sp(ctx).getString(KEY_LAST_NOTICE, "") ?: ""

    fun setLastOffPeakNotice(ctx: Context, date: String) {
        sp(ctx).edit().putString(KEY_LAST_NOTICE, date).apply()
    }
}
