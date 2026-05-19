# Rencana Implementasi: KidLockTimer (Offline Kid Timer Lock)

Aplikasi Android berbasis Kotlin murni tanpa library pihak ketiga yang berfungsi sebagai pengontrol waktu bermain anak (*Offline Kid Timer Lock*). Aplikasi ini berjalan di latar belakang (*Foreground Service*), menggunakan `DevicePolicyManager` untuk mengunci layar secara instan ketika waktu habis, dilengkapi dengan sistem keamanan PIN Orang Tua, serta kemampuan pemulihan otomatis jika perangkat di-reboot.

---

## User Review Required

> [!IMPORTANT]
> **Izin Administrator Perangkat (Device Admin):**
> Aplikasi ini memerlukan izin administrator perangkat untuk mematikan dan mengunci layar secara programatis. Pengguna harus mengaktifkan izin ini secara manual di pengaturan sistem Android melalui petunjuk yang disediakan di aplikasi.

> [!TIP]
> **Anti-Bypass (Boot Recovery):**
> Kami menambahkan fitur pendeteksi restart (`BootReceiver`). Jika anak mematikan/menghidupkan ulang HP untuk menghindari timer, aplikasi akan mendeteksi boot ulang, menghitung sisa waktu secara otomatis, dan segera melanjutkan timer atau langsung mengunci layar jika waktu sudah habis selama HP mati.

---

## Proposed Changes

Proyek ini akan dibangun sebagai proyek Gradle Kotlin murni di folder workspace `c:\xampp\htdocs\lock-screen`. Berikut adalah struktur komponen yang akan dibuat:

```text
📁 c:\xampp\htdocs\lock-screen/
├── 📁 .github/
│   └── 📁 workflows/
│       └── 📄 android-build.yml           <- Pipeline CI/CD GitHub Actions untuk build APK
├── 📁 app/
│   ├── 📄 build.gradle.kts                <- Konfigurasi modul aplikasi
│   └── 📁 src/
│       └── 📁 main/
│           ├── 📄 AndroidManifest.xml     <- Deklarasi komponen, Service, Receiver, dan permission
│           ├── 📁 java/
│           │   └── 📁 com/
│           │       └── 📁 example/
│           │           └── 📁 locktimer/
│           │               ├── 📄 AdminReceiver.kt   <- Menangani izin Device Admin
│           │               ├── 📄 BootReceiver.kt    <- Menangani pemulihan timer setelah restart HP
│           │               ├── 📄 MainActivity.kt    <- UI Utama, Input PIN, Setup Timer, Dashboard
│           │               └── 📄 TimerService.kt     <- Foreground Service & Hitung Mundur Persistent
│           └── 📁 res/
│               ├── 📁 layout/
│               │   └── 📄 activity_main.xml  <- Tampilan antarmuka modern & premium
│               ├── 📁 values/
│               │   ├── 📄 colors.xml         <- Palet warna modern (Indigo, Cyan, Dark Slate)
│               │   ├── 📄 strings.xml        <- Teks multibahasa/lokalisasi
│               │   └── 📄 themes.xml         <- Tema Material3 modern
│               └── 📁 xml/
│                   └── 📄 device_admin_rules.xml <- Konfigurasi aturan kebijakan Device Admin
├── 📄 build.gradle.kts                    <- Konfigurasi build root proyek
└── 📄 settings.gradle.kts                 <- Konfigurasi repositori dan modul Gradle
```

### 1. Konfigurasi Proyek & CI/CD
Kita akan mengonfigurasi build gradle dan setup GitHub Actions agar setiap push ke repositori GitHub Anda akan secara otomatis memicu proses build di cloud dan menghasilkan file `.apk` yang siap diunduh.

