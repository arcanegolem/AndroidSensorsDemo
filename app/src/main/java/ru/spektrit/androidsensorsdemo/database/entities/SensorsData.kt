package ru.spektrit.androidsensorsdemo.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("sensors_data")
data class SensorsData(
   @PrimaryKey(autoGenerate = true) val id : Int = 0,
   val time : String,
   val latitude : Double,
   val longitude : Double,
   val altitude : Double,
   val gpsAccuracy : Float,
   val gpsSpeed : Float,
   val gpsSpeedAccuracyMetSec : Float,
   val gpsBearing : Float,
   val gpsBearingAccuracyDeg : Float,
   val accelerometer : String,
   val orientation : String,
   val gyroscope : String,
   val magneticField : String,
   val gravity : String,
   val geomagneticRotation : String,
   val rotationVector : String,
   val computedOrientation: String
)
