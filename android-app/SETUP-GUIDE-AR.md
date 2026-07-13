# 📱 دليل إنشاء تطبيق فكرني - Android

## 🎯 التطبيق سيعمل للأبد في الخلفية!

---

## 📋 الخطوات:

### **1️⃣ إنشاء مشروع Android Studio**

```
1. افتح Android Studio
2. File → New → New Project
3. اختر "Empty Activity"
4. Name: Fakerni
5. Package: com.fakerni.app
6. Language: Kotlin
7. Minimum SDK: API 21 (Android 5.0)
8. اضغط Finish
```

---

### **2️⃣ نسخ الملفات**

#### **A. AndroidManifest.xml**
انسخ محتوى `AndroidManifest.xml` إلى:
```
app/src/main/AndroidManifest.xml
```

#### **B. ملفات Kotlin**
انسخ جميع ملفات `.kt` إلى:
```
app/src/main/java/com/fakerni/app/
```

الملفات المطلوبة:
- ✅ MainActivity.kt
- ✅ FakerniService.kt
- ✅ FakerniApplication.kt
- ✅ AlarmScheduler.kt
- ✅ AlarmReceiver.kt
- ✅ AlarmActivity.kt
- ✅ NotificationHelper.kt
- ✅ BootReceiver.kt
- ✅ RestartReceiver.kt

#### **C. ملفات XML**
انسخ ملفات الـ Layout إلى:
```
app/src/main/res/layout/
```

الملفات:
- ✅ activity_main.xml
- ✅ activity_alarm.xml

#### **D. build.gradle**
انسخ محتوى `build.gradle` إلى:
```
app/build.gradle
```

---

### **3️⃣ إنشاء ملفات Drawable**

#### **A. alarm_background.xml**
في `app/src/main/res/drawable/alarm_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:angle="135"
        android:startColor="#FF5722"
        android:centerColor="#FF9800"
        android:endColor="#FFC107"
        android:type="linear" />
</shape>
```

#### **B. card_background.xml**
في `app/src/main/res/drawable/card_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#FFFFFF" />
    <corners android:radius="16dp" />
    <stroke android:width="2dp" android:color="#E0E0E0" />
</shape>
```

#### **C. button_background.xml**
في `app/src/main/res/drawable/button_background.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#4CAF50" />
    <corners android:radius="12dp" />
</shape>
```

---

### **4️⃣ إنشاء ملفات Themes**

#### **themes.xml**
في `app/src/main/res/values/themes.xml`:

```xml
<resources>
    <style name="Theme.Fakerni" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="colorPrimary">#8B5CF6</item>
        <item name="colorPrimaryVariant">#7C3AED</item>
        <item name="colorOnPrimary">#FFFFFF</item>
    </style>

    <style name="Theme.Fakerni.Alarm" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <item name="android:windowBackground">@drawable/alarm_background</item>
        <item name="android:windowIsTranslucent">false</item>
        <item name="android:windowShowWallpaper">false</item>
    </style>
</resources>
```

---

### **5️⃣ نسخ تطبيق الويب**

انسخ ملف `dist/index.html` من تطبيق الويب إلى:
```
app/src/main/assets/index.html
```

**ملاحظة مهمة:** يجب بناء تطبيق الويب أولاً:
```bash
npm run build
```

ثم انسخ `dist/index.html` إلى مجلد `assets`.

---

### **6️⃣ Sync و Build**

```
1. اضغط "Sync Now" في الأعلى
2. اضغط Build → Make Project
3. انتظر حتى ينتهي البناء
```

---

### **7️⃣ تشغيل التطبيق**

```
1. وصّل هاتف Android (أو استخدم Emulator)
2. اضغط Run ▶️
3. انتظر التثبيت
```

---

## ⚙️ **الأذونات التي سيطلبها التطبيق:**

### **عند أول تشغيل:**

1. ✅ **إذن الإشعارات** - لإرسال التنبيهات
2. ✅ **إذن المنبه الدقيق** - لضبط مواعيد دقيقة
3. ✅ **إذن العرض فوق التطبيقات** - لإظهار المنبه فوق شاشة القفل
4. ✅ **إيقاف تحسين البطارية** - للعمل في الخلفية
5. ✅ **التشغيل التلقائي** - للعمل بعد إعادة التشغيل

---

## 🔧 **إعدادات إضافية على الهاتف:**

