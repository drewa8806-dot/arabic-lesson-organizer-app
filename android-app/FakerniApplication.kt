package com.fakerni.app

import android.app.Application
import android.content.Intent
import android.os.Build

class FakerniApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // بدء الخدمة عند تشغيل التطبيق
        startFakerniService()
    }

    private fun startFakerniService() {
        val serviceIntent = Intent(this, FakerniService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}