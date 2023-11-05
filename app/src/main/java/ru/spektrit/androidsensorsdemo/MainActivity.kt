package ru.spektrit.androidsensorsdemo

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.spektrit.androidsensorsdemo.ui.theme.AndroidSensorsDemoTheme
import kotlin.math.PI
import kotlin.math.acos

// Константа для обозначения 500мс в мкс
const val MILLIS_500 = 500000


class MainActivity : ComponentActivity(), SensorEventListener {
   // Обьявление переменной менеджера сенсора
   private lateinit var sensorManager : SensorManager

   // Flow хранящие в себе данные необходимых сенсоров
   // Сила ускорения по осям (XYZ соотвественно) м/с^2
   private val accelerometerFlow       = MutableStateFlow(FloatArray(3))
   // DEPRECATED! Азимут, шаг и вращение соотвественно град, этот сенсор вроде как не поддерживается
   // поэтому лучше рассчитвывать из показаний магнетометра и акселерометра (метод
   // computeDeviceOrientation и Flow computedOrientationFlow)
   private val orientationFlow         = MutableStateFlow(FloatArray(3))
   // Скорость вращения по осям (XYZ соотвественно) радиан/сек
   private val gyroscopeFlow           = MutableStateFlow(FloatArray(3))
   // Сила магнитного поля по осям (XYZ соотвественно) μT
   private val magneticFieldFlow       = MutableStateFlow(FloatArray(3))
   // Сила гравитации по осям (XYZ соотвественно) м/с^2
   private val gravityFlow             = MutableStateFlow(FloatArray(3))
   // Компоненты вектора вращения по осям (XYZ соотвественно)
   private val geomagneticRotationFlow = MutableStateFlow(FloatArray(3))
   // Кватернион вращения (гугл в помощь) (XYZ и скалярный компонент)
   private val rotationVectorFlow      = MutableStateFlow(FloatArray(4))
   // Азимут, шаг и вращение соотвественно град получаемые по данным с акселерометра и магнетометра
   private val computedOrientationFlow = MutableStateFlow(FloatArray(3))


   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      // Инициализация менеджера сенсоров
      sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

      logAvailableSensors()

