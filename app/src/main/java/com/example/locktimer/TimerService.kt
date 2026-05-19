package com.example.locktimer

import android.app.*
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private lateinit var prefs: SharedPreferences
    
    companion object {
        const val CHANNEL_ID = "KidLockTimerServiceChannel"
        const val NOTIFICATION_ID = 101
        
        // Broadcast Action Constants
        const val ACTION_TIMER_TICK = "com.example.locktimer.TIMER_TICK"
        const val ACTION_TIMER_FINISHED = "com.example.locktimer.TIMER_FINISHED"
        const val EXTRA_REMAINING_MS = "remaining_ms"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("KidLockTimerPrefs", Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // Ambil durasi. Bisa berupa duration_ms (dari BootReceiver) atau menit (dari MainActivity)
        val durationMs = intent?.getLongExtra("duration_ms", -1L) ?: -1L
        val durationMin = intent?.getIntExtra("duration_min", -1) ?: -1
        
        val totalMs = when {
            durationMs > 0 -> durationMs
            durationMin > 0 -> durationMin * 60 * 1000L
            else -> 15 * 60 * 1000L // Default 15 menit jika kosong
        }

        val endTimeMillis = System.currentTimeMillis() + totalMs
        
        // Simpan status aktif ke SharedPreferences untuk proteksi boot-loop/reboot
        prefs.edit().apply {
            putBoolean("pref_timer_active", true)
            putLong("pref_end_time_millis", endTimeMillis)
            apply()
        }

        // Jadwalkan AlarmManager sebagai fallback tangguh saat aplikasi ditutup/dimatikan
        scheduleAlarm(endTimeMillis)

        // Tampilkan notifikasi persistent foreground service segera
        val initialNotification = buildNotification(totalMs)
        startForeground(NOTIFICATION_ID, initialNotification)

        // Hentikan timer sebelumnya jika ada
        countDownTimer?.cancel()

        // Mulai hitung mundur
        countDownTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Perbarui notifikasi persistent
                val notification = buildNotification(millisUntilFinished)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notification)

                // Kirim broadcast agar MainActivity (jika terbuka) bisa memperbarui UI
                val tickIntent = Intent(ACTION_TIMER_TICK).apply {
                    putExtra(EXTRA_REMAINING_MS, millisUntilFinished)
                }
                sendBroadcast(tickIntent)
            }

            override fun onFinish() {
                // Sesi habis, kunci layar secara instan!
                lockScreen()

                // Kirim broadcast finish ke UI
                val finishedIntent = Intent(ACTION_TIMER_FINISHED)
                sendBroadcast(finishedIntent)

                // Bersihkan data prefs & matikan service
                prefs.edit().putBoolean("pref_timer_active", false).apply()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()

        return START_STICKY // Ubah ke START_STICKY agar OS menghidupkan kembali service jika tidak sengaja terbunuh
    }

    private fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, AdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            try {
                dpm.lockNow()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    private fun buildNotification(remainingMs: Long): Notification {
        val minutes = (remainingMs / 1000) / 60
        val seconds = (remainingMs / 1000) % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sesi Bermain Anak Aktif")
            .setContentText("Sisa waktu bermain: $timeString")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Penghitung Waktu Sesi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan sisa waktu sesi anak di bilah notifikasi agar tidak dihentikan oleh sistem."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        // Batalkan alarm jika service dihancurkan (misalnya saat dihentikan manual oleh PIN Orang Tua)
        cancelAlarm()
        super.onDestroy()
    }

    private fun scheduleAlarm(endTimeMillis: Long) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        endTimeMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        endTimeMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    endTimeMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback jika izin exact alarm dinonaktifkan
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                endTimeMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
