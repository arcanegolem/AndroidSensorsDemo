package ru.spektrit.androidsensorsdemo

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ru.spektrit.androidsensorsdemo.database.DB_NAME
import ru.spektrit.androidsensorsdemo.database.dao.SensorValuesDao
import ru.spektrit.androidsensorsdemo.database.entities.SensorsData
import ru.spektrit.androidsensorsdemo.util.SAVING_INTERVAL_MILLIS
import ru.spektrit.androidsensorsdemo.util.SensorType
import java.util.Calendar

/**
 * Класс отвечающий за бизнес-логику
 */
class SensorViewModel : ViewModel() {
   private var savingJob : Job? = null
   private var dao : SensorValuesDao? = null

   /**
    * Метод прикрепления DAO объекта к ViewModel
    *
    * @param dao [SensorValuesDao] объект
    */
   fun pinDao( dao: SensorValuesDao ) { this.dao = dao }

   /**
    * Метод старта сохранения данных в БД
    *
    * @param sensorFlows [Map] с ключами [SensorType] и значениями в виде [MutableStateFlow] данных с сенсоров
    */
   fun startSavingSensorsData( sensorFlows : Map<SensorType, MutableStateFlow<FloatArray>> ) {
      savingJob = viewModelScope.launch {
         while (true) {
            val calendar = Calendar.getInstance()
            val currentDateAsString =  "${calendar.get(Calendar.DATE)}-${calendar.get(Calendar.MONTH)}-" +
                  "${calendar.get(Calendar.YEAR)} ${calendar.get(Calendar.HOUR_OF_DAY)}:" +
                  "${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}:" +
                  "${calendar.get(Calendar.MILLISECOND)}"
            Log.i("SensorsDataSaving", "Data saving started! $currentDateAsString")

            val sensorsData = currentLocationFlow.value?.let { location ->
               SensorsData(
                  time = currentDateAsString,
                  latitude = location.latitude,
                  longitude = location.longitude,
                  altitude = location.altitude,
                  gpsAccuracy = location.accuracy,
                  gpsSpeed = location.speed,
                  gpsSpeedAccuracyMetSec = location.speedAccuracyMetersPerSecond,
                  gpsBearing = location.bearing,
                  gpsBearingAccuracyDeg = location.bearingAccuracyDegrees,
                  accelerometer = sensorFlows[SensorType.ACCELEROMETER]!!.value.joinToString(" "),
                  orientation = sensorFlows[SensorType.ORIENTATION]!!.value.joinToString(" "),
                  gyroscope = sensorFlows[SensorType.GYROSCOPE]!!.value.joinToString(" "),
                  magneticField = sensorFlows[SensorType.MAGNETIC_FIELD]!!.value.joinToString(" "),
                  gravity = sensorFlows[SensorType.GRAVITY]!!.value.joinToString(" "),
                  geomagneticRotation = sensorFlows[SensorType.GEOMAGNETIC_ROTATION]!!.value.joinToString(" "),
                  rotationVector = sensorFlows[SensorType.ROTATION_VECTOR]!!.value.joinToString(" "),
                  computedOrientation = sensorFlows[SensorType.COMPUTED_ORIENTATION]!!.value.joinToString(" "),
               )
            }

            saveData(sensorsData)
            delay(SAVING_INTERVAL_MILLIS)
         }
      }
   }

   /**
    * Метод остановки сохранения данных в БД
    */
   fun stopSavingSensorsData() {
      savingJob?.cancel()
      savingJob = null
   }

   /**
    * Логирование пути к локальной БД
    *
    * @param context Объект типа [Context]
    */
   fun logDBPath(context : Context) {
      Log.i("LocalDBPath", context.getDatabasePath(DB_NAME).absolutePath)
   }

   /**
    * Асинхронный метод непосредственного сохранения данных в локальную БД
    */
   private suspend fun saveData( sensorsData : SensorsData? ) {
      if (sensorsData != null) {
         dao?.recordSensorsData(sensorsData)
      }
   }
}