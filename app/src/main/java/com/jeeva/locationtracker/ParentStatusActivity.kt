package com.jeeva.locationtracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jeeva.locationtracker.databinding.ActivityParentStatusBinding
import com.jeeva.locationtracker.service.LocationForegroundService
import com.jeeva.locationtracker.util.Prefs

class ParentStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentStatusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val code = Prefs.getFamilyCode(this)
        binding.familyCodeText.text = getString(R.string.parent_status_code_prefix, code)

        binding.stopSharingButton.setOnClickListener { stopSharing() }
    }

    private fun stopSharing() {
        startService(
            Intent(this, LocationForegroundService::class.java)
                .setAction(LocationForegroundService.ACTION_STOP)
        )
        Prefs.clear(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
