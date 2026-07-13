import { useState, useEffect, useRef } from 'react';

interface Lesson {
  id: string;
  name: string;
  datetime: string;
  notified: boolean;
}

function App() {
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [lessonName, setLessonName] = useState('');
  const [selectedDatetime, setSelectedDatetime] = useState('');
  const [notificationPermission, setNotificationPermission] = useState<NotificationPermission>('default');
  const [showAlarmModal, setShowAlarmModal] = useState(false);
  const [currentAlarm, setCurrentAlarm] = useState<Lesson | null>(null);
  const [serviceWorkerReady, setServiceWorkerReady] = useState(false);
  const [showInstallPrompt, setShowInstallPrompt] = useState(false);
  const checkIntervalRef = useRef<number | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const isPlayingRef = useRef(false);
  const swRegistrationRef = useRef<ServiceWorkerRegistration | null>(null);
  const deferredPromptRef = useRef<any>(null);

  // تسجيل Service Worker
  useEffect(() => {
    registerServiceWorker();
    setupInstallPrompt();
  }, []);

  const registerServiceWorker = async () => {
    if ('serviceWorker' in navigator) {
      try {
        const registration = await navigator.serviceWorker.register('/service-worker.js', {
          scope: '/'
        });
        
        swRegistrationRef.current = registration;
        setServiceWorkerReady(true);
        console.log('✅ Service Worker تم تسجيله بنجاح');

        // الاستماع للرسائل من Service Worker
        navigator.serviceWorker.addEventListener('message', handleServiceWorkerMessage);

        // طلب Background Sync إذا كان مدعوماً
        if ('sync' in registration) {
          console.log('✅ Background Sync مدعوم');
        }

        // طلب Periodic Background Sync إذا كان مدعوماً
        if ('periodicSync' in registration) {
          try {
            await (registration as any).periodicSync.register('check-lessons', {
              minInterval: 60 * 1000 // كل دقيقة
            });
            console.log('✅ Periodic Background Sync تم تفعيله');
          } catch (error) {
            console.log('⚠️ Periodic Background Sync غير متاح:', error);
          }
        }

      } catch (error) {
        console.error('❌ فشل تسجيل Service Worker:', error);
      }
    } else {
      console.log('⚠️ Service Worker غير مدعوم في هذا المتصفح');
    }
  };

  const setupInstallPrompt = () => {
    window.addEventListener('beforeinstallprompt', (e) => {
      e.preventDefault();
      deferredPromptRef.current = e;
      setShowInstallPrompt(true);
    });

    window.addEventListener('appinstalled', () => {
      console.log('✅ تم تثبيت التطبيق');
      setShowInstallPrompt(false);
    });
  };

  const installApp = async () => {
    if (deferredPromptRef.current) {
      deferredPromptRef.current.prompt();
      const { outcome } = await deferredPromptRef.current.userChoice;
      console.log(`نتيجة التثبيت: ${outcome}`);
      deferredPromptRef.current = null;
      setShowInstallPrompt(false);
    }
  };

  const handleServiceWorkerMessage = (event: MessageEvent) => {
    if (event.data && event.data.type === 'PLAY_ALARM') {
      const lesson = event.data.lesson;
      setCurrentAlarm(lesson);
      setShowAlarmModal(true);
      playAlarmSound();
    }
  };

  // تحميل الدروس
  useEffect(() => {
    const savedLessons = localStorage.getItem('lessons');
    if (savedLessons) {
      try {
        setLessons(JSON.parse(savedLessons));
      } catch (e) {
        console.error('خطأ في تحميل الدروس:', e);
      }
    }
    
    // طلب إذن الإشعارات
    requestNotificationPermission();

    // تهيئة AudioContext
    audioContextRef.current = new (window.AudioContext || (window as any).webkitAudioContext)();
  }, []);

  const requestNotificationPermission = async () => {
    if ('Notification' in window) {
      if (Notification.permission === 'default') {
        const permission = await Notification.requestPermission();
        setNotificationPermission(permission);
        if (permission === 'granted') {
          console.log('✅ تم منح إذن الإشعارات');
        }
      } else {
        setNotificationPermission(Notification.permission);
      }
    }
  };

  // حفظ الدروس وإرسالها للـ Service Worker
  useEffect(() => {
    if (lessons.length > 0) {
      localStorage.setItem('lessons', JSON.stringify(lessons));
      
      // إرسال الدروس للـ Service Worker
      if (swRegistrationRef.current?.active) {
        swRegistrationRef.current.active.postMessage({
          type: 'CHECK_LESSONS',
          lessons: lessons
        });
      }
    } else {
      localStorage.removeItem('lessons');
    }
  }, [lessons]);

  // فحص الدروس محلياً كل 10 ثواني (عندما يكون التطبيق مفتوحاً)
  useEffect(() => {
    const checkLessons = () => {
      const now = new Date();
      
      lessons.forEach(lesson => {
        if (!lesson.notified) {
          const lessonDate = new Date(lesson.datetime);
          const timeDiff = lessonDate.getTime() - now.getTime();
          
          if (timeDiff <= 30000 && timeDiff >= -30000) {
            console.log('🚨 وقت التنبيه! درس:', lesson.name);
            triggerAlarm(lesson);
          }
        }
      });

      // إرسال للـ Service Worker أيضاً
      if (swRegistrationRef.current?.active) {
        swRegistrationRef.current.active.postMessage({
          type: 'CHECK_LESSONS',
          lessons: lessons
        });
      }
    };

    checkLessons();
    checkIntervalRef.current = window.setInterval(checkLessons, 10000);

    return () => {
      if (checkIntervalRef.current) {
        clearInterval(checkIntervalRef.current);
      }
    };
  }, [lessons]);

  const triggerAlarm = (lesson: Lesson) => {
    setLessons(prev => 
      prev.map(l => l.id === lesson.id ? { ...l, notified: true } : l)
    );

    setCurrentAlarm(lesson);
    setShowAlarmModal(true);
    playAlarmSound();

    // إرسال إشعار عبر Service Worker للعمل في الخلفية
    if (swRegistrationRef.current && notificationPermission === 'granted') {
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

      swRegistrationRef.current.showNotification('🔔 فكرني - تذكير بالدرس', {
        body: `هذا هو درس ${lesson.name}\n📅 ${formattedDate}\n🕐 ${formattedTime}\n\nهل فهمت؟`,
        icon: '/icon-192.png',
        badge: '/icon-192.png',
        tag: lesson.id,
        requireInteraction: true,
        data: { lesson }
      });
    }
  };

  const playAlarmSound = () => {
    if (isPlayingRef.current) return;
    isPlayingRef.current = true;

    const playBeep = (frequency: number, duration: number, delay: number) => {
      setTimeout(() => {
        if (!audioContextRef.current) return;
        
        const oscillator = audioContextRef.current.createOscillator();
        const gainNode = audioContextRef.current.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContextRef.current.destination);
        
        oscillator.frequency.value = frequency;
        oscillator.type = 'sine';
        
        gainNode.gain.setValueAtTime(0.3, audioContextRef.current.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContextRef.current.currentTime + duration);
        
        oscillator.start(audioContextRef.current.currentTime);
        oscillator.stop(audioContextRef.current.currentTime + duration);
      }, delay);
    };

    for (let i = 0; i < 15; i++) {
      playBeep(800, 0.2, i * 400);
      playBeep(1000, 0.2, i * 400 + 200);
    }

    setTimeout(() => {
      isPlayingRef.current = false;
    }, 6000);
  };

  const dismissAlarm = () => {
    setShowAlarmModal(false);
    setCurrentAlarm(null);
    stopAlarmSound();
  };

  const stopAlarmSound = () => {
    isPlayingRef.current = false;
    if (audioContextRef.current) {
      audioContextRef.current.close().then(() => {
        audioContextRef.current = new (window.AudioContext || (window as any).webkitAudioContext)();
      });
    }
  };

  const addLesson = () => {
    if (!lessonName || !selectedDatetime) {
      alert('⚠️ الرجاء إدخال اسم الدرس والتاريخ والوقت');
      return;
    }

    const selectedDate = new Date(selectedDatetime);
    const now = new Date();
    
    if (selectedDate <= now) {
      const confirm = window.confirm('⚠️ التاريخ والوقت المحدد قد مضى. هل تريد المتابعة؟');
      if (!confirm) return;
    }

    const newLesson: Lesson = {
      id: Date.now().toString(),
      name: lessonName,
      datetime: selectedDatetime,
      notified: false
    };

    setLessons(prev => [...prev, newLesson].sort((a, b) => 
      new Date(a.datetime).getTime() - new Date(b.datetime).getTime()
    ));
    
    setLessonName('');
    setSelectedDatetime('');
    setShowForm(false);
    
    alert('✅ تم إضافة الدرس بنجاح! سيتم تذكيرك حتى لو أغلقت التطبيق.');
  };

  const deleteLesson = (id: string) => {
    const lesson = lessons.find(l => l.id === id);
    if (confirm(`هل أنت متأكد من حذف درس "${lesson?.name}"؟`)) {
      setLessons(prev => prev.filter(l => l.id !== id));
    }
  };

  const resetNotification = (id: string) => {
    setLessons(prev => 
      prev.map(l => l.id === id ? { ...l, notified: false } : l)
    );
  };

  const testAlarm = () => {
    const testLesson: Lesson = {
      id: 'test',
      name: 'اختبار المنبه',
      datetime: new Date().toISOString(),
      notified: false
    };
    
    setCurrentAlarm(testLesson);
    setShowAlarmModal(true);
    playAlarmSound();

    if (swRegistrationRef.current && notificationPermission === 'granted') {
      swRegistrationRef.current.showNotification('🔔 فكرني - اختبار', {
        body: 'هذا اختبار للمنبه والإشعارات ✅\nالمنبه سيعمل حتى لو أغلقت التطبيق!',
        icon: '/icon-192.png',
        requireInteraction: false
      });
    }
  };

  const formatDateTime = (datetime: string) => {
    const date = new Date(datetime);
    return {
      date: date.toLocaleDateString('ar-EG', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      }),
      time: date.toLocaleTimeString('ar-EG', {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
      })
    };
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-100 via-purple-50 to-pink-100" dir="rtl">
      
      {/* نافذة المنبه المنبثقة */}
      {showAlarmModal && currentAlarm && (
        <div className="fixed inset-0 bg-black bg-opacity-70 flex items-center justify-center z-50 p-4 animate-pulse">
          <div className="bg-gradient-to-br from-red-500 to-orange-500 rounded-3xl shadow-2xl p-8 max-w-md w-full text-center transform animate-bounce">
            <div className="text-9xl mb-6 animate-spin">🔔</div>
            
            <h2 className="text-4xl font-bold text-white mb-4">
              تذكير بالدرس!
            </h2>
            
            <div className="bg-white bg-opacity-90 rounded-2xl p-6 mb-6">
              <p className="text-2xl font-bold text-red-600 mb-4">
                📖 {currentAlarm.name}
              </p>
              
              <div className="text-lg text-gray-700 space-y-2">
                {currentAlarm.datetime && (
                  <>
                    <p className="font-bold">
                      📅 {formatDateTime(currentAlarm.datetime).date}
                    </p>
                    <p className="font-bold">
                      🕐 {formatDateTime(currentAlarm.datetime).time}
                    </p>
                  </>
                )}
              </div>
              
              <p className="text-xl font-bold text-purple-700 mt-4">
                هل فهمت؟
              </p>
            </div>
            
            <button
              onClick={dismissAlarm}
              className="w-full bg-white text-red-600 px-8 py-4 rounded-2xl font-bold text-2xl shadow-lg hover:bg-gray-100 transition-all transform hover:scale-105"
            >
              ✅ فهمت، شكراً!
            </button>
          </div>
        </div>
      )}

      {/* الرأس */}
      <div className="bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 text-white py-8 px-4 shadow-xl">
        <div className="max-w-4xl mx-auto">
          <h1 className="text-5xl md:text-6xl font-bold text-center mb-2 flex items-center justify-center gap-3">
            <span>🔔</span>
            <span>فكرني</span>
            <span>🔔</span>
          </h1>
          <p className="text-center text-xl opacity-90 font-bold">لن أنسى أبداً</p>
          
          <div className="flex flex-wrap justify-center gap-3 mt-6">
            <button
              onClick={testAlarm}
              className="bg-white bg-opacity-20 hover:bg-opacity-30 px-6 py-3 rounded-xl text-base font-bold transition-all flex items-center gap-2"
            >
              🔊 اختبار المنبه
            </button>
            
            {notificationPermission !== 'granted' && (
              <button
                onClick={requestNotificationPermission}
                className="bg-yellow-400 hover:bg-yellow-300 text-purple-900 px-6 py-3 rounded-xl text-base font-bold transition-all flex items-center gap-2 animate-pulse"
              >
                🔔 تفعيل الإشعارات
              </button>
            )}
            
            {showInstallPrompt && (
              <button
                onClick={installApp}
                className="bg-green-500 hover:bg-green-400 text-white px-6 py-3 rounded-xl text-base font-bold transition-all flex items-center gap-2 animate-pulse"
              >
                📱 تثبيت التطبيق
              </button>
            )}
            
            <div className="flex gap-2 flex-wrap justify-center">
              {notificationPermission === 'granted' && (
                <div className="bg-green-500 px-4 py-2 rounded-xl text-sm font-bold flex items-center gap-2">
                  ✅ الإشعارات
                </div>
              )}
              
              {serviceWorkerReady && (
                <div className="bg-blue-500 px-4 py-2 rounded-xl text-sm font-bold flex items-center gap-2">
                  ✅ يعمل في الخلفية
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* المحتوى الرئيسي */}
      <div className="max-w-4xl mx-auto p-4 md:p-8">
        
        {/* تنبيه مهم */}
        {serviceWorkerReady && notificationPermission === 'granted' && (
          <div className="bg-green-100 border-2 border-green-500 rounded-2xl p-6 mb-8 text-center">
            <p className="text-2xl font-bold text-green-800 mb-2">
              🎉 التطبيق الآن يعمل في الخلفية!
            </p>
            <p className="text-green-700 text-lg">
              سيتم تذكيرك بدروسك حتى لو أغلقت المتصفح أو التبويب
            </p>
          </div>
        )}

        {!serviceWorkerReady && (
          <div className="bg-yellow-100 border-2 border-yellow-500 rounded-2xl p-6 mb-8 text-center">
            <p className="text-xl font-bold text-yellow-800 mb-2">
              ⚠️ جارٍ تفعيل الخلفية...
            </p>
            <p className="text-yellow-700">
              يرجى الانتظار قليلاً
            </p>
          </div>
        )}
        
        {/* زر إضافة درس جديد */}
        <div className="mb-8">
          <button
            onClick={() => setShowForm(!showForm)}
            className="w-full bg-gradient-to-r from-green-500 to-emerald-500 text-white px-8 py-5 rounded-2xl font-bold text-2xl shadow-xl hover:shadow-2xl transform hover:scale-105 transition-all flex items-center justify-center gap-3"
          >
            <span className="text-4xl">{showForm ? '❌' : '➕'}</span>
            <span>{showForm ? 'إلغاء' : 'إضافة درس جديد'}</span>
          </button>
        </div>

        {/* نموذج إضافة درس */}
        {showForm && (
          <div className="bg-white rounded-3xl shadow-2xl p-8 mb-8 border-4 border-purple-300">
            <h2 className="text-3xl font-bold text-purple-700 mb-6 text-center flex items-center justify-center gap-2">
              <span>📝</span>
              <span>درس جديد</span>
            </h2>
            
            <div className="space-y-6">
              <div>
                <label className="block text-gray-800 font-bold mb-3 text-xl">📖 اسم الدرس:</label>
                <input
                  type="text"
                  value={lessonName}
                  onChange={(e) => setLessonName(e.target.value)}
                  placeholder="مثال: رياضيات، فيزياء، لغة عربية..."
                  className="w-full px-5 py-4 border-3 border-purple-300 rounded-xl focus:outline-none focus:border-purple-600 text-xl"
                />
              </div>

              <div>
                <label className="block text-gray-800 font-bold mb-3 text-xl">📅 التاريخ والوقت:</label>
                <input
                  type="datetime-local"
                  value={selectedDatetime}
                  onChange={(e) => setSelectedDatetime(e.target.value)}
                  className="w-full px-5 py-4 border-3 border-purple-300 rounded-xl focus:outline-none focus:border-purple-600 text-xl"
                />
                <p className="text-sm text-gray-600 mt-2 mr-1 font-bold">
                  💡 سيتم تذكيرك تلقائياً حتى لو أغلقت التطبيق!
                </p>
              </div>

              <button
                onClick={addLesson}
                className="w-full bg-gradient-to-r from-purple-600 to-pink-600 text-white px-6 py-5 rounded-xl font-bold text-2xl hover:shadow-xl transform hover:scale-105 transition-all"
              >
                ✅ حفظ الدرس
              </button>
            </div>
          </div>
        )}

        {/* قائمة الدروس */}
        <div className="space-y-5">
          <h2 className="text-4xl font-bold text-purple-700 mb-6 text-center flex items-center justify-center gap-3">
            <span>📋</span>
            <span>دروسي ({lessons.length})</span>
          </h2>

          {lessons.length === 0 ? (
            <div className="bg-white rounded-3xl shadow-xl p-16 text-center">
              <div className="text-9xl mb-6">📚</div>
              <p className="text-3xl text-gray-500 font-bold mb-2">لا توجد دروس بعد</p>
              <p className="text-xl text-gray-400">ابدأ بإضافة درسك الأول واترك الباقي علينا!</p>
            </div>
          ) : (
            lessons.map(lesson => {
              const { date, time } = formatDateTime(lesson.datetime);
              const lessonDate = new Date(lesson.datetime);
              const now = new Date();
              const isPast = lessonDate < now;
              
              return (
                <div
                  key={lesson.id}
                  className={`bg-white rounded-3xl shadow-xl p-7 border-r-8 ${
                    lesson.notified 
                      ? 'border-gray-400 bg-gray-50' 
                      : isPast 
                        ? 'border-orange-500 bg-orange-50'
                        : 'border-purple-600'
                  } hover:shadow-2xl transition-all`}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-4 flex-wrap">
                        <span className="text-5xl">📖</span>
                        <h3 className="text-3xl font-bold text-purple-700">{lesson.name}</h3>
                        {lesson.notified && (
                          <span className="bg-gray-500 text-white px-4 py-2 rounded-full text-base font-bold">
                            ✓ تم التنبيه
                          </span>
                        )}
                        {!lesson.notified && isPast && (
                          <span className="bg-orange-500 text-white px-4 py-2 rounded-full text-base font-bold animate-pulse">
                            ⚠️ فات الموعد
                          </span>
                        )}
                      </div>
                      
                      <div className="space-y-3 mr-16">
                        <div className="flex items-center gap-3 text-xl">
                          <span className="text-3xl">📅</span>
                          <span className="font-bold text-gray-700">التاريخ:</span>
                          <span className="text-purple-600 font-bold">{date}</span>
                        </div>
                        
                        <div className="flex items-center gap-3 text-xl">
                          <span className="text-3xl">🕐</span>
                          <span className="font-bold text-gray-700">الوقت:</span>
                          <span className="text-purple-600 font-bold">{time}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-col gap-3">
                      {lesson.notified && (
                        <button
                          onClick={() => resetNotification(lesson.id)}
                          className="bg-blue-500 hover:bg-blue-600 text-white px-5 py-3 rounded-xl text-base font-bold transition-all"
                          title="إعادة تفعيل التنبيه"
                        >
                          🔄
                        </button>
                      )}
                      <button
                        onClick={() => deleteLesson(lesson.id)}
                        className="bg-red-500 hover:bg-red-600 text-white px-5 py-3 rounded-xl text-base font-bold transition-all"
                        title="حذف"
                      >
                        🗑️
                      </button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* التذييل */}
      <footer className="bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 text-white py-8 mt-16">
        <p className="text-center text-2xl font-bold mb-2">
          ✨ تم الإنشاء بواسطة Youssef Mahmoud ✨
        </p>
        <p className="text-center text-lg opacity-90">
          🔔 فكرني - لن أنسى أبداً 💜
        </p>
      </footer>
    </div>
  );
}

export default App;