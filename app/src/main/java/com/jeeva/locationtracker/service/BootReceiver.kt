package com.jeeva.locationtracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jeeva.locationtracker.util.Prefs

// Restarts location sharing after the phone reboots (or the app is updated), so a parent's
// phone resumes sharing on its own without anyone having to reopen the app.
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs.getRole(context) != Prefs.Role.PARENT) return
        val code = Prefs.getFamilyCode(context) ?: return

        val serviceIntent = Intent(context, LocationForegroundService::class.java)
            .putExtra(LocationForegroundService.EXTRA_FAMILY_CODE, code)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
