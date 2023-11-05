package ru.spektrit.androidsensorsdemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.spektrit.androidsensorsdemo.database.SensorValuesDatabase
import ru.spektrit.androidsensorsdemo.ui.theme.AndroidSensorsDemoTheme
import ru.spektrit.androidsensorsdemo.util.LOCATION_RETRIEVAL_INTERVAL
import ru.spektrit.androidsensorsdemo.util.MILLIS_500
import ru.spektrit.androidsensorsdemo.util.RESOLUTION_REQUEST_CODE
import ru.spektrit.androidsensorsdemo.util.SensorType


class MainActivity : ComponentActivity(), SensorEventListener {
   // Обьявление переменной менеджера сенсора
   private lateinit var sensorManager : SensorManager

   // Клиент локации
   private lateinit var fusedLocationClient: FusedLocationProviderClient

   // Flow хранящее в себе данные о локации пользователя
   private val currentLocationFlow: MutableStateFlow<Location?> = MutableStateFlow(null)

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

   private val sensorFlows = mapOf(
      SensorType.ACCELEROMETER        to accelerometerFlow,
      SensorType.ORIENTATION          to orientationFlow,
      SensorType.GYROSCOPE            to gyroscopeFlow,
      SensorType.MAGNETIC_FIELD       to magneticFieldFlow,
      SensorType.GRAVITY              to gravityFlow,
      SensorType.GEOMAGNETIC_ROTATION to geomagneticRotationFlow,
      SensorType.ROTATION_VECTOR      to rotationVectorFlow,
      SensorType.COMPUTED_ORIENTATION to computedOrientationFlow
   )

   @SuppressLint("MissingPermission")
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      // Инициализация менеджера сенсоров
      sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

      // Инициализация клиента локации
      fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

      logAvailableSensors()

      // Запрос разрешений на геолокацию и старт ее отслеживания при успешной выдаче разрешений
      val locationPermissionRequest = registerForActivityResult(
         ActivityResultContracts.RequestMultiplePermissions()
      ) { permissions ->
         when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
               setupLocation()
               return@registerForActivityResult
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
               setupLocation()
               return@registerForActivityResult
            } else -> { /* TODO */ }
         }
      }
      locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))

      val db = SensorValuesDatabase.getInstance(this)
      val dao = db.sensorValuesDao

      // UI
      setContent {
         AndroidSensorsDemoTheme {
            val viewModel : SensorViewModel = viewModel()
            viewModel.startSavingSensorsData(sensorFlows)
            viewModel.pinDao(dao)

            Surface(
               modifier = Modifier.fillMaxSize(),
               color = MaterialTheme.colorScheme.background
            ) {
               val currentLocationState           = currentLocationFlow.asStateFlow().collectAsState()
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
                  Text(textAlign = TextAlign.Center, text = "Локация\n${currentLocationState.value?.latitude} -- ${currentLocationState.value?.longitude}")
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
    * Метод запускающий отслеживание локации устройства
    */
   @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
   private fun setupLocation() {
      val locationRequest = LocationRequest.Builder(
         Priority.PRIORITY_HIGH_ACCURACY,
         LOCATION_RETRIEVAL_INTERVAL
      ).build()
      val client = LocationServices.getSettingsClient(this)
      val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
      val checkLocationSettingsTask = client.checkLocationSettings(builder.build())

      checkLocationSettingsTask.addOnSuccessListener {
         val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
               super.onLocationResult(locationResult)
               for (location in locationResult.locations) {
                  currentLocationFlow.value = location
                  Log.i("CurrentLocation", "${currentLocationFlow.value?.latitude} -- ${currentLocationFlow.value?.longitude}")
               }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
               super.onLocationAvailability(locationAvailability)
               Log.i("LocationAvailability", "${locationAvailability.isLocationAvailable}")
            }
         }
         fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
      }

      checkLocationSettingsTask.addOnFailureListener { exception ->
         if (exception is ResolvableApiException){
            try { exception.startResolutionForResult(this@MainActivity, RESOLUTION_REQUEST_CODE)
            } catch (sendEx: IntentSender.SendIntentException) {/* Игнорируем ошибку */}
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
    * @param event [SensorEvent] объект который содержит данные о сенсоре и что с ним произошло
    */
   override fun onSensorChanged(event: SensorEvent?) {
      if (event != null) {
         when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
               accelerometerFlow.value = floatArrayOf(event.values[0], event.values[1], event.values[2])
               Log.i("Accelerometer", "${accelerometerFlow.value[0]} ${accelerometerFlow.value[1]} ${accelerometerFlow.value[2]}")
            }
            Sensor.TYPE_ORIENTATION -> {
               orientationFlow.value = floatArrayOf(event.values[0], event.values[1], event.values[2])
               Log.i("Orientation", "${orientationFlow.value[0]} ${orientationFlow.value[1]} ${orientationFlow.value[2]}")
            }
            Sensor.TYPE_GYROSCOPE -> {
               gyroscopeFlow.value = floatArrayOf(event.values[0], event.values[1], event.values[2])
               Log.i("Gyroscope", "${gyroscopeFlow.value[0]} ${gyroscopeFlow.value[1]} ${gyroscopeFlow.value[2]}")
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
               magneticFieldFlow.value = floatArrayOf(event.values[0], event.values[1], event.values[2])
               Log.i("MagneticField", "${magneticFieldFlow.value[0]} ${magneticFieldFlow.value[1]} ${magneticFieldFlow.value[2]}")
            }
            Sensor.TYPE_GRAVITY -> {
               gravityFlow.value = floatArrayOf(event.values[0], event.values[1], event.values[2])
               Log.i("Gravity", "${gravityFlow.value[0]} ${gravityFlow.value[1]} ${gravityFlow.value[2]}")
            }
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
               geomagneticRotationFlow.value = floatArrayOf(event.values[0], event.values[1], event.values[2])
               Log.i("GeomagneticRotation", "${geomagneticRotationFlow.value[0]} ${geomagneticRotationFlow.value[1]} ${geomagneticRotationFlow.value[2]}")
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
               rotationVectorFlow.value = floatArrayOf(event.values[0], event.values[1], event.values[2], event.values[3])
               Log.i("RotationVector", "${rotationVectorFlow.value[0]} ${rotationVectorFlow.value[1]} ${rotationVectorFlow.value[2]} ${rotationVectorFlow.value[3]}")
            }
         }
      }
      try { computedOrientationFlow.value = computeDeviceOrientation() }
      catch (e : Exception) { /* Обработка исключения */ }
   }


   override fun onAccuracyChanged(p0: Sensor?, p1: Int) { /* Колбек если точность сенсора изменилась */ }
}