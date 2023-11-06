package ru.spektrit.androidsensorsdemo.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("sensors_data")
data class SensorsData(
   @PrimaryKey(autoGenerate = true) val id : Int = 0,
   val time : String,
   val accelerometer : String,
   val orientation : String,
   val gyroscope : String,
   val magneticField : String,
   val gravity : String,
   val geomagneticRotation : String,
   val rotationVector : String,
   val computedOrientation: String
)
