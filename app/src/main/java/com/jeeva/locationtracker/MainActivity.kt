package com.jeeva.locationtracker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jeeva.locationtracker.databinding.ActivityMainBinding
import com.jeeva.locationtracker.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = Prefs.getRole(this)
        if (role != null && Prefs.getFamilyCode(this) != null) {
            routeToRoleScreen(role)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.parentRoleButton.setOnClickListener {
            startActivity(Intent(this, ParentSetupActivity::class.java))
        }
        binding.viewerRoleButton.setOnClickListener {
            startActivity(Intent(this, ViewerSetupActivity::class.java))
        }
    }

    private fun routeToRoleScreen(role: Prefs.Role) {
        val target = when (role) {
            Prefs.Role.PARENT -> ParentStatusActivity::class.java
            Prefs.Role.VIEWER -> ViewerMapActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }
}
