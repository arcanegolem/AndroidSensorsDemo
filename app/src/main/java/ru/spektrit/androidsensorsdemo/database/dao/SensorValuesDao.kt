package ru.spektrit.androidsensorsdemo.database.dao

import androidx.room.Dao
import androidx.room.Insert
import ru.spektrit.androidsensorsdemo.database.entities.AccelerometerData
import ru.spektrit.androidsensorsdemo.database.entities.OrientationData

@Dao
interface SensorValuesDao {
   @Insert suspend fun recordData(accelerometerData: AccelerometerData)
   @Insert suspend fun recordData(orientationData: OrientationData)
   // TODO: Добавить все сенсоры
}