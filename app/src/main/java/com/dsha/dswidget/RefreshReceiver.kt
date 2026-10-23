package com.dsha.dswidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RefreshReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ALARM -> BalanceWidget.refreshAsync(context.applicationContext)
            Intent.ACTION_BOOT_COMPLETED -> {
                RefreshScheduler.schedule(context.applicationContext)
                BalanceWidget.refreshAsync(context.applicationContext)
            }
        }
    }

    companion object {
        const val ACTION_ALARM = "com.dsha.dswidget.ALARM_REFRESH"
    }
}
