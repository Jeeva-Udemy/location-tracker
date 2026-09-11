package com.jeeva.locationtracker.model

// No-arg constructor + var properties are required for Firebase Realtime Database deserialization.
data class LocationPoint(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var accuracy: Float = 0f,
    var timestamp: Long = 0L
)
