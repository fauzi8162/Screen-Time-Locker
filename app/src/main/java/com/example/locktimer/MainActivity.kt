package com.example.locktimer

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private lateinit var prefs: SharedPreferences
    private var failedAttempts = 0

    // Views
    private lateinit var cardAdminWarning: MaterialCardView
    private lateinit var btnEnableAdmin: MaterialButton
    private lateinit var cardNotificationWarning: MaterialCardView
    private lateinit var btnEnableNotification: MaterialButton
    private lateinit var cardBatteryWarning: MaterialCardView
    private lateinit var btnDisableBatteryOptimization: MaterialButton

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
    private val REQUEST_CODE_NOTIFICATION_PERMISSION = 2

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
        cardNotificationWarning = findViewById(R.id.cardNotificationWarning)
        btnEnableNotification = findViewById(R.id.btnEnableNotification)
        cardBatteryWarning = findViewById(R.id.cardBatteryWarning)
        btnDisableBatteryOptimization = findViewById(R.id.btnDisableBatteryOptimization)

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

        btnEnableNotification.setOnClickListener {
            requestNotificationPermission()
        }

        btnDisableBatteryOptimization.setOnClickListener {
            requestBatteryOptimizationExclusion()
        }

        btnStart.setOnClickListener {
            handleStartSession()
        }

        btnStop.setOnClickListener {
            handleStopSession()
        }

        updateAdminPermissionCard()
        updateNotificationPermissionCard()
        updateBatteryPermissionCard()
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
        updateNotificationPermissionCard()
        updateBatteryPermissionCard()
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

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun updateNotificationPermissionCard() {
        if (checkNotificationPermission()) {
            cardNotificationWarning.visibility = View.GONE
        } else {
            cardNotificationWarning.visibility = View.VISIBLE
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_NOTIFICATION_PERMISSION
            )
        }
    }

    private fun checkBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    private fun updateBatteryPermissionCard() {
        if (checkBatteryOptimizationIgnored()) {
            cardBatteryWarning.visibility = View.GONE
        } else {
            cardBatteryWarning.visibility = View.VISIBLE
        }
    }

    private fun requestBatteryOptimizationExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                } catch (ex: Exception) {
                    Toast.makeText(this, "Pengaturan baterai tidak didukung di perangkat ini.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION_PERMISSION) {
            updateNotificationPermissionCard()
            if (checkNotificationPermission()) {
                Toast.makeText(this, "Izin Notifikasi aktif!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin Notifikasi ditolak. Aplikasi mungkin mati di latar belakang.", Toast.LENGTH_LONG).show()
            }
        }
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

        // 2. Validasi Durasi Detik
        val detikText = inputMenit.text.toString().trim()
        if (detikText.isEmpty()) {
            inputMenit.error = "Masukkan durasi waktu bermain (detik)!"
            return
        }
        val detik = detikText.toIntOrNull()
        if (detik == null || detik <= 0) {
            inputMenit.error = "Durasi harus lebih dari 0 detik!"
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
            failedAttempts = 0
            Toast.makeText(this, "PIN Orang Tua berhasil dibuat!", Toast.LENGTH_SHORT).show()
        } else {
            // Verifikasi PIN
            if (pinInput != savedPin) {
                failedAttempts++
                inputSetupPin.error = "PIN Salah! Gagal memulai sesi anak ($failedAttempts/3)."
                if (failedAttempts >= 3) {
                    showResetPinDialog()
                }
                return
            }
        }

        failedAttempts = 0

        // 4. Jalankan TimerService Foreground
        val serviceIntent = Intent(this, TimerService::class.java).apply {
            putExtra("duration_sec", detik)
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
        Toast.makeText(this, "Sesi Bermain Anak dimulai selama $detik detik!", Toast.LENGTH_SHORT).show()
    }

    private fun handleStopSession() {
        val pinInput = inputVerifyPin.text.toString().trim()
        val savedPin = prefs.getString("pref_parent_pin", null)

        if (pinInput.isEmpty()) {
            inputVerifyPin.error = "Masukkan PIN Orang Tua!"
            return
        }

        if (pinInput != savedPin) {
            failedAttempts++
            inputVerifyPin.error = "PIN Salah! Gagal membatalkan timer ($failedAttempts/3)."
            Toast.makeText(this, "PIN Salah! Hubungi Orang Tua.", Toast.LENGTH_SHORT).show()
            if (failedAttempts >= 3) {
                showResetPinDialog()
            }
            return
        }

        failedAttempts = 0

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

    private fun showResetPinDialog() {
        val num1 = (6..9).random()
        val num2 = (6..9).random()
        val correctAnswer = num1 * num2

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Verifikasi Orang Tua (Reset PIN)")
        builder.setMessage("Sandi salah 3 kali. Selesaikan perkalian berikut untuk mereset PIN:\n\nBerapakah hasil dari $num1 x $num2?")

        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Jawaban Anda"
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(60, 20, 60, 20)
            }
            input.layoutParams = params
            addView(input)
        }
        builder.setView(container)

        builder.setPositiveButton("Reset PIN") { dialog, _ ->
            val answerText = input.text.toString().trim()
            val answer = answerText.toIntOrNull()
            if (answer == correctAnswer) {
                // Hapus PIN lama
                prefs.edit().remove("pref_parent_pin").apply()
                failedAttempts = 0
                
                // Hentikan sesi aktif jika sedang berjalan
                val isTimerActive = prefs.getBoolean("pref_timer_active", false)
                if (isTimerActive) {
                    val serviceIntent = Intent(this, TimerService::class.java)
                    stopService(serviceIntent)
                    prefs.edit().putBoolean("pref_timer_active", false).apply()
                    showSetupMode()
                }
                
                checkPinState()
                Toast.makeText(this, "PIN berhasil direset! Silakan buat PIN baru.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Jawaban salah! Gagal mereset PIN.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.dismiss()
        }

        builder.setCancelable(false)
        builder.show()
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
        val seconds = (remainingMs + 999) / 1000
        txtTimerDisplay.text = "$seconds detik"
    }
}
