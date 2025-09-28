package com.example.despachodistribuidora

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.despachodistribuidora.ui.theme.DespachoDistribuidoraTheme
import kotlin.math.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val distanceText = mutableStateOf("Calculando distancia hacia Plaza de Armas de Chillán...")

        // Comprobar permisos (API 21+)
        if (locationPermissionGranted()) {
            obtainLocationAndCalculateDistance { distanceKm ->
                distanceText.value = "Distancia dispositivo → bodega: %.4f km".format(distanceKm)
            }
        } else {
            // Como API 21 no soporta checkSelfPermission dinámico, solo mostramos mensaje
            distanceText.value = "No se tiene permiso de ubicación. Habilítalo en el dispositivo/emulador."
            Log.w("Haversine", "Permiso de ubicación no disponible en API <23. Usa Ajustes para habilitarlo.")
        }

        // UI Compose mínima para que la pantalla no aparezca negra
        setContent {
            DespachoDistribuidoraTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { padding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = distanceText.value,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                )
            }
        }
    }

    // Verificar permisos usando ContextCompat (solo sirve en API 23+, aquí es informativo)
    private fun locationPermissionGranted(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    // Convertir grados a radianes
    private fun convertirARadianes(valor: Double) = Math.toRadians(valor)

    // Fórmula Haversine
    private fun haversineDistanceKm(
        lat1Deg: Double, lon1Deg: Double,
        lat2Deg: Double, lon2Deg: Double
    ): Double {
        val R = 6371.0
        val dLat = convertirARadianes(lat2Deg - lat1Deg)
        val dLon = convertirARadianes(lon2Deg - lon1Deg)
        val lat1 = convertirARadianes(lat1Deg)
        val lat2 = convertirARadianes(lat2Deg)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        return R * c
    }

    @SuppressLint("MissingPermission")
    private fun obtainLocationAndCalculateDistance(callback: (Double) -> Unit) {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Intentar lastKnownLocation GPS primero
        var location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                processLocation(loc, callback)
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        if (location != null) {
            processLocation(location, callback)
        } else {
            // Solicitar actualizaciones para obtener ubicación
            try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}
            try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}
        }
    }

    private fun processLocation(location: Location, callback: (Double) -> Unit) {
        val deviceLat = location.latitude
        val deviceLon = location.longitude
        val plazaLat = -36.6066
        val plazaLon = -72.1034

        val distanceKm = haversineDistanceKm(deviceLat, deviceLon, plazaLat, plazaLon)

        Log.i(
            "Haversine",
            "Distancia dispositivo -> bodega (km): %.4f (desde $deviceLat,$deviceLon hasta $plazaLat,$plazaLon)".format(distanceKm)
        )
        callback(distanceKm)
    }
}
