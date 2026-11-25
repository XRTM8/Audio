# Audio

## العربية

مشغل ملفات صوت محلية لأندرويد. التطبيق مكتوب بـ Kotlin وJetpack Compose، ويستخدم Media3 للتشغيل وخدمة وسائط في الخلفية. توجد مكتبة Rust أصلية لبعض وظائف معالجة الصوت والبحث والبيانات الوصفية.

### الموجود في المستودع

- عرض المقاطع والألبومات والفنانين والمجلدات من مكتبة الجهاز، مع البحث والفرز وتحديث المكتبة عند تغير ملفات الوسائط.
- تشغيل في الخلفية مع إشعار وتحكم بالوسائط، وطابور تشغيل، وسرعة تشغيل قابلة للتغيير، ومؤقت إيقاف.
- قوائم تشغيل ومفضلة وسجل استماع محفوظة محليًا عبر Room.
- معادل صوت وتأثيرات صوتية، ومكتبة `rust/otium_dsp/` لمعالجة الإشارة وتحليل الطيف وقراءة بعض بيانات الملفات.
- كلمات أغاني متزامنة من ملفات LRC المجاورة، أو من بيانات الملف عند توفرها، مع مسار لجلب الكلمات عبر الشبكة.
- واجهة تشغيل تعرض غلاف الألبوم والألوان المستخرجة منه، وأداة تحكم للشاشة الرئيسية.
- نوعا بناء `github` و`playstore`؛ التحديث داخل التطبيق مفعّل في الأول ومعطّل في الثاني.

### البنية والبناء

`app/` يحتوي تطبيق أندرويد واختبارات الوحدة، و`rust/otium_dsp/` يحتوي المكتبة الأصلية. الحد الأدنى هو Android 8.0 ‏(API 26)، ويستخدم البناء JDK 17 وAndroid SDK 35. لبناء نسخة تطوير:

```bash
./gradlew assembleGithubDebug
```

تتطلب كلمات الأغاني عبر الشبكة وتحديثات GitHub اتصالًا بالإنترنت؛ تشغيل الملفات المحلية لا يعتمد عليه. الترخيص: [GPL-3.0](LICENSE).

## English

Otium Audio is a local music player for Android. It uses Kotlin and Jetpack Compose, Media3 for playback and a background media service, and a Rust library for selected audio processing, search, and metadata tasks.

### Repository contents

- Device-library views for tracks, albums, artists, and folders, with search, sorting, and updates when media files change.
- Background playback with media notification and controls, a play queue, playback-speed controls, and a sleep timer.
- Playlists, favorites, and listening history stored locally with Room.
- Equalizer and audio effects; `rust/otium_dsp/` contains native signal processing, spectrum analysis, and file-metadata routines.
- Synchronized lyrics from adjacent LRC files or embedded metadata when available, plus an online lyrics retrieval path.
- A playback screen using album artwork colors and a home-screen playback widget.
- `github` and `playstore` build flavors; in-app updates are enabled for the former and disabled for the latter.

### Layout and build

`app/` contains the Android application and unit tests; `rust/otium_dsp/` contains the native library. The minimum version is Android 8.0 (API 26). The build uses JDK 17 and Android SDK 35.

```bash
./gradlew assembleGithubDebug
```

Online lyrics and GitHub updates require network access; local-file playback does not. License: [GPL-3.0](LICENSE).


---

للتواصل / Contact: [@MA.XRT](https://www.instagram.com/ma.xrt/)
