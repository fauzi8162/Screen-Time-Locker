# Walkthrough: KidLockTimer Fungsional & Siap Deploy

Seluruh file dan struktur proyek untuk aplikasi **KidLockTimer** (Offline Kid Timer Lock) telah berhasil dibuat dan dikonfigurasi di direktori kerja `c:\xampp\htdocs\lock-screen`. Aplikasi ini siap di-push ke repositori GitHub Anda dan di-build secara otomatis menjadi APK yang siap digunakan!

---

## 🛠️ Perubahan yang Dilakukan (Files Created)

Berikut adalah daftar file yang telah dibuat untuk menyusun proyek ini secara utuh:

### 1. Konfigurasi Sistem Proyek (Gradle)
* **[settings.gradle.kts](file:///c:/xampp/htdocs/lock-screen/settings.gradle.kts)**: Konfigurasi repositori Google & Maven Central serta integrasi modul `:app`.
* **[build.gradle.kts](file:///c:/xampp/htdocs/lock-screen/build.gradle.kts)** (root): Deklarasi Android Gradle Plugin v8.2.0 dan Kotlin Gradle Plugin v1.9.0.
* **[app/build.gradle.kts](file:///c:/xampp/htdocs/lock-screen/app/build.gradle.kts)**: Konfigurasi target SDK 34 (Android 14) dan Java 17, serta menyertakan dependensi UI modern (AppCompat, Material, ConstraintLayout).
* **[.gitignore](file:///c:/xampp/htdocs/lock-screen/.gitignore)**: Mengabaikan berkas build lokal dan konfigurasi IDE agar repositori Git Anda tetap bersih.

### 2. Sumber Daya Desain UI Premium (Resources)
* **[app/src/main/res/values/colors.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/values/colors.xml)**: Palet warna malam elegan kustom (Slate Dark background, Indigo primary, Cyan secondary, Emerald success, Red error).
* **[app/src/main/res/values/strings.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/values/strings.xml)**: Teks multibahasa dan pelacak deskripsi Device Admin.
* **[app/src/main/res/values/themes.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/values/themes.xml)**: Menggunakan Material 3 Dark theme sebagai basis tampilan utama.
* **[app/src/main/res/drawable/bg_input_field.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/drawable/bg_input_field.xml)**: Desain kolom input dengan sudut membulat modern.
* **[app/src/main/res/drawable/bg_countdown_circle.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/drawable/bg_countdown_circle.xml)**: Cincin indikator hitung mundur dengan efek cahaya cyan premium.

### 3. Logika Backend & Integrasi Android (Kotlin & Manifest)
* **[app/src/main/res/xml/device_admin_rules.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/res/xml/device_admin_rules.xml)**: Menyatakan kebijakan penguncian paksa layar (`force-lock`).
* **[app/src/main/java/com/example/locktimer/AdminReceiver.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/AdminReceiver.kt)**: Menerima callback sistem ketika hak Device Admin diaktifkan/dinonaktifkan oleh pengguna.
* **[app/src/main/java/com/example/locktimer/BootReceiver.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/BootReceiver.kt)**: Proteksi anti-bypass. Mendengarkan booting HP selesai. Jika ada timer aktif saat HP mati, ia akan langsung melanjutkan timer atau langsung mengunci layar jika waktu habis saat HP dinonaktifkan.
* **[app/src/main/java/com/example/locktimer/TimerService.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/TimerService.kt)**: Layanan latar belakang (`Foreground Service`) dengan notifikasi persistent yang menampilkan countdown dinamis menit-demi-menit serta memanggil sistem penguncian layar begitu timer mencapai nol.
* **[app/src/main/java/com/example/locktimer/MainActivity.kt](file:///c:/xampp/htdocs/lock-screen/app/src/main/java/com/example/locktimer/MainActivity.kt)**: Controller utama aplikasi. Mengontrol verifikasi PIN Orang Tua, inisialisasi awal PIN offline aman, navigasi state layar setup/aktif, serta monitoring update timer secara real-time.
* **[app/src/main/AndroidManifest.xml](file:///c:/xampp/htdocs/lock-screen/app/src/main/AndroidManifest.xml)**: Deklarasi izin-izin (Foreground Service, Boot completed, dll) dan pendaftaran semua komponen Android.

### 4. Konfigurasi CI/CD Otomatis
* **[.github/workflows/android-build.yml](file:///c:/xampp/htdocs/lock-screen/.github/workflows/android-build.yml)**: Pipeline GitHub Actions. Menyiapkan JDK 17, menghasilkan gradle wrapper secara dinamis tanpa perlu menyimpan berkas biner `.jar` di repositori Anda, mengompilasi debug APK, dan mengunggahnya sebagai artefak build otomatis.

---

## 🚀 Panduan Deploy ke GitHub & Build APK

Berikut adalah panduan langkah-demi-langkah bagi Anda untuk menghubungkan direktori lokal ini ke repositori GitHub Anda dan mendapatkan file APK-nya:

### Langkah 1: Buka Terminal di VS Code
Buka workspace `c:\xampp\htdocs\lock-screen` di VS Code Anda, lalu buka terminal terintegrasi (`Ctrl + ~` atau klik *Terminal -> New Terminal*).

### Langkah 2: Inisialisasi Git Lokal
Jalankan rangkaian perintah berikut di terminal Anda untuk menyiapkan repositori lokal dan melakukan commit awal:
```bash
# 1. Inisialisasi git di folder proyek Anda
git init

# 2. Tambahkan semua file yang baru saja dibuat ke staging area
git add .

# 3. Lakukan commit awal
git commit -m "Initial Commit: KidLockTimer dengan Proteksi PIN dan Anti-Bypass"
```

### Langkah 3: Sambungkan ke Repositori GitHub Anda
Gunakan URL repositori yang telah Anda siapkan untuk mengarahkan git lokal ke GitHub:
```bash
# 4. Hubungkan remote origin ke URL Github Anda
git remote add origin https://github.com/fauzi8162/Screen-Time-Locker.git

# 5. Buat atau atur nama branch utama menjadi main
git branch -M main

# 6. Push kode lokal Anda ke GitHub
git push -u origin main
```

### Langkah 4: Unduh File APK dari GitHub Actions
Setelah perintah `git push` selesai berjalan:
1. Buka halaman repositori Anda di browser: [GitHub Screen-Time-Locker](https://github.com/fauzi8162/Screen-Time-Locker).
2. Klik tab **Actions** di bagian atas menu repositori.
3. Anda akan melihat sebuah proses *workflow* berjalan bernama `"Android CI/CD (Build APK)"`.
4. Tunggu sekitar 2-3 menit sampai proses kompilasi selesai (ditandai dengan centang hijau ✅).
5. Klik pada hasil build tersebut, scroll ke bagian paling bawah di bawah sub-judul **Artifacts**, dan Anda akan melihat berkas bernama `KidLockTimer-Debug-APK`.
6. Klik pada nama berkas tersebut untuk mengunduhnya. Ekstrak file zip yang diunduh untuk mendapatkan file `app-debug.apk`.

### Langkah 5: Instalasi di HP Target (HP Anak)
1. Kirim file `app-debug.apk` ke HP target (bisa melalui WhatsApp, kabel data, atau Google Drive).
2. Buka berkas APK tersebut di HP anak dan lakukan instalasi (jika muncul peringatan *Unknown Sources*, izinkan pemasangan).
3. Buka aplikasi **Kid Lock Timer**.
4. **SANGAT PENTING:** Klik tombol berwarna merah **"Aktifkan Izin Administrator"** terlebih dahulu, kemudian setujui pengaktifan Device Administrator agar aplikasi diizinkan mengunci layar.
5. Buat PIN Orang Tua baru (PIN 4-Digit).
6. Masukkan durasi bermain dalam menit (misal: 10 menit).
7. Klik **"Mulai Sesi Anak"**.
8. Aplikasi akan masuk ke mode berjalan latar belakang dengan notifikasi persisten yang menunjukkan sisa waktu bermain secara langsung.
9. Untuk menghentikan timer sebelum waktunya habis, Anda perlu membuka kembali aplikasi dan memasukkan PIN Orang Tua yang telah dibuat.
