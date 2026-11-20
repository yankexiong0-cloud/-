package com.dsha.dswidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * OpenCode Console 余额小组件。
 *
 * 数据靠浏览器会话 cookie，所以会有「登录过期」这一天——过期时卡片直接说，
 * 别显示一个假的 0，那样反而误导。
 */
class OpencodeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAsync(context.applicationContext)
    }

    override fun onEnabled(context: Context) {
        RefreshScheduler.schedule(context.applicationContext)
        refreshAsync(context.applicationContext)
    }

    override fun onDisabled(context: Context) {
        RefreshScheduler.cancelIfNoWidgets(context.applicationContext)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refreshAsync(context.applicationContext)
            ACTION_OPEN -> openConsole(context.applicationContext)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.dsha.dswidget.OPENCODE_REFRESH"
        const val ACTION_OPEN = "com.dsha.dswidget.OPENCODE_OPEN"

        const val CONSOLE_URL = "https://opencode.ai/console"

        fun refreshAsync(context: Context) {
            thread {
                val result = try {
                    OpencodeApi.fetch(Prefs.ocCookie(context))
                } catch (t: Throwable) {
                    OpencodeApi.Result(false, error = t.message ?: "刷新异常")
                }
                render(context, result)
            }
        }

        fun render(context: Context, result: OpencodeApi.Result) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, OpencodeWidget::class.java))
            if (ids.isEmpty()) return

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val configured = Prefs.ocCookie(context).isNotBlank()

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_opencode)

                if (result.ok) {
                    views.setTextViewText(R.id.opencode_amount, OpencodeApi.formatUsd(result.balanceUsd))
                    views.setTextViewText(
                        R.id.opencode_detail,
                        if (result.availableUsd > result.balanceUsd + 0.001) {
                            "可用 " + OpencodeApi.formatUsd(result.availableUsd)
                        } else {
                            result.mode.ifBlank { "Console 余额" }
                        }
                    )
                    val stamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    views.setTextViewText(R.id.opencode_updated, "更新于 $stamp")
                    views.setTextViewText(
                        R.id.opencode_note,
                        if (result.balanceUsd <= 0.0) "余额见底 · 点开充值" else "点开 Console"
                    )
                } else if (!configured) {
                    views.setTextViewText(R.id.opencode_amount, "点开查看")
                    views.setTextViewText(R.id.opencode_detail, "未配置会话 cookie")
                    views.setTextViewText(R.id.opencode_updated, "")
                    views.setTextViewText(R.id.opencode_note, "OpenCode Console · 点击直达")
                } else {
                    views.setTextViewText(R.id.opencode_amount, context.getString(R.string.widget_empty))
                    views.setTextViewText(R.id.opencode_detail, result.error ?: "读取失败")
                    views.setTextViewText(R.id.opencode_updated, "")
                    views.setTextViewText(R.id.opencode_note, "点开重新贴 cookie")
                }

                val openIntent = Intent(context, OpencodeWidget::class.java).setAction(ACTION_OPEN)
                val openPi = PendingIntent.getBroadcast(context, 40, openIntent, flags)
                views.setOnClickPendingIntent(R.id.opencode_root, openPi)
                views.setOnClickPendingIntent(R.id.opencode_amount, openPi)

                val refreshIntent = Intent(context, OpencodeWidget::class.java).setAction(ACTION_REFRESH)
                val refreshPi = PendingIntent.getBroadcast(context, 41, refreshIntent, flags)
                views.setOnClickPendingIntent(R.id.opencode_title, refreshPi)

                mgr.updateAppWidget(id, views)
            }
        }

        fun openConsole(context: Context) {
            try {
                val view = Intent(Intent.ACTION_VIEW, Uri.parse(CONSOLE_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(view)
            } catch (e: Exception) {
                // 没浏览器就算了，别崩
            }
        }
    }
}