### **على هواتف Xiaomi (MIUI):**
```
1. الإعدادات → التطبيقات → فكرني
2. التشغيل التلقائي → تفعيل
3. توفير الطاقة → بدون قيود
4. الأذونات → السماح بكل شيء
5. قفل التطبيق في قائمة المهام الأخيرة
```

### **على هواتف Huawei:**
```
1. الإعدادات → البطارية → تطبيقات محمية
2. تفعيل "فكرني"
3. مدير التطبيقات → التشغيل → السماح بالتشغيل في الخلفية
```

### **على هواتف Samsung:**
```
1. الإعدادات → العناية بالجهاز → البطارية
2. استخدام الطاقة للتطبيقات → فكرني → غير محدود
3. الإعدادات → التطبيقات → فكرني → البطارية → غير مُحسّن
```

### **على هواتف Oppo/Realme:**
```
1. الإعدادات → البطارية → التطبيقات عالية الاستهلاك
2. تعطيل التحسين لـ "فكرني"
3. الإعدادات → مدير الخصوصية → مدير التشغيل التلقائي
4. تفعيل "فكرني"
```

---

## 🎯 **المميزات:**

### ✅ **يعمل للأبد في الخلفية**
- Foreground Service دائم
- Wake Lock للحفاظ على النشاط
- إعادة تشغيل تلقائية عند الإغلاق

### ✅ **AlarmManager.setExactAndAllowWhileIdle()**
- دقة عالية جداً
- يعمل في Doze Mode
- يعمل في App Standby

### ✅ **إعادة التشغيل بعد Reboot**
- BootReceiver يعيد جدولة كل شيء
- الخدمة تبدأ تلقائياً

### ✅ **إعادة التشغيل عند الإغلاق**
- إذا حذفت من المهام الأخيرة → يعيد التشغيل
- إذا أوقف النظام الخدمة → تعيد نفسها

### ✅ **منبه ملء الشاشة**
- يفتح فوق شاشة القفل
- صوت + اهتزاز
- لا يمكن إغلاقه بزر الرجوع

---

## 📊 **كيف يعمل:**

```
1. التطبيق يحتوي على WebView
   ↓
2. WebView يشغل تطبيق الويب (HTML/JS)
   ↓
3. JavaScript يتواصل مع Kotlin عبر JavascriptInterface
   ↓
4. عند إضافة درس، JS يرسل للـ Kotlin
   ↓
5. Kotlin يجدول المنبه عبر AlarmManager
   ↓
6. عند حلول الموعد:
   - AlarmReceiver يستقبل التنبيه
   - يشغل صوت المنبه
   - يفتح AlarmActivity بملء الشاشة
   - يرسل Notification
```

---

## 🔍 **حل المشاكل:**

### **المشكلة: التطبيق يتوقف في الخلفية**
**الحل:**
- تأكد من إيقاف تحسين البطارية
- فعّل التشغيل التلقائي
- اقفل التطبيق في قائمة المهام

### **المشكلة: المنبه لا يعمل**
**الحل:**
- تأكد من إذن SCHEDULE_EXACT_ALARM
- الإعدادات → التطبيقات → الأذونات الخاصة

### **المشكلة: لا يظهر بملء الشاشة**
**الحل:**
- تأكد من إذن "العرض فوق التطبيقات"
- Settings.ACTION_MANAGE_OVERLAY_PERMISSION

---

## 📱 **اختبار التطبيق:**

### **1. اختبار فوري:**
```kotlin
// أضف في MainActivity onCreate:
val testLesson = Lesson(
    id = "test1",
    name = "اختبار",
    datetime = "2024-01-15T15:30",
    notified = false
)
alarmScheduler.scheduleLesson(testLesson)
```

### **2. اختبار بعد 30 ثانية:**
غيّر الوقت في `AlarmScheduler.kt`:
```kotlin
val triggerTime = System.currentTimeMillis() + 30000 // 30 ثانية
```

---

## 🚀 **نشر التطبيق:**

### **إنشاء APK:**
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

### **الملف سيكون في:**
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## ✅ **الخلاصة:**

هذا التطبيق:
- ✅ يستخدم AlarmManager.setExactAndAllowWhileIdle()
- ✅ يستخدم Foreground Service
- ✅ يستخدم BroadcastReceiver
- ✅ يعمل للأبد في الخلفية
- ✅ يعيد نفسه عند الإغلاق
- ✅ يعمل بعد إعادة تشغيل الجهاز
- ✅ منبه ملء الشاشة
- ✅ صوت واهتزاز قوي

**بالضبط كما طلبت! 🎉**
