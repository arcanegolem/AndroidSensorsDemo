package ru.spektrit.androidsensorsdemo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import ru.spektrit.androidsensorsdemo.database.entities.SensorsData

@Dao
interface SensorValuesDao {
   @Insert suspend fun recordSensorsData(data : SensorsData)
}