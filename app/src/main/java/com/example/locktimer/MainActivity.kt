package com.example.locktimer

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var prefs: SharedPreferences

    // Views
    private lateinit var cardAdminWarning: MaterialCardView
    private lateinit var btnEnableAdmin: MaterialButton
    private lateinit var layoutSetupMode: LinearLayout
    private lateinit var inputMenit: EditText
    private lateinit var txtPinLabel: TextView
    private lateinit var inputSetupPin: EditText
    private lateinit var btnStart: MaterialButton
    
    private lateinit var layoutActiveMode: LinearLayout
    private lateinit var txtTimerDisplay: TextView
    private lateinit var inputVerifyPin: EditText
    private lateinit var btnStop: MaterialButton

    private val REQUEST_CODE_ENABLE_ADMIN = 1

    // Receiver untuk menerima broadcast timer dari TimerService
    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                TimerService.ACTION_TIMER_TICK -> {
                    val remainingMs = intent.getLongExtra(TimerService.EXTRA_REMAINING_MS, 0L)
                    updateTimerUI(remainingMs)
                }
                TimerService.ACTION_TIMER_FINISHED -> {
                    showSetupMode()
                    Toast.makeText(context, "Sesi bermain anak telah selesai! Layar dikunci.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Managers & Preferences
        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)
        prefs = getSharedPreferences("KidLockTimerPrefs", Context.MODE_PRIVATE)

        // Bind Views
        cardAdminWarning = findViewById(R.id.cardAdminWarning)
        btnEnableAdmin = findViewById(R.id.btnEnableAdmin)
        layoutSetupMode = findViewById(R.id.layoutSetupMode)
        inputMenit = findViewById(R.id.inputMenit)
        txtPinLabel = findViewById(R.id.txtPinLabel)
        inputSetupPin = findViewById(R.id.inputSetupPin)
        btnStart = findViewById(R.id.btnStart)
        
        layoutActiveMode = findViewById(R.id.layoutActiveMode)
        txtTimerDisplay = findViewById(R.id.txtTimerDisplay)
        inputVerifyPin = findViewById(R.id.inputVerifyPin)
        btnStop = findViewById(R.id.btnStop)

        // Setup Button Listeners
        btnEnableAdmin.setOnClickListener {
            requestDeviceAdmin()
        }

        btnStart.setOnClickListener {
            handleStartSession()
        }

        btnStop.setOnClickListener {
            handleStopSession()
        }

        updateAdminPermissionCard()
        checkPinState()
    }

    override fun onResume() {
        super.onResume()
        
        // Cek apakah timer saat ini sedang aktif
        val isTimerActive = prefs.getBoolean("pref_timer_active", false)
        val endTimeMillis = prefs.getLong("pref_end_time_millis", 0L)
        val remainingMs = endTimeMillis - System.currentTimeMillis()

        if (isTimerActive && remainingMs > 0) {
            showActiveMode()
            updateTimerUI(remainingMs)
        } else {
            showSetupMode()
            // Reset prefs jika sisa waktu sudah habis tapi state masih active
            if (isTimerActive) {
                prefs.edit().putBoolean("pref_timer_active", false).apply()
            }
        }

        updateAdminPermissionCard()
        checkPinState()

        // Registrasi receiver broadcast timer
        val filter = IntentFilter().apply {
            addAction(TimerService.ACTION_TIMER_TICK)
            addAction(TimerService.ACTION_TIMER_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(timerReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(timerReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(timerReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateAdminPermissionCard() {
        val isAdminActive = dpm.isAdminActive(adminComponent)
        if (isAdminActive) {
            cardAdminWarning.visibility = View.GONE
        } else {
            cardAdminWarning.visibility = View.VISIBLE
        }
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_description))
        }
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            updateAdminPermissionCard()
            if (dpm.isAdminActive(adminComponent)) {
                Toast.makeText(this, "Izin Administrator berhasil diaktifkan!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin Administrator ditolak. Aplikasi tidak dapat mengunci layar.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkPinState() {
        val savedPin = prefs.getString("pref_parent_pin", null)
        if (savedPin.isNullOrEmpty()) {
            txtPinLabel.text = "Buat PIN 4-Digit Orang Tua"
            inputSetupPin.hint = "Masukkan 4 angka sandi baru"
        } else {
            txtPinLabel.text = "Masukkan PIN Orang Tua Untuk Konfirmasi"
            inputSetupPin.hint = "Verifikasi PIN"
        }
    }

    private fun handleStartSession() {
        // 1. Validasi Izin Administrator
        if (!dpm.isAdminActive(adminComponent)) {
            Toast.makeText(this, "Harap aktifkan Izin Administrator terlebih dahulu!", Toast.LENGTH_LONG).show()
            requestDeviceAdmin()
            return
        }

        // 2. Validasi Durasi Menit
        val menitText = inputMenit.text.toString().trim()
        if (menitText.isEmpty()) {
            inputMenit.error = "Masukkan durasi waktu bermain!"
            return
        }
        val menit = menitText.toIntOrNull()
        if (menit == null || menit <= 0) {
            inputMenit.error = "Durasi harus lebih dari 0 menit!"
            return
        }

        // 3. Validasi & Simpan PIN
        val pinInput = inputSetupPin.text.toString().trim()
        if (pinInput.length != 4) {
            inputSetupPin.error = "PIN harus terdiri dari 4 angka!"
            return
        }

        val savedPin = prefs.getString("pref_parent_pin", null)
        if (savedPin.isNullOrEmpty()) {
            // Pertama kali setup PIN, simpan
            prefs.edit().putString("pref_parent_pin", pinInput).apply()
            Toast.makeText(this, "PIN Orang Tua berhasil dibuat!", Toast.LENGTH_SHORT).show()
        } else {
            // Verifikasi PIN
            if (pinInput != savedPin) {
                inputSetupPin.error = "PIN Salah! Gagal memulai sesi anak."
                return
            }
        }

        // 4. Jalankan TimerService Foreground
        val serviceIntent = Intent(this, TimerService::class.java).apply {
            putExtra("duration_min", menit)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // Reset input fields
        inputSetupPin.text.clear()
        
        // Pindah ke tampilan Active
        showActiveMode()
        Toast.makeText(this, "Sesi Bermain Anak dimulai selama $menit menit!", Toast.LENGTH_SHORT).show()
    }

    private fun handleStopSession() {
        val pinInput = inputVerifyPin.text.toString().trim()
        val savedPin = prefs.getString("pref_parent_pin", null)

        if (pinInput.isEmpty()) {
            inputVerifyPin.error = "Masukkan PIN Orang Tua!"
            return
        }

        if (pinInput != savedPin) {
            inputVerifyPin.error = "PIN Salah! Gagal membatalkan timer."
            Toast.makeText(this, "PIN Salah! Hubungi Orang Tua.", Toast.LENGTH_SHORT).show()
            return
        }

        // Stop Timer Service
        val serviceIntent = Intent(this, TimerService::class.java)
        stopService(serviceIntent)

        // Reset status di Shared Preferences
        prefs.edit().putBoolean("pref_timer_active", false).apply()

        // Bersihkan field verifikasi
        inputVerifyPin.text.clear()

        // Pindah ke Setup Mode
        showSetupMode()
        Toast.makeText(this, "Sesi Bermain Anak berhasil dihentikan.", Toast.LENGTH_SHORT).show()
    }

    private fun showSetupMode() {
        layoutSetupMode.visibility = View.VISIBLE
        layoutActiveMode.visibility = View.GONE
    }

    private fun showActiveMode() {
        layoutSetupMode.visibility = View.GONE
        layoutActiveMode.visibility = View.VISIBLE
    }

    private fun updateTimerUI(remainingMs: Long) {
        val minutes = (remainingMs / 1000) / 60
        val seconds = (remainingMs / 1000) % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        txtTimerDisplay.text = timeString
    }
}
