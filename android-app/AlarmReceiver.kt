package com.fakerni.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        var ringtone: Ringtone? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val lessonId = intent.getStringExtra("LESSON_ID") ?: return
        val lessonName = intent.getStringExtra("LESSON_NAME") ?: "درس"
        val lessonDatetime = intent.getStringExtra("LESSON_DATETIME") ?: ""

        println("🔔 استلام منبه الدرس: $lessonName")

        // إيقاظ الجهاز
        wakeUpDevice(context)

        // تشغيل صوت المنبه
        playAlarmSound(context)

        // إظهار نشاط المنبه بشكل كامل
        showFullScreenAlarm(context, lessonId, lessonName, lessonDatetime)

        // إرسال إشعار أيضاً
        sendNotification(context, lessonId, lessonName, lessonDatetime)
    }

    private fun wakeUpDevice(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "Fakerni::AlarmWakeLock"
        )
        wakeLock.acquire(60 * 1000L) // دقيقة واحدة
    }

    private fun playAlarmSound(context: Context) {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            ringtone = RingtoneManager.getRingtone(context, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showFullScreenAlarm(
        context: Context,
        lessonId: String,
        lessonName: String,
        lessonDatetime: String
    ) {
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                flags = flags or Intent.FLAG_ACTIVITY_SHOW_WHEN_LOCKED or
                        Intent.FLAG_ACTIVITY_TURN_SCREEN_ON
            }
            
            putExtra("LESSON_ID", lessonId)
            putExtra("LESSON_NAME", lessonName)
            putExtra("LESSON_DATETIME", lessonDatetime)
        }
        
        context.startActivity(alarmIntent)
    }

    private fun sendNotification(
        context: Context,
        lessonId: String,
        lessonName: String,
        lessonDatetime: String
    ) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showLessonNotification(lessonId, lessonName, lessonDatetime)
    }
}