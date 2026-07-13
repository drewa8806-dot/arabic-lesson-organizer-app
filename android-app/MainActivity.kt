package com.fakerni.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var alarmScheduler: AlarmScheduler
    private val gson = Gson()

    // طلب الأذونات
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            println("${it.key} = ${it.value}")
        }
        // التحقق من باقي الإعدادات
        checkBatteryOptimization()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        alarmScheduler = AlarmScheduler(this)

        // إعداد WebView
        setupWebView()

        // بدء الخدمة الأمامية
        startForegroundService()

        // طلب جميع الأذونات
        requestAllPermissions()

        // تحميل التطبيق
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
        }

        // إضافة JavaScript Interface للتواصل مع الويب
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun startForegroundService() {
        val serviceIntent = Intent(this, FakerniService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()

        // إشعارات (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // تنبيه على الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        }

        // طلب الأذونات
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            checkBatteryOptimization()
        }

        // طلب إذن المنبه الدقيق (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("إذن المنبه الدقيق")
                    .setMessage("يحتاج التطبيق إلى إذن لضبط منبهات دقيقة لتذكيرك بدروسك في الوقت المحدد.")
                    .setPositiveButton("السماح") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("لاحقاً", null)
                    .show()
            }
        }

        // طلب إذن التنبيه على الشاشة
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                AlertDialog.Builder(this)
                    .setTitle("إذن العرض فوق التطبيقات")
                    .setMessage("يحتاج التطبيق إلى هذا الإذن لإظهار التنبيهات بشكل كامل.")
                    .setPositiveButton("السماح") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                    .setNegativeButton("لاحقاً", null)
                    .show()
            }
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("⚡ تحسين البطارية")
                    .setMessage("لضمان عمل التطبيق في الخلفية بشكل دائم، يرجى إيقاف تحسين البطارية لهذا التطبيق.\n\nهذا ضروري جداً لتذكيرك بدروسك!")
                    .setPositiveButton("الإعدادات") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            intent.data = Uri.parse("package:$packageName")
                            startActivity(intent)
                        } catch (e: Exception) {
                            // فتح إعدادات البطارية العامة
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("لاحقاً", null)
                    .setCancelable(false)
                    .show()
            } else {
                showAutoStartDialog()
            }
        }
    }

    private fun showAutoStartDialog() {
        AlertDialog.Builder(this)
            .setTitle("🚀 التشغيل التلقائي")
            .setMessage("للتأكد من عمل التطبيق بشكل دائم:\n\n1. افتح إعدادات الهاتف\n2. ابحث عن 'التشغيل التلقائي' أو 'Autostart'\n3. فعّل التطبيق 'فكرني'\n\nهذا مهم جداً على هواتف Xiaomi و Huawei و Oppo!")
            .setPositiveButton("فهمت") { _, _ ->
                showBackgroundRunDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun showBackgroundRunDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔒 قفل التطبيق في الخلفية")
            .setMessage("للتأكد من أن النظام لن يغلق التطبيق:\n\n1. افتح قائمة التطبيقات الأخيرة\n2. اسحب تطبيق 'فكرني' للأسفل\n3. اضغط على أيقونة القفل 🔒\n\nهذا يمنع النظام من إغلاق التطبيق!")
            .setPositiveButton("فهمت", null)
            .show()
    }

    // JavaScript Interface للتواصل مع WebView
    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun scheduleAlarm(lessonJson: String) {
            try {
                val type = object : TypeToken<Lesson>() {}.type
                val lesson: Lesson = gson.fromJson(lessonJson, type)
                alarmScheduler.scheduleLesson(lesson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @JavascriptInterface
        fun cancelAlarm(lessonId: String) {
            alarmScheduler.cancelLesson(lessonId)
        }

        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun requestPermissions() {
            runOnUiThread {
                requestAllPermissions()
            }
        }

        @JavascriptInterface
        fun isServiceRunning(): Boolean {
            return FakerniService.isRunning
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            // لا تغلق التطبيق، فقط اخفيه
            moveTaskToBack(true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // إعادة تشغيل الخدمة إذا تم إغلاق التطبيق
        val restartIntent = Intent(this, RestartReceiver::class.java)
        sendBroadcast(restartIntent)
    }
}

data class Lesson(
    val id: String,
    val name: String,
    val datetime: String,
    val notified: Boolean
)