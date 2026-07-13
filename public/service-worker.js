// Service Worker لتشغيل التطبيق في الخلفية
const CACHE_NAME = 'fakerni-v1';

// تثبيت Service Worker
self.addEventListener('install', (event) => {
  console.log('✅ Service Worker تم تثبيته');
  self.skipWaiting();
  
  // حفظ الملفات في الكاش للعمل بدون إنترنت
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(['/']);
    })
  );
});

// تفعيل Service Worker
self.addEventListener('activate', (event) => {
  console.log('✅ Service Worker تم تفعيله');
  event.waitUntil(clients.claim());
  
  // طلب استمرار العمل في الخلفية
  event.waitUntil(self.registration.navigationPreload?.enable());
});

// فحص الدروس كل دقيقة في الخلفية
let checkInterval = null;
let lessonsCache = [];

// بدء الفحص الدوري
function startPeriodicCheck() {
  if (checkInterval) return;
  
  checkInterval = setInterval(() => {
    console.log('🔍 فحص الدروس في الخلفية...');
    if (lessonsCache.length > 0) {
      checkLessonsInBackground(lessonsCache);
    }
  }, 30000); // كل 30 ثانية
}

// الاستماع للرسائل من التطبيق
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'CHECK_LESSONS') {
    lessonsCache = event.data.lessons || [];
    checkLessonsInBackground(lessonsCache);
    startPeriodicCheck();
  }
  
  if (event.data && event.data.type === 'KEEP_ALIVE') {
    // رد للحفاظ على Service Worker نشطاً
    event.ports[0]?.postMessage({ status: 'alive' });
  }
});

// فحص الدروس في الخلفية
function checkLessonsInBackground(lessons) {
  if (!lessons || lessons.length === 0) return;

  const now = new Date();
  
  lessons.forEach(lesson => {
    if (!lesson.notified) {
      const lessonDate = new Date(lesson.datetime);
      const timeDiff = lessonDate.getTime() - now.getTime();
      
      // إذا حان وقت الدرس
      if (timeDiff <= 30000 && timeDiff >= -30000) {
        showNotification(lesson);
      }
    }
  });
}

// عرض الإشعار
function showNotification(lesson) {
  const lessonDate = new Date(lesson.datetime);
  const formattedTime = lessonDate.toLocaleTimeString('ar-EG', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: true
  });
  
  const formattedDate = lessonDate.toLocaleDateString('ar-EG', {
    weekday: 'long',
    day: 'numeric',
    month: 'long'
  });

  const options = {
    body: `هذا هو درس ${lesson.name}\n📅 ${formattedDate}\n🕐 ${formattedTime}\n\nهل فهمت؟`,
    icon: '/icon-192.png',
    badge: '/icon-192.png',
    tag: lesson.id,
    requireInteraction: true,
    vibrate: [300, 100, 300, 100, 300],
    data: {
      lesson: lesson,
      url: '/'
    },
    actions: [
      { action: 'open', title: 'فتح التطبيق' },
      { action: 'dismiss', title: 'فهمت' }
    ]
  };

  self.registration.showNotification('🔔 فكرني - تذكير بالدرس', options);
  
  // تشغيل صوت (إذا كان متاحاً)
  clients.matchAll({ includeUncontrolled: true, type: 'window' }).then(clients => {
    clients.forEach(client => {
      client.postMessage({
        type: 'PLAY_ALARM',
        lesson: lesson
      });
    });
  });
}

// عند النقر على الإشعار
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  
  if (event.action === 'open' || !event.action) {
    event.waitUntil(
      clients.matchAll({ type: 'window', includeUncontrolled: true }).then(clientList => {
        // إذا كان التطبيق مفتوحاً، ركز عليه
        for (let client of clientList) {
          if (client.url === '/' && 'focus' in client) {
            return client.focus();
          }
        }
        // وإلا افتح نافذة جديدة
        if (clients.openWindow) {
          return clients.openWindow('/');
        }
      })
    );
  }
});

// Periodic Background Sync (إذا كان مدعوماً)
self.addEventListener('periodicsync', (event) => {
  if (event.tag === 'check-lessons') {
    event.waitUntil(checkPeriodicLessons());
  }
});

async function checkPeriodicLessons() {
  try {
    // قراءة الدروس من IndexedDB أو من cache
    const lessons = await getLessonsFromStorage();
    if (lessons) {
      checkLessonsInBackground(lessons);
    }
  } catch (error) {
    console.error('خطأ في Periodic Sync:', error);
  }
}

async function getLessonsFromStorage() {
  // محاولة قراءة من localStorage عبر الـ clients
  const clients = await self.clients.matchAll();
  if (clients.length > 0) {
    clients[0].postMessage({ type: 'GET_LESSONS' });
  }
  return null;
}