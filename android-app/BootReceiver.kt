package com.fakerni.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                
                println("📱 الجهاز تم تشغيله - إعادة تشغيل خدمة فكرني")
                
                // إعادة تشغيل الخدمة الأمامية
                val serviceIntent = Intent(context, FakerniService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                // إعادة جدولة جميع المنبهات
                val alarmScheduler = AlarmScheduler(context)
                alarmScheduler.rescheduleAllLessons()
            }
        }
    }
}