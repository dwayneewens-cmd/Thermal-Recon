package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thermal_reports")
data class ThermalReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val maxTemp: Float,
    val minTemp: Float,
    val avgTemp: Float,
    val centerTemp: Float,
    val paletteName: String,
    val signatureDetected: String,
    val droneModel: String,
    val altitudeMeters: Float,
    val gpsCoordinates: String,
    val notes: String = "",
    val alertTriggered: Boolean = false,
    val imageBase64: String? = null // Optional compressed thumbnail
)
