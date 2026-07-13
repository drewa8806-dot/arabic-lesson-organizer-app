package com.fakerni.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleLesson(lesson: Lesson) {
        try {
            val triggerTime = parseDateTime(lesson.datetime)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.fakerni.app.LESSON_ALARM"
                putExtra("LESSON_ID", lesson.id)
                putExtra("LESSON_NAME", lesson.name)
                putExtra("LESSON_DATETIME", lesson.datetime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                lesson.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // استخدام setExactAndAllowWhileIdle للدقة العالية
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            // حفظ في SharedPreferences للإعادة بعد إعادة التشغيل
            saveLesson(lesson)

            println("✅ تم جدولة الدرس: ${lesson.name} في ${Date(triggerTime)}")

        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ خطأ في جدولة الدرس: ${e.message}")
        }
    }

    fun cancelLesson(lessonId: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lessonId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // حذف من SharedPreferences
        removeLesson(lessonId)
        
        println("❌ تم إلغاء المنبه للدرس: $lessonId")
    }

    private fun parseDateTime(datetime: String): Long {
        return try {
            // تحويل من ISO 8601 format
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val date = format.parse(datetime)
            date?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
            System.currentTimeMillis()
        }
    }

    private fun saveLesson(lesson: Lesson) {
        val prefs = context.getSharedPreferences("fakerni_prefs", Context.MODE_PRIVATE)
        val lessonsJson = prefs.getString("lessons", "[]")
        
        // يمكنك استخدام Gson هنا لحفظ الدروس
        // مؤقتاً نحفظ فقط للإشارة
        prefs.edit().putString("lesson_${lesson.id}", lesson.toString()).apply()
    }

    private fun removeLesson(lessonId: String) {
        val prefs = context.getSharedPreferences("fakerni_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("lesson_$lessonId").apply()
    }

    fun rescheduleAllLessons() {
        // قراءة جميع الدروس من SharedPreferences وإعادة جدولتها
        val prefs = context.getSharedPreferences("fakerni_prefs", Context.MODE_PRIVATE)
        // يمكنك إضافة منطق إعادة الجدولة هنا
    }
}