      // UI
      setContent {
         AndroidSensorsDemoTheme {
            Surface(
               modifier = Modifier.fillMaxSize(),
               color = MaterialTheme.colorScheme.background
            ) {
               val accelerometerValuesState       = accelerometerFlow.asStateFlow().collectAsState()
               val orientationValuesState         = orientationFlow.asStateFlow().collectAsState()
               val gyroscopeValuesState           = gyroscopeFlow.asStateFlow().collectAsState()
               val magneticFieldValuesState       = magneticFieldFlow.asStateFlow().collectAsState()
               val gravityValuesState             = gravityFlow.asStateFlow().collectAsState()
               val geomagneticRotationValuesState = geomagneticRotationFlow.asStateFlow().collectAsState()
               val rotationVectorValuesState      = rotationVectorFlow.asStateFlow().collectAsState()
               val computedOrientationValuesState = computedOrientationFlow.asStateFlow().collectAsState()

               Column(
                  modifier = Modifier.fillMaxSize(),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center
               ) {
                  Text(textAlign = TextAlign.Center, text = "Акселерометр\n${accelerometerValuesState.value[0]}, ${accelerometerValuesState.value[1]}, ${accelerometerValuesState.value[2]}")
                  Text(textAlign = TextAlign.Center, text = "Ориентация (Деприкейтед)\n${orientationValuesState.value[0]}, ${orientationValuesState.value[1]}, ${orientationValuesState.value[2]}")
                  Text(textAlign = TextAlign.Center, text = "Гироскоп\n${gyroscopeValuesState.value[0]}, ${gyroscopeValuesState.value[1]}, ${gyroscopeValuesState.value[2]}")
                  Text(textAlign = TextAlign.Center, text = "Магнитометр\n${magneticFieldValuesState.value[0]}, ${magneticFieldValuesState.value[1]}, ${magneticFieldValuesState.value[2]}")
                  Text(textAlign = TextAlign.Center, text = "Гравитометр\n${gravityValuesState.value[0]}, ${gravityValuesState.value[1]}, ${gravityValuesState.value[2]}")
                  Text(textAlign = TextAlign.Center, text = "Геомагнитное вращение\n${geomagneticRotationValuesState.value[0]}, ${geomagneticRotationValuesState.value[1]}, ${geomagneticRotationValuesState.value[2]}")
                  Text(textAlign = TextAlign.Center, text = "Вектор вращения\n${rotationVectorValuesState.value[0]}, ${rotationVectorValuesState.value[1]}, ${rotationVectorValuesState.value[2]}")
                  Text(textAlign = TextAlign.Center, text = "Ориентация (вычисляемая)\n${computedOrientationValuesState.value[0]}, ${computedOrientationValuesState.value[1]}, ${computedOrientationValuesState.value[2]}")
               }
            }
         }
      }
   }


   /**
    * Функция определения ориентации устройства в пространстве на основе показателей магнетометра и акселерометра
    *
    * @return [FloatArray] объект с углами по осям (XYZ соответственно)
    */
   private fun computeDeviceOrientation() : FloatArray {
      val rotationMatrix = FloatArray(9)
      SensorManager.getRotationMatrix(rotationMatrix, null, magneticFieldFlow.value, accelerometerFlow.value)
      val orientationAngles = FloatArray(3)
      SensorManager.getOrientation(rotationMatrix, orientationAngles)

      return orientationAngles
   }


   /**
    * Вспомогательная функция для отслеживания доступных сенсоров на устройстве при дебаге
    *
    * @return Список сенсоров доступных на устройстве
    */
   private fun logAvailableSensors() : List<Sensor> {
      val deviceSensors : List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)
      Log.i("Available Sensors", "${deviceSensors.map { it.name }}")
      return deviceSensors
   }


   override fun onResume() {
      super.onResume()
      // Тут присваеваем активити статус слушателя значений с сенсоров
      val sensorTypesList = listOf(
         Sensor.TYPE_ACCELEROMETER,
         Sensor.TYPE_ORIENTATION,
         Sensor.TYPE_GYROSCOPE,
         Sensor.TYPE_MAGNETIC_FIELD,
         Sensor.TYPE_GRAVITY,
         Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
         Sensor.TYPE_ROTATION_VECTOR
      )

      sensorTypesList.forEach { sensorType ->
         sensorManager.getDefaultSensor(sensorType)?.also { sensor ->
            sensorManager.registerListener(this, sensor, MILLIS_500, MILLIS_500)
         }
      }
   }


   override fun onPause() {
      super.onPause()
      // Нужно чтоб работало только не в фоне
      sensorManager.unregisterListener(this)
   }


   /**
    * Колбек при изменении значения сенсора, тут только присвоение значений в потоке, вычисления
    * тут в основном делать не стоит, так как эта функция вызывается очень часто даже при редком
    * обновлении данных прослушивания
    *
    * @param p0 [SensorEvent] объект который содержит данные о сенсоре и что с ним произошло
    */
   override fun onSensorChanged(p0: SensorEvent?) {
      if (p0 != null) {
         when (p0.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
               accelerometerFlow.value = floatArrayOf(p0.values[0], p0.values[1], p0.values[2])
               Log.i("Accelerometer", "${accelerometerFlow.value[0]} ${accelerometerFlow.value[1]} ${accelerometerFlow.value[2]}")
            }
            Sensor.TYPE_ORIENTATION -> {
               orientationFlow.value = floatArrayOf(p0.values[0], p0.values[1], p0.values[2])
               Log.i("Orientation", "${orientationFlow.value[0]} ${orientationFlow.value[1]} ${orientationFlow.value[2]}")
            }
            Sensor.TYPE_GYROSCOPE -> {
               gyroscopeFlow.value = floatArrayOf(p0.values[0], p0.values[1], p0.values[2])
               Log.i("Gyroscope", "${gyroscopeFlow.value[0]} ${gyroscopeFlow.value[1]} ${gyroscopeFlow.value[2]}")
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
               magneticFieldFlow.value = floatArrayOf(p0.values[0], p0.values[1], p0.values[2])
               Log.i("MagneticField", "${magneticFieldFlow.value[0]} ${magneticFieldFlow.value[1]} ${magneticFieldFlow.value[2]}")
            }
            Sensor.TYPE_GRAVITY -> {
               gravityFlow.value = floatArrayOf(p0.values[0], p0.values[1], p0.values[2])
               Log.i("Gravity", "${gravityFlow.value[0]} ${gravityFlow.value[1]} ${gravityFlow.value[2]}")
            }
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
               geomagneticRotationFlow.value = floatArrayOf(p0.values[0], p0.values[1], p0.values[2])
               Log.i("GeomagneticRotation", "${geomagneticRotationFlow.value[0]} ${geomagneticRotationFlow.value[1]} ${geomagneticRotationFlow.value[2]}")
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
               rotationVectorFlow.value = floatArrayOf(p0.values[0], p0.values[1], p0.values[2], p0.values[3])
               Log.i("RotationVector", "${rotationVectorFlow.value[0]} ${rotationVectorFlow.value[1]} ${rotationVectorFlow.value[2]} ${rotationVectorFlow.value[3]}")
            }
         }
      }
      try { computedOrientationFlow.value = computeDeviceOrientation() }
      catch (e : Exception) { /* Обработка исключения */ }
   }


   override fun onAccuracyChanged(p0: Sensor?, p1: Int) { /* Колбек если точность сенсора изменилась */ }
}