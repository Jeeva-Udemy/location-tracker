package com.jeeva.locationtracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jeeva.locationtracker.databinding.ActivityViewerMapBinding
import com.jeeva.locationtracker.model.LocationPoint
import com.jeeva.locationtracker.util.Prefs

class ViewerMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityViewerMapBinding
    private var map: GoogleMap? = null
    private var latestLocation: LocationPoint? = null
    private var locationListener: ValueEventListener? = null
    private lateinit var familyCode: String

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateStatusText()
            refreshHandler.postDelayed(this, 30_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val code = Prefs.getFamilyCode(this)
        if (code == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        familyCode = code

        (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)
            .getMapAsync(this)

        binding.shareLocationButton.setOnClickListener { shareLocation() }
        binding.openMapsButton.setOnClickListener { openInMapsApp() }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        ensureSignedInAndListen()
    }

    private fun ensureSignedInAndListen() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            attachLocationListener()
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { attachLocationListener() }
            .addOnFailureListener { e ->
                Log.e(TAG, "Anonymous sign-in failed", e)
                Toast.makeText(this, "Could not connect. Check your internet connection.", Toast.LENGTH_LONG).show()
            }
    }

    private fun attachLocationListener() {
        val ref = FirebaseDatabase.getInstance().getReference("families/$familyCode/location")
        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val point = snapshot.getValue(LocationPoint::class.java) ?: return
                latestLocation = point
                onLocationUpdated(point)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Location listener cancelled", error.toException())
            }
        }
        ref.addValueEventListener(locationListener as ValueEventListener)
    }

    private fun onLocationUpdated(point: LocationPoint) {
        val latLng = LatLng(point.lat, point.lng)
        val currentMap = map ?: return

        currentMap.clear()
        currentMap.addMarker(MarkerOptions().position(latLng).title(getString(R.string.app_name)))
        currentMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

        updateStatusText()
    }

    private fun updateStatusText() {
        val point = latestLocation
        binding.statusText.text = if (point == null) {
            getString(R.string.viewer_map_waiting)
        } else {
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                point.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            getString(R.string.viewer_map_last_updated, relativeTime)
        }
    }

    private fun shareLocation() {
        val point = latestLocation
        if (point == null) {
            Toast.makeText(this, R.string.viewer_map_waiting, Toast.LENGTH_SHORT).show()
            return
        }
        val mapsUrl = "https://maps.google.com/?q=${point.lat},${point.lng}"
        val message = getString(R.string.viewer_map_share_text, mapsUrl)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.viewer_map_share)))
    }

    private fun openInMapsApp() {
        val point = latestLocation
        if (point == null) {
            Toast.makeText(this, R.string.viewer_map_waiting, Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse("geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun onStart() {
        super.onStart()
        refreshHandler.post(refreshRunnable)
    }

    override fun onStop() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onStop()
    }

    override fun onDestroy() {
        locationListener?.let {
            FirebaseDatabase.getInstance().getReference("families/$familyCode/location")
                .removeEventListener(it)
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ViewerMap"
    }
}
