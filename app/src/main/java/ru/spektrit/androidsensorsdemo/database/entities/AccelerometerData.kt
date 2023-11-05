package ru.spektrit.androidsensorsdemo.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("accelerator")
data class AccelerometerData(
   @PrimaryKey(autoGenerate = true) val id : Int = 0,
   val time : String,
   val x : Float,
   val y : Float,
   val z : Float
)
