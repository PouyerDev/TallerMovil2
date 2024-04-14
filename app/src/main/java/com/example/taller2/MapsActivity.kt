package com.example.taller2

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.core.content.ContextCompat
import android.os.Environment
import android.os.Handler
import android.view.View
import android.view.inputmethod.EditorInfo
import com.example.taller2.databinding.ActivityMapsBinding
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.api.IGeoPoint
import org.osmdroid.events.MapEventsReceiver
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.overlay.Marker
import java.io.IOException
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.MapEventsOverlay


class MapsActivity : AppCompatActivity(), SensorEventListener {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map: MapView
    private lateinit var locationOverlay: MyLocationNewOverlay
    private var lastKnownLocation: GeoPoint? = null
    private var jsonArray = JSONArray()

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private var isDarkMode = false
    private lateinit var binding: ActivityMapsBinding
    private var mGeocoder: Geocoder? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGeocoder = Geocoder(this)

        // Sensor de luz
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!

        // Pedir permiso
        askPermiso()

        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        map = findViewById<MapView>(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)


        // Establecer el zoom inicial del mapa
        map.controller.setZoom(5)

        // Agregar overlay para mostrar la ubicación del usuario
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        locationOverlay.enableMyLocation()
        map.overlays.add(locationOverlay)

        centerOnUserLocation()
        locationOverlay.enableFollowLocation()

        //buscar y longclick
        buscar()
        longClick()


    }

    private fun longClick() {
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                addMarkerWithAddress(p)
                return false
            }
        })

        map.overlays.add(0, mapEventsOverlay)
    }

    private fun addMarkerWithAddress(geoPoint: IGeoPoint) {
        val addresses = try {
            mGeocoder?.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        val address = addresses?.getOrNull(0)
        val marker = Marker(map)
        marker.position = geoPoint as GeoPoint?
        marker.title = address?.getAddressLine(0) ?: "Ubicación"
        map.overlays.add(marker)
        map.invalidate()

        // Calcular la distancia entre la posición actual y el marcador
        val distance = lastKnownLocation?.distanceToAsDouble(geoPoint) ?: 0.0
        val distanceInMeters = String.format(Locale.getDefault(), "%.2f", distance)

        // Mostrar un Toast con la distancia
        Toast.makeText(
            this,
            "Distancia al marcador: $distanceInMeters metros",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun buscar() {
        binding.etLocation.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val addressString = binding.etLocation.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        val addresses = Geocoder(this).getFromLocationName(addressString, 2)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val addressResult = addresses[0]
                            val position = GeoPoint(addressResult.latitude, addressResult.longitude)

                            // Create a new marker
                            val marker = Marker(map)
                            marker.position = position
                            marker.title = addressResult.getAddressLine(0)
                            map.overlays.add(marker)

                            // Move the map's view to the marker
                            map.controller.animateTo(position)

                            // Calcular la distancia entre la posición actual y el marcador
                            val distance = lastKnownLocation?.distanceToAsDouble(position) ?: 0.0
                            val distanceInMeters =
                                String.format(Locale.getDefault(), "%.2f", distance)

                            // Mostrar un Toast con la distancia
                            Toast.makeText(
                                this,
                                "Distancia al marcador: $distanceInMeters metros",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "La dirección está vacía", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
        locationOverlay.enableMyLocation()
        centerOnUserLocation()

        // Registrar el sensor de luz
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)

    }

    override fun onPause() {
        super.onPause()
        map.onPause()
        locationOverlay.disableMyLocation()

        // Detener la escucha del sensor de luz
        mSensorManager.unregisterListener(this)
    }

    private fun askPermiso() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permiso concedido, no hacer nada adicional
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE
                )
            }

            else -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permiso concedido, no hacer nada adicional
                } else {
                    finish()
                    Toast.makeText(this, "Funcionalidades reducidas", Toast.LENGTH_LONG).show()
                }
                return
            }

            else -> {
                // Ignorar todas las demás solicitudes
            }
        }
    }

    private fun centerOnUserLocation() {
        val handler = Handler()
        handler.postDelayed({
            val lastLocation = locationOverlay.myLocation
            if (lastLocation != null) {
                if (lastKnownLocation != null) {
                    val distance = lastKnownLocation!!.distanceToAsDouble(lastLocation)
                    if (distance > 30) {
                        saveLocationToJson(lastLocation)
                        lastKnownLocation = lastLocation
                    }
                } else {
                    lastKnownLocation = lastLocation
                }

                map.controller.setZoom(18)
                map.controller.setCenter(lastLocation)
            } else {
                Toast.makeText(this, "Buscando ubicación...", Toast.LENGTH_SHORT).show()
            }
        }, 1000)
    }

    private fun saveLocationToJson(location: GeoPoint) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDateAndTime: String = dateFormat.format(Date())

        val locationData = LocationData(location.latitude, location.longitude, currentDateAndTime)
        val jsonData = JSONObject()
        jsonData.put("latitude", locationData.latitude)
        jsonData.put("longitude", locationData.longitude)
        jsonData.put("timestamp", locationData.timestamp)

        jsonArray.put(jsonData)

        try {
            val directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = File(directory, "location_data.json")
            val fileWriter = FileWriter(file)
            fileWriter.write(jsonArray.toString())
            fileWriter.close()
            Toast.makeText(this, "Ubicación guardada en $file", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()

            Toast.makeText(this, "Error al guardar la ubicación", Toast.LENGTH_LONG).show()
        }
    }

    data class LocationData(val latitude: Double, val longitude: Double, val timestamp: String)

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se necesita implementar
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Detectar cambios en la luminosidad
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            // Cambiar entre los modos claro y oscuro según la luminosidad
            if (event.values[0] < 100 && !isDarkMode) { // Cambia el 10 a tu umbral deseado
                isDarkMode = true
                changeMapStyle("oscuro") // Cambia al estilo oscuro
            } else if (event.values[0] >= 100 && isDarkMode) {
                isDarkMode = false
                changeMapStyle("claro") // Cambia al estilo claro
            }
        }
    }

    fun changeMapStyle(string: String) {
        when (string) {
            "claro" -> {
                map.setTileSource(TileSourceFactory.MAPNIK)
            }

            "oscuro" -> {
                map.setTileSource(
                    object : OnlineTileSourceBase(
                        "Mapbox Dark", 0, 19, 256, ".png",
                        arrayOf("https://umap.openstreetmap.fr/en/map/anonymous-edit/1054581:y-mlvCOKOA_FuvPW9zF8xwy96niBiKSQeLm05JmBU_Q")
                    ) {
                        override fun getTileURLString(pMapTileIndex: Long): String {
                            return baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
                                    MapTileIndex.getX(pMapTileIndex) + "/" +
                                    MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding
                        }
                    }
                )
            }
        }
    }
}
