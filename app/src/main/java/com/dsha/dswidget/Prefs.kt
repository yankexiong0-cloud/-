package com.dsha.dswidget

import android.content.Context

object Prefs {
    private const val NAME = "dswidget"
    private const val KEY_API = "api_key"

    fun apiKey(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_API, "") ?: ""

    fun setApiKey(ctx: Context, value: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_API, value.trim()).apply()
    }
}
