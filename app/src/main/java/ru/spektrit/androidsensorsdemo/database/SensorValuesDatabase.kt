package ru.spektrit.androidsensorsdemo.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ru.spektrit.androidsensorsdemo.database.dao.SensorValuesDao
import ru.spektrit.androidsensorsdemo.database.entities.SensorsData

const val DB_NAME = "sensor_values.db"

@Database(
   entities = [SensorsData::class],
   version = 1
)
abstract class SensorValuesDatabase : RoomDatabase() {
   abstract val sensorValuesDao : SensorValuesDao

   companion object {
      private var INSTANCE : SensorValuesDatabase? = null

      fun getInstance(context: Context) : SensorValuesDatabase {
         if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context, SensorValuesDatabase::class.java, DB_NAME)
               .fallbackToDestructiveMigration()
               .build()
         }

         return INSTANCE as SensorValuesDatabase
      }
   }
}