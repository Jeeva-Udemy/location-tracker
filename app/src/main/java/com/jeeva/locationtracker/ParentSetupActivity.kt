package com.jeeva.locationtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jeeva.locationtracker.databinding.ActivityParentSetupBinding
import com.jeeva.locationtracker.service.LocationForegroundService
import com.jeeva.locationtracker.util.FamilyCodeGenerator
import com.jeeva.locationtracker.util.Prefs

class ParentSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentSetupBinding
    private var pendingFamilyCode: String? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (locationGranted) {
                maybeRequestBatteryOptimizationExemption()
                startSharing()
            } else {
                Toast.makeText(this, R.string.permission_location_required, Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startSharingButton.setOnClickListener { onStartSharingClicked() }
    }

    private fun onStartSharingClicked() {
        val code = FamilyCodeGenerator.normalize(binding.familyCodeInput.text?.toString().orEmpty())
        if (code.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_code, Toast.LENGTH_SHORT).show()
            return
        }
        pendingFamilyCode = code
        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            maybeRequestBatteryOptimizationExemption()
            startSharing()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun maybeRequestBatteryOptimizationExemption() {
        val powerManager = getSystemService(PowerManager::class.java)
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName")
                    )
                )
            } catch (_: Exception) {
                // Some OEM ROMs block this intent; sharing still works without the exemption.
            }
        }
    }

    private fun startSharing() {
        val code = pendingFamilyCode ?: return
        Prefs.save(this, Prefs.Role.PARENT, code)

        val serviceIntent = Intent(this, LocationForegroundService::class.java)
            .putExtra(LocationForegroundService.EXTRA_FAMILY_CODE, code)
        ContextCompat.startForegroundService(this, serviceIntent)

        startActivity(Intent(this, ParentStatusActivity::class.java))
        finish()
    }
}
