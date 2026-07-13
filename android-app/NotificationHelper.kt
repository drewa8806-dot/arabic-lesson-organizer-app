package com.fakerni.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val LESSON_CHANNEL_ID = "lesson_alarms"
        const val LESSON_CHANNEL_NAME = "منبهات الدروس"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // قناة منبهات الدروس
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val lessonChannel = NotificationChannel(
                LESSON_CHANNEL_ID,
                LESSON_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات تذكير بمواعيد الدروس"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(alarmSound, audioAttributes)
                enableLights(true)
                setBypassDnd(true) // تجاوز وضع عدم الإزعاج
            }

            notificationManager.createNotificationChannel(lessonChannel)
        }
    }

    fun showLessonNotification(lessonId: String, lessonName: String, lessonDatetime: String) {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("LESSON_ID", lessonId)
            putExtra("LESSON_NAME", lessonName)
            putExtra("LESSON_DATETIME", lessonDatetime)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            lessonId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            lessonId.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(context, LESSON_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🔔 فكرني - تذكير بالدرس")
            .setContentText("هذا هو درس $lessonName")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("هذا هو درس $lessonName\n\nهل فهمت؟")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenIntent, true) // فتح بملء الشاشة
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(alarmSound)
            .setOngoing(false)
            .setTimeoutAfter(60000) // إلغاء بعد دقيقة
            .build()

        notificationManager.notify(lessonId.hashCode(), notification)
    }
}