# 🔔 فكرني - لن أنسى أبداً

تطبيق ذكي لتذكيرك بدروسك ومواعيدك

---

## ⚠️ **ملاحظة مهمة جداً**

### 🌐 **هذا تطبيق ويب (Web App)** وليس تطبيق Android أصلي

**لا يمكن لتطبيق ويب استخدام:**
- ❌ `AlarmManager.setExactAndAllowWhileIdle()`
- ❌ `Foreground Service`
- ❌ `BroadcastReceiver`
- ❌ `SCHEDULE_EXACT_ALARM` permission

**هذه مكونات Android Native فقط!**

---

## ✅ **ما يمكن لتطبيق الويب فعله:**

### 1. **Service Worker** ✅
- يعمل في الخلفية **طالما المتصفح مفتوح**
- يرسل إشعارات من الخلفية
- يعمل حتى لو أغلقت التبويب

### 2. **Web Notifications API** ✅
- إشعارات النظام
- صوت + اهتزاز (على المتصفحات المدعومة)

### 3. **PWA (Progressive Web App)** ✅
- تثبيت على الشاشة الرئيسية
- يعمل بدون إنترنت
- أيقونات مخصصة

### 4. **Web Audio API** ✅
- صوت منبه مدمج في التطبيق

---

## 🚫 **القيود:**

### **على Chrome/Edge:**
- ✅ Service Worker يعمل في الخلفية
- ✅ الإشعارات تعمل
- ⚠️ **لكن المتصفح يجب أن يكون مفتوحاً** (يمكن تصغيره)

### **على Safari:**
- ⚠️ دعم محدود جداً للـ Service Worker
- ⚠️ الإشعارات لا تعمل في الخلفية

### **على الهاتف:**
- ⚠️ إذا أغلقت المتصفح تماماً، التطبيق لن يعمل
- ⚠️ Android قد يقتل المتصفح لتوفير البطارية

---

## 💡 **الحل المثالي:**

### **تحويل التطبيق إلى Android Native**

راجع ملف [`android-guide.md`](./android-guide.md) للحصول على:
- ✅ كود كامل بـ Kotlin
- ✅ استخدام `AlarmManager.setExactAndAllowWhileIdle()`
- ✅ `Foreground Service` للعمل الدائم
- ✅ `BroadcastReceiver` للمنبهات
- ✅ يعمل حتى لو أغلقت التطبيق تماماً
- ✅ موفر للبطارية

---

## 🎯 **كيفية استخدام النسخة الحالية (Web):**

1. **افتح التطبيق في Chrome/Edge**
2. **اضغط "تفعيل الإشعارات"** واسمح بها
3. **اضغط "تثبيت التطبيق"** (إذا ظهر الزر)
4. **أضف دروسك**
5. **اترك المتصفح مفتوحاً** (يمكن تصغيره)
6. **على الهاتف:** ثبّت التطبيق كـ PWA للحصول على أفضل أداء

---

## 🔨 **للمطورين:**

### **تحويل لـ Android Native:**
```bash
# استخدم Android Studio
# راجع android-guide.md
```

### **تحويل لـ React Native:**
```bash
npx react-native init Fakerni
# استخدم react-native-push-notification
# استخدم @notifee/react-native
```

### **تحويل لـ Flutter:**
```bash
flutter create fakerni
# استخدم android_alarm_manager_plus
# استخدم flutter_local_notifications
```

---

## 📱 **مقارنة الخيارات:**

| الميزة | Web App | Android Native |
|--------|---------|----------------|
| AlarmManager | ❌ | ✅ |
| Foreground Service | ❌ | ✅ |
| يعمل بعد إغلاق التطبيق | ❌ | ✅ |
| سهولة التطوير | ✅ سهل | ⚠️ متوسط |
| متعدد المنصات | ✅ | ❌ |
| توفير البطارية | ⚠️ | ✅ |

---

## ✨ **تم الإنشاء بواسطة Youssef Mahmoud**

🔔 فكرني - لن أنسى أبداً 💜
