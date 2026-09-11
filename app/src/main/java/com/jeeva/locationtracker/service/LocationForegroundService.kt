package com.jeeva.locationtracker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.jeeva.locationtracker.MainActivity
import com.jeeva.locationtracker.R
import com.jeeva.locationtracker.util.Prefs

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var familyCode: String? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSharing()
            return START_NOT_STICKY
        }

        familyCode = intent?.getStringExtra(EXTRA_FAMILY_CODE) ?: Prefs.getFamilyCode(this)
        if (familyCode.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )

        signInAndStartLocationUpdates()
        return START_STICKY
    }

    private fun signInAndStartLocationUpdates() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startLocationUpdates()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { startLocationUpdates() }
            .addOnFailureListener { e -> Log.e(TAG, "Anonymous sign-in failed", e) }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationCallback != null) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                writeLocationToFirebase(location.latitude, location.longitude, location.accuracy)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback as LocationCallback,
                mainLooper
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing location permission", e)
            stopSelf()
        }
    }

    private fun writeLocationToFirebase(lat: Double, lng: Double, accuracy: Float) {
        val code = familyCode ?: return
        val data = mapOf(
            "lat" to lat,
            "lng" to lng,
            "accuracy" to accuracy,
            "timestamp" to ServerValue.TIMESTAMP
        )
        FirebaseDatabase.getInstance().getReference("families/$code/location")
            .setValue(data)
            .addOnFailureListener { e -> Log.e(TAG, "Failed to write location", e) }
    }

    private fun stopSharing() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, LocationForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.notification_stop_action), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "LocationService"
        private const val CHANNEL_ID = "location_sharing_channel"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 30_000L
        private const val MIN_UPDATE_INTERVAL_MS = 15_000L

        const val ACTION_STOP = "com.jeeva.locationtracker.action.STOP"
        const val EXTRA_FAMILY_CODE = "family_code"
    }
}
