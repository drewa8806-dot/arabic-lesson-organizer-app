package com.fakerni.app

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AlarmActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // إظهار فوق شاشة القفل
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // فك القفل تلقائياً
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        }

        setContentView(R.layout.activity_alarm)

        // الحصول على بيانات الدرس
        val lessonName = intent.getStringExtra("LESSON_NAME") ?: "درس"
        val lessonDatetime = intent.getStringExtra("LESSON_DATETIME") ?: ""

        // عرض البيانات
        findViewById<TextView>(R.id.lessonNameText).text = lessonName
        findViewById<TextView>(R.id.lessonTimeText).text = formatDateTime(lessonDatetime)

        // زر الإغلاق
        findViewById<Button>(R.id.dismissButton).setOnClickListener {
            dismissAlarm()
        }

        // تشغيل الاهتزاز
        startVibration()
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // تكرار لا نهائي
        } else {
            val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
            vibrator.vibrate(pattern, 0) // تكرار لا نهائي
        }
    }

    private fun dismissAlarm() {
        // إيقاف الصوت
        AlarmReceiver.ringtone?.stop()
        
        // إيقاف الاهتزاز
        vibrator.cancel()
        
        // إغلاق النشاط
        finish()
    }

    private fun formatDateTime(datetime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val date = inputFormat.parse(datetime)
            
            val outputFormat = SimpleDateFormat("EEEE، d MMMM yyyy - hh:mm a", Locale("ar"))
            date?.let { outputFormat.format(it) } ?: ""
        } catch (e: Exception) {
            datetime
        }
    }

    override fun onBackPressed() {
        // منع إغلاق النشاط بزر الرجوع - يجب الضغط على الزر
    }

    override fun onDestroy() {
        super.onDestroy()
        vibrator.cancel()
        AlarmReceiver.ringtone?.stop()
    }
}