* #### [NEW] [.github/workflows/android-build.yml](file:///c:/xampp/htdocs/lock-screen/.github/workflows/android-build.yml)
  Konfigurasi GitHub Actions untuk meng-compile proyek menggunakan JDK 17, membuat Gradle Wrapper secara otomatis, mem-build file debug APK, dan mengunggahnya sebagai artefak build yang dapat langsung diunduh dari tab Actions di GitHub.
* #### [NEW] [settings.gradle.kts](file:///c:/xampp/htdocs/lock-screen/settings.gradle.kts)
  Mendefinisikan nama proyek `"KidLockTimer"`, mengaktifkan manajemen resolusi dependensi terpusat, dan menyertakan subproyek `:app`.
* #### [NEW] [build.gradle.kts](file:///c:/xampp/htdocs/lock-screen/build.gradle.kts)
  Konfigurasi Gradle root yang menerapkan Android Gradle Plugin (8.2.0) dan Kotlin Gradle Plugin (1.9.0).
* #### [NEW] [app/build.gradle.kts](file:///c:/xampp/htdocs/lock-screen/app/build.gradle.kts)
  Mengatur target SDK ke 34 (Android 14) dengan minimum SDK 26 (Android 8.0) untuk memastikan kompatibilitas Notification Channel. Menyertakan pustaka UI modern seperti AndroidX Core, AppCompat, Material Components, dan ConstraintLayout.

### 2. Antarmuka Pengguna Premium & Sumber Daya UI
Aplikasi ini akan memiliki desain antarmuka yang sangat menarik, menggunakan skema warna malam yang elegan (Deep Slate / Dark Violet) dengan visualisasi status timer yang modern.

* #### [NEW] [app/src/main/res/values/colors.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/values/colors.xml)
  Palet warna kustom kelas premium:
  * `colorPrimary`: Indigo Violet `#6366F1`
  * `colorSecondary`: Bright Cyan `#06B6D4`
  * `colorBackground`: Deep Slate Charcoal `#0F172A`
  * `colorSurface`: Charcoal Card `#1E293B`
* #### [NEW] [app/src/main/res/values/themes.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/values/themes.xml)
  Menerapkan tema Material 3 gelap (`Theme.Material3.DayNight.NoActionBar`) dengan pewarnaan custom di status bar dan elemen kontrol.
* #### [NEW] [app/src/main/res/layout/activity_main.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/layout/activity_main.xml)
  Tata letak (UI) premium dengan:
  * **Card Status:** Menunjukkan status sesi saat ini (Aktif / Tidak Aktif).
  * **Input Waktu:** Kontrol input nomor yang bersih dengan ikon penjelas.
  * **Layar Hitung Mundur Dinamis:** Saat timer berjalan, menampilkan sisa waktu format besar (`MM:SS`) dengan animasi mikro.
  * **Tombol Hentikan Sesi:** Memerlukan konfirmasi PIN Orang Tua sebelum membatalkan timer.
  * **Input PIN Orang Tua:** Elemen input tersembunyi yang meminta PIN 4-digit untuk mengubah pengaturan.

### 3. Logika Backend Kotlin (Core Logic)
Seluruh fungsionalitas inti aplikasi ditulis dalam Kotlin murni dengan interaksi API Android tingkat rendah untuk keamanan tingkat tinggi.

* #### [NEW] [app/src/main/res/xml/device_admin_rules.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/xml/device_admin_rules.xml)
  Mendeklarasikan bahwa aplikasi meminta hak akses `force-lock` (mengunci layar).
* #### [NEW] [app/src/main/java/com/example/locktimer/AdminReceiver.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/AdminReceiver.kt)
  Menerima siaran sistem saat status administrator perangkat berubah dan menangani umpan balik pengguna.
* #### [NEW] [app/src/main/java/com/example/locktimer/BootReceiver.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/BootReceiver.kt)
  Mendengarkan siaran `ACTION_BOOT_COMPLETED`. Jika SharedPreferences menunjukkan bahwa ada timer aktif yang belum selesai saat HP dimatikan, receiver akan:
  * Menghitung selisih waktu saat ini dengan target akhir (`endTimeMillis`).
  * Jika sisa waktu masih ada: Menjalankan kembali `TimerService` dengan sisa durasi.
  * Jika waktu sudah habis saat HP dimatikan: Secara instan memanggil Device Admin untuk mengunci layar begitu HP selesai menyala.
