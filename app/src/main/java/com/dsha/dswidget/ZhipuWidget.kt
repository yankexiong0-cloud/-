package com.dsha.dswidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

/**
 * 智谱（BigModel）没有公开的余额查询 API，这里做成「入口卡片」：
 * 桌面显示一张卡，点一下直接在浏览器打开官方财务页。
 */
class ZhipuWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_OPEN) {
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(FINANCE_URL))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(view)
        }
    }

    companion object {
        const val ACTION_OPEN = "com.dsha.dswidget.ZHIPU_OPEN"
        const val FINANCE_URL = "https://bigmodel.cn/finance/overview"

        fun render(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ZhipuWidget::class.java))
            if (ids.isEmpty()) return

            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.widget_zhipu)

                val openIntent = Intent(context, ZhipuWidget::class.java).setAction(ACTION_OPEN)
                val pi = PendingIntent.getBroadcast(context, 20, openIntent, flags)
                views.setOnClickPendingIntent(R.id.zhipu_root, pi)

                mgr.updateAppWidget(id, views)
            }
        }
    }
}
