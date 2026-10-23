package com.dsha.dswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object RefreshScheduler {

    private const val INTERVAL_MS = 15 * 60 * 1000L

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, RefreshReceiver::class.java).setAction(RefreshReceiver.ACTION_ALARM)
        return PendingIntent.getBroadcast(
            context, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            INTERVAL_MS,
            pending(context)
        )
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context))
    }
}
