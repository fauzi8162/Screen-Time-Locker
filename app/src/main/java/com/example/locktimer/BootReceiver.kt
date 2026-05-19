package com.example.locktimer

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs: SharedPreferences = context.getSharedPreferences("KidLockTimerPrefs", Context.MODE_PRIVATE)
            val isTimerActive = prefs.getBoolean("pref_timer_active", false)
            
            if (isTimerActive) {
                val endTimeMillis = prefs.getLong("pref_end_time_millis", 0)
                val currentTime = System.currentTimeMillis()
                val remainingMillis = endTimeMillis - currentTime

                if (remainingMillis > 0) {
                    // Sesi belum habis, lanjutkan sisa waktu timer
                    val serviceIntent = Intent(context, TimerService::class.java).apply {
                        putExtra("duration_ms", remainingMillis)
                    }
                    try {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Waktu sudah habis saat HP mati, langsung kunci layar!
                    lockScreen(context)
                    // Reset state
                    prefs.edit().putBoolean("pref_timer_active", false).apply()
                }
            }
        }
    }

    private fun lockScreen(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, AdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            try {
                dpm.lockNow()
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}
