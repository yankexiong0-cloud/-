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
 * 千问（阿里云百炼）余额小组件。
 *
 * 两种形态：
 *  - 配了阿里云 AccessKey → 显示真实可用余额（费用中心 QueryAccountBalance）
 *  - 没配 → 显示入口卡片，点一下直达百炼控制台
 */
class QwenWidget : AppWidgetProvider() {

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
        const val ACTION_REFRESH = "com.dsha.dswidget.QWEN_REFRESH"
        const val ACTION_OPEN = "com.dsha.dswidget.QWEN_OPEN"

        /** 百炼控制台（模型服务/费用入口） */
        const val CONSOLE_URL = "https://bailian.console.aliyun.com/"
        /** 费用中心 → 资金管理（真正看余额的地方） */
        const val FINANCE_URL = "https://usercenter2.aliyun.com/finance/fund-management"

        fun refreshAsync(context: Context) {
            thread {
                // 任何异常都要落成「卡片上的错误文案」，不能让线程静默死掉、卡片一直停在 —
                val result = try {
                    QwenApi.fetch(Prefs.qwenAkId(context), Prefs.qwenAkSecret(context))
                } catch (t: Throwable) {
                    QwenApi.Result(false, error = t.message ?: "刷新异常")
                }
                render(context, result)
            }
        }

        fun render(context: Context, result: QwenApi.Result) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, QwenWidget::class.java))
            if (ids.isEmpty()) return

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val configured = Prefs.qwenAkId(context).isNotBlank() && Prefs.qwenAkSecret(context).isNotBlank()

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_qwen)

                if (result.ok) {
                    views.setTextViewText(
                        R.id.qwen_amount,
                        QwenApi.formatMoney(result.currency, result.available)
                    )
                    views.setTextViewText(R.id.qwen_detail, detailText(result))
                    val stamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    views.setTextViewText(R.id.qwen_updated, "更新于 $stamp")
                    // 可用余额为 0 时，多半是在吃新人免费额度（免费额度不体现在账户余额里）
                    views.setTextViewText(
                        R.id.qwen_note,
                        if (result.available > 0.0) "账户余额 · 点开费用中心" else "免费额度另算 · 点开看余量"
                    )
                } else if (!configured) {
                    views.setTextViewText(R.id.qwen_amount, "点开查看")
                    views.setTextViewText(R.id.qwen_detail, "未配置阿里云 AccessKey")
                    views.setTextViewText(R.id.qwen_updated, "")
                    views.setTextViewText(R.id.qwen_note, "百炼控制台 · 点击直达")
                } else {
                    views.setTextViewText(R.id.qwen_amount, context.getString(R.string.widget_empty))
                    views.setTextViewText(R.id.qwen_detail, result.error ?: "读取失败")
                    views.setTextViewText(R.id.qwen_updated, "")
                    views.setTextViewText(R.id.qwen_note, "点开费用中心查看")
                }

                val openIntent = Intent(context, QwenWidget::class.java).setAction(ACTION_OPEN)
                val openPi = PendingIntent.getBroadcast(context, 30, openIntent, flags)
                views.setOnClickPendingIntent(R.id.qwen_root, openPi)
                views.setOnClickPendingIntent(R.id.qwen_amount, openPi)

                val refreshIntent = Intent(context, QwenWidget::class.java).setAction(ACTION_REFRESH)
                val refreshPi = PendingIntent.getBroadcast(context, 31, refreshIntent, flags)
                views.setOnClickPendingIntent(R.id.qwen_title, refreshPi)

                mgr.updateAppWidget(id, views)
            }
        }

        private fun detailText(result: QwenApi.Result): String {
            val cash = String.format(Locale.US, "现金 %.2f", result.cash)
            return if (result.credit > 0.0) {
                cash + String.format(Locale.US, " · 信控 %.2f", result.credit)
            } else {
                cash
            }
        }

        fun openConsole(context: Context) {
            val ok = Prefs.qwenAkId(context).isNotBlank() && Prefs.qwenAkSecret(context).isNotBlank()
            val target = if (ok) FINANCE_URL else CONSOLE_URL
            try {
                val view = Intent(Intent.ACTION_VIEW, Uri.parse(target))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(view)
            } catch (e: Exception) {
                // 没有浏览器就什么都不做，别崩
            }
        }
    }
}
