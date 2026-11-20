package com.dsha.dswidget

import android.content.Context

object Prefs {
    private const val NAME = "dswidget"
    private const val KEY_API = "api_key"
    private const val KEY_LAST_NOTICE = "last_offpeak_notice"
    private const val KEY_QWEN_AK_ID = "qwen_ak_id"
    private const val KEY_QWEN_AK_SECRET = "qwen_ak_secret"
    private const val KEY_OC_COOKIE = "opencode_cookie"
    private const val KEY_OC_ORG = "opencode_org"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun apiKey(ctx: Context): String = sp(ctx).getString(KEY_API, "") ?: ""

    fun setApiKey(ctx: Context, value: String) {
        sp(ctx).edit().putString(KEY_API, value.trim()).apply()
    }

    fun lastOffPeakNotice(ctx: Context): String = sp(ctx).getString(KEY_LAST_NOTICE, "") ?: ""

    fun setLastOffPeakNotice(ctx: Context, date: String) {
        sp(ctx).edit().putString(KEY_LAST_NOTICE, date).apply()
    }

    fun qwenAkId(ctx: Context): String = sp(ctx).getString(KEY_QWEN_AK_ID, "") ?: ""

    fun qwenAkSecret(ctx: Context): String = sp(ctx).getString(KEY_QWEN_AK_SECRET, "") ?: ""

    fun setQwenCredential(ctx: Context, akId: String, akSecret: String) {
        sp(ctx).edit()
            .putString(KEY_QWEN_AK_ID, akId.trim())
            .putString(KEY_QWEN_AK_SECRET, akSecret.trim())
            .apply()
    }

    fun ocCookie(ctx: Context): String = sp(ctx).getString(KEY_OC_COOKIE, "") ?: ""

    fun setOcCookie(ctx: Context, value: String) {
        sp(ctx).edit().putString(KEY_OC_COOKIE, value.trim()).apply()
    }

    fun ocOrg(ctx: Context): String = sp(ctx).getString(KEY_OC_ORG, "") ?: ""

    fun setOcOrg(ctx: Context, value: String) {
        sp(ctx).edit().putString(KEY_OC_ORG, value.trim()).apply()
    }
}
