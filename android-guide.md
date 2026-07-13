# دليل تحويل "فكرني" لتطبيق Android أصلي

## 📱 ما تحتاجه:

### 1. **استخدام Android Studio وKotlin/Java**
تطبيق الويب الحالي لا يمكنه استخدام:
- ❌ `AlarmManager.setExactAndAllowWhileIdle()`
- ❌ `Foreground Service`
- ❌ `BroadcastReceiver`
- ❌ `SCHEDULE_EXACT_ALARM` permission

هذه كلها مكونات Android Native فقط.

---

## ✅ الحل المقترح: تطبيق Android أصلي

### **الكود المطلوب (Kotlin):**

#### 1️⃣ **AndroidManifest.xml**
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- الأذونات المطلوبة -->
    <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.USE_EXACT_ALARM" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application>
        
        <!-- BroadcastReceiver للمنبه -->
        <receiver 
            android:name=".AlarmReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <action android:name="com.fakerni.LESSON_ALARM" />
            </intent-filter>
        </receiver>

        <!-- BroadcastReceiver للتشغيل عند إعادة تشغيل الجهاز -->
        <receiver 
            android:name=".BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

        <!-- Foreground Service (اختياري) -->
        <service
            android:name=".LessonReminderService"
            android:foregroundServiceType="dataSync"
            android:enabled="true"
            android:exported="false" />

    </application>
</manifest>
```

---

#### 2️⃣ **AlarmScheduler.kt** - جدولة المنبه
```kotlin
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    fun scheduleLesson(lessonId: String, lessonName: String, triggerTime: Long) {
        
        // التحقق من الإذن في Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // طلب الإذن من المستخدم
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
                return
            }
        }
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.fakerni.LESSON_ALARM"
            putExtra("LESSON_ID", lessonId)
            putExtra("LESSON_NAME", lessonName)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lessonId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // استخدام setExactAndAllowWhileIdle للدقة العالية حتى في Doze Mode
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
        
        println("✅ تم جدولة الدرس: $lessonName في ${Date(triggerTime)}")
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
        println("❌ تم إلغاء المنبه للدرس: $lessonId")
    }
}
```

---

#### 3️⃣ **AlarmReceiver.kt** - استقبال المنبه
```kotlin
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.PowerManager

class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        
        val lessonId = intent.getStringExtra("LESSON_ID") ?: return
        val lessonName = intent.getStringExtra("LESSON_NAME") ?: "درس"
        
        // إيقاظ الجهاز
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or 
            PowerManager.ACQUIRE_CAUSES_WAKEUP or 
            PowerManager.ON_AFTER_RELEASE,
            "Fakerni::AlarmWakeLock"
        )
        wakeLock.acquire(60 * 1000L) // دقيقة واحدة
        
        // تشغيل الصوت
        playAlarmSound(context)
        
        // إظهار الإشعار
        showNotification(context, lessonId, lessonName)
        
        // فتح نشاط التنبيه
        showAlarmActivity(context, lessonId, lessonName)
        
        wakeLock.release()
    }
    
    private fun playAlarmSound(context: Context) {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, notification)
        ringtone.play()
    }
    
    private fun showNotification(context: Context, lessonId: String, lessonName: String) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showLessonNotification(lessonId, lessonName)
    }
    
    private fun showAlarmActivity(context: Context, lessonId: String, lessonName: String) {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("LESSON_ID", lessonId)
            putExtra("LESSON_NAME", lessonName)
        }
        context.startActivity(intent)
    }
}
```

---

#### 4️⃣ **NotificationHelper.kt** - الإشعارات
```kotlin
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {
    
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "lesson_reminders"
        const val CHANNEL_NAME = "تذكير الدروس"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات تذكير الدروس"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    null
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showLessonNotification(lessonId: String, lessonName: String) {
        
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🔔 فكرني - تذكير بالدرس")
            .setContentText("هذا هو درس $lessonName - هل فهمت؟")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()
        
        notificationManager.notify(lessonId.hashCode(), notification)
    }
}
```

---

#### 5️⃣ **BootReceiver.kt** - إعادة جدولة بعد إعادة التشغيل
```kotlin
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // إعادة جدولة جميع الدروس من قاعدة البيانات
            rescheduleAllLessons(context)
        }
    }
    
    private fun rescheduleAllLessons(context: Context) {
        // قراءة الدروس من SharedPreferences أو Room Database
        // ثم إعادة جدولتها
        val scheduler = AlarmScheduler(context)
        // scheduler.scheduleLesson(...)
    }
}
```

---

#### 6️⃣ **LessonReminderService.kt** - Foreground Service (اختياري)
```kotlin
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LessonReminderService : Service() {
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setContentTitle("فكرني يعمل في الخلفية")
            .setContentText("سنذكرك بدروسك في الوقت المحدد")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(1, notification)
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
```

---

## 🎯 **المميزات:**

✅ **AlarmManager.setExactAndAllowWhileIdle()** - دقة عالية حتى في Doze Mode
✅ **BroadcastReceiver** - استقبال المنبه
✅ **Foreground Service** - يعمل باستمرار (اختياري)
✅ **BOOT_COMPLETED** - إعادة الجدولة بعد إعادة تشغيل الجهاز
✅ **Wake Lock** - إيقاظ الجهاز
✅ **Full Screen Intent** - فتح التطبيق فوراً
✅ **Exact Alarm Permission** - Android 12+

---

## 📋 **الخطوات للتنفيذ:**

1. إنشاء مشروع Android جديد في Android Studio
2. نسخ الأكواد أعلاه
3. إضافة الأذونات في AndroidManifest.xml
4. طلب الإذن `SCHEDULE_EXACT_ALARM` من المستخدم
5. حفظ الدروس في Room Database أو SharedPreferences
6. جدولة المنبهات عند إضافة الدروس

---

## ⚠️ **ملاحظة:**

تطبيق الويب الحالي **لا يمكنه** الوصول لهذه المميزات.
**الحل الوحيد** هو تحويله لتطبيق Android أصلي باستخدام:
- Kotlin/Java Native
- أو React Native
- أو Flutter

هل تريدني أن أنشئ لك تطبيق Android أصلي كامل؟
