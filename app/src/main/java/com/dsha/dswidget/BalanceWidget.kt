package com.dsha.dswidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class BalanceWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAsync(context.applicationContext)
    }

    override fun onEnabled(context: Context) {
        RefreshScheduler.schedule(context.applicationContext)
        refreshAsync(context.applicationContext)
    }

    override fun onDisabled(context: Context) {
        RefreshScheduler.cancel(context.applicationContext)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            refreshAsync(context.applicationContext)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.dsha.dswidget.REFRESH"

        fun refreshAsync(context: Context) {
            thread {
                val result = BalanceApi.fetch(Prefs.apiKey(context))
                render(context, result)
            }
        }

        fun render(context: Context, result: BalanceApi.Result) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, BalanceWidget::class.java))
            if (ids.isEmpty()) return

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_balance)

                if (result.ok) {
                    val symbol = when (result.currency) {
                        "CNY" -> "¥"
                        "USD" -> "$"
                        else -> result.currency + " "
                    }
                    views.setTextViewText(
                        R.id.widget_amount,
                        symbol + String.format(Locale.US, "%.2f", result.total)
                    )
                    views.setTextViewText(
                        R.id.widget_detail,
                        String.format(Locale.US, "充值 %.2f · 赠送 %.2f", result.toppedUp, result.granted)
                    )
                    val stamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    views.setTextViewText(R.id.widget_updated, "更新于 $stamp")
                } else {
                    views.setTextViewText(R.id.widget_amount, context.getString(R.string.widget_empty))
                    views.setTextViewText(R.id.widget_detail, result.error ?: "读取失败")
                    views.setTextViewText(R.id.widget_updated, "")
                }

                val openIntent = Intent(context, MainActivity::class.java)
                val openPi = PendingIntent.getActivity(context, 0, openIntent, flags)
                views.setOnClickPendingIntent(R.id.widget_root, openPi)
                views.setOnClickPendingIntent(R.id.widget_amount, openPi)

                val refreshIntent = Intent(context, BalanceWidget::class.java).setAction(ACTION_REFRESH)
                val refreshPi = PendingIntent.getBroadcast(context, 1, refreshIntent, flags)
                views.setOnClickPendingIntent(R.id.widget_title, refreshPi)

                mgr.updateAppWidget(id, views)
            }
        }
    }
}
