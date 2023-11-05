package ru.spektrit.androidsensorsdemo

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.spektrit.androidsensorsdemo.database.dao.SensorValuesDao
import ru.spektrit.androidsensorsdemo.database.entities.AccelerometerData
import ru.spektrit.androidsensorsdemo.database.entities.OrientationData
import ru.spektrit.androidsensorsdemo.util.SAVING_INTERVAL_MILLIS
import ru.spektrit.androidsensorsdemo.util.SensorType
import java.util.Calendar

class SensorViewModel() : ViewModel() {
   private var savingJob : Job? = null
   private var dao : SensorValuesDao? = null

   fun pinDao(dao: SensorValuesDao) { this.dao = dao }

   fun startSavingSensorsData(sensorFlows : Map<SensorType, MutableStateFlow<FloatArray>>) {
      savingJob = viewModelScope.launch {
         while (true) {
            val calendar = Calendar.getInstance()
            val currentDateAsString =  "${calendar.get(Calendar.DATE)}-${calendar.get(Calendar.MONTH)}-" +
                  "${calendar.get(Calendar.YEAR)} ${calendar.get(Calendar.HOUR_OF_DAY)}:" +
                  "${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}:" +
                  "${calendar.get(Calendar.MILLISECOND)}"
            Log.i("SensorsDataSaving", "Data saving started! $currentDateAsString")
            saveData(sensorFlows, currentDateAsString)
            delay(SAVING_INTERVAL_MILLIS)
         }
      }
   }

   fun stopSavingSensorsData() {
      savingJob?.cancel()
      savingJob = null
   }

   private suspend fun saveData(
      sensorFlows : Map<SensorType, MutableStateFlow<FloatArray>>,
      currentDate : String
   ){
      val accelerometerData = sensorFlows[SensorType.ACCELEROMETER]!!.value
      dao?.recordData(
         AccelerometerData(
            time = currentDate,
            x = accelerometerData[0],
            y = accelerometerData[1],
            z = accelerometerData[2]
         )
      )
      val orientationData = sensorFlows[SensorType.ORIENTATION]!!.value
      dao?.recordData(
         OrientationData(
            time = currentDate,
            x = orientationData[0],
            y = orientationData[1],
            z = orientationData[2]
         )
      )
   }
}