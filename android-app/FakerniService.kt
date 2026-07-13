package com.fakerni.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FakerniService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "fakerni_service_channel"
        const val NOTIFICATION_ID = 1
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, createNotification())
        startBackgroundWork()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY // سيعيد النظام تشغيل الخدمة إذا أوقفها
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "خدمة فكرني",
                NotificationManager.IMPORTANCE_LOW // منخفض حتى لا يزعج المستخدم
            ).apply {
                description = "يعمل في الخلفية لتذكيرك بدروسك"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔔 فكرني يعمل في الخلفية")
            .setContentText("سنذكرك بدروسك في الوقت المحدد")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // لا يمكن إزالته بالسحب
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Fakerni::ServiceWakeLock"
        ).apply {
            acquire(10*60*60*1000L /*10 hours*/) // وقت طويل جداً
        }
    }

    private fun startBackgroundWork() {
        scope.launch {
            while (isActive) {
                // فحص الدروس كل 30 ثانية
                checkLessons()
                delay(30000) // 30 ثانية
            }
        }
    }

    private fun checkLessons() {
        // قراءة الدروس من SharedPreferences
        val prefs = getSharedPreferences("fakerni_prefs", Context.MODE_PRIVATE)
        val lessonsJson = prefs.getString("lessons", "[]")
        
        // يمكنك إضافة منطق الفحص هنا
        // وإرسال إشعار إذا حان وقت درس
        
        println("✅ فحص الدروس في الخلفية...")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        wakeLock?.release()
        
        // إعادة تشغيل الخدمة
        val restartIntent = Intent(this, RestartReceiver::class.java)
        sendBroadcast(restartIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // إعادة تشغيل الخدمة عند حذف التطبيق من المهام الأخيرة
        val restartServiceIntent = Intent(applicationContext, FakerniService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            1000, // إعادة التشغيل بعد ثانية
            restartServicePendingIntent
        )
    }
}