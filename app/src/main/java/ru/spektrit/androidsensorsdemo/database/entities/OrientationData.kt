package ru.spektrit.androidsensorsdemo.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("orientation")
data class OrientationData(
   @PrimaryKey(autoGenerate = true) val id : Int = 0,
   val time : String,
   val x : Float,
   val y : Float,
   val z : Float
)