* #### [NEW] [app/src/main/java/com/example/locktimer/TimerService.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/TimerService.kt)
  Layanan latar belakang (`Foreground Service`) dengan prioritas tinggi agar tidak dihentikan oleh OS saat anak sedang membuka aplikasi berat.
  * Membuat Notification Channel dan menampilkan notifikasi persisten yang menunjukkan hitung mundur menit demi menit.
  * Menggunakan `CountDownTimer` untuk melacak sisa waktu.
  * Menyimpan target waktu akhir (`endTimeMillis`) ke SharedPreferences untuk pencegahan restart.
  * Ketika hitung mundur selesai: Memanggil `DevicePolicyManager.lockNow()` untuk mengunci layar, menghapus state timer, dan menghentikan layanan.
* #### [NEW] [app/src/main/java/com/example/locktimer/MainActivity.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/MainActivity.kt)
  Pusat pengendali aplikasi:
  * Memvalidasi status Device Admin dan meminta izin jika belum diberikan.
  * Mengelola state PIN Orang Tua (menyimpan PIN 4-digit secara offline menggunakan SharedPreferences terenkripsi atau standar aman).
  * Menampilkan antarmuka set-up jika timer mati, atau antarmuka status/countdown jika timer aktif.
  * Melakukan verifikasi PIN sebelum mengizinkan penghentian sesi timer yang sedang berjalan.
* #### [NEW] [app/src/main/AndroidManifest.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/AndroidManifest.xml)
  Mendaftarkan izin yang diperlukan:
  * `USES_POLICY_FORCE_LOCK` (kebijakan penguncian layar)
  * `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` (Android 14) untuk menjaga timer tetap berjalan stabil.
  * `RECEIVE_BOOT_COMPLETED` (izin autostart pasca-reboot).
  * Mendaftarkan `MainActivity`, `TimerService`, `AdminReceiver`, dan `BootReceiver`.

---

## Verification Plan

### Automated Build Verification
Kita akan menggunakan GitHub Actions untuk memvalidasi bahwa seluruh konfigurasi Gradle dan sintaksis Kotlin valid serta berhasil dikompilasi menjadi APK.
* Jalankan kompilasi menggunakan perintah `./gradlew assembleDebug` (akan dijalankan secara otomatis di runner GitHub Actions ubuntu-latest setelah di-push).

### Manual Verification
1. **Verifikasi Pemasangan & Izin:**
   * Instal APK yang dihasilkan di HP Android target.
   * Buka aplikasi, buat PIN baru, dan setujui permintaan izin "Device Administrator".
2. **Uji Coba Timer & Penguncian:**
   * Setel durasi timer pendek (misal: 1 menit) dan klik "Mulai Sesi Anak".
   * Keluar dari aplikasi atau buka aplikasi lain (YouTube / Game). Pastikan notifikasi hitung mundur tetap berjalan stabil di bilah status.
   * Tunggu hingga waktu habis. Layar HP harus terkunci secara instan secara otomatis.
3. **Uji Coba PIN Keamanan:**
   * Saat timer aktif, buka aplikasi kembali dan klik "Hentikan Sesi".
   * Pastikan aplikasi meminta PIN Orang Tua dan tidak mengizinkan penghentian timer sebelum PIN yang benar dimasukkan.
4. **Uji Coba Bypass (Reboot HP):**
   * Setel timer selama 5 menit.
   * Matikan dan hidupkan kembali (restart) HP secara paksa.
   * Begitu HP menyala kembali, pastikan layar segera terkunci kembali (jika waktu habis saat mati) ATAU timer berlanjut secara otomatis melalui Foreground Service.
