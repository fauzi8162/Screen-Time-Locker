package com.example.locktimer

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs: SharedPreferences = context.getSharedPreferences("KidLockTimerPrefs", Context.MODE_PRIVATE)
        val isTimerActive = prefs.getBoolean("pref_timer_active", false)

        if (isTimerActive) {
            // 1. Kunci layar secara instan! (Bisa berjalan di background)
            lockScreen(context)

            // 2. Hentikan TimerService jika masih berjalan di background
            val serviceIntent = Intent(context, TimerService::class.java)
            context.stopService(serviceIntent)

            // 3. Kirim broadcast finish agar UI terupdate jika MainActivity sedang terbuka
            val finishedIntent = Intent(TimerService.ACTION_TIMER_FINISHED)
            context.sendBroadcast(finishedIntent)

            // 4. Update status di SharedPreferences
            prefs.edit().putBoolean("pref_timer_active", false).apply()
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
