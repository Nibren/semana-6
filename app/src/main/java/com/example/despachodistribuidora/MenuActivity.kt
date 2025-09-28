package com.example.despachodistribuidora

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
import com.example.despachodistribuidora.ui.theme.DespachoDistribuidoraTheme
import kotlin.math.*

class MenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DespachoDistribuidoraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    val distanceText = remember { mutableStateOf("Calculando distancia hacia Plaza de Armas de Chillán...") }

                    // Pedir ubicación y calcular distancia
                    ObtainLocation(distanceText)

                    MenuScreen(distanceText.value, Modifier.padding(padding))
                }
            }
        }
    }

    @Composable
    fun MenuScreen(distance: String, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = distance,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }

    @Composable
    private fun ObtainLocation(distanceText: MutableState<String>) {
        LaunchedEffect(Unit) {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

            @SuppressLint("MissingPermission")
            fun requestLocation() {
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        try { locationManager.removeUpdates(this) } catch (_: Exception) {}
                        val plazaLat = -36.6066
                        val plazaLon = -72.1034
                        val distanceKm = haversineDistanceKm(loc.latitude, loc.longitude, plazaLat, plazaLon)
                        distanceText.value = "Distancia dispositivo → bodega: %.4f km".format(distanceKm)
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }

                // Intentar last known
                var location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location == null) location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

                if (location != null) {
                    val plazaLat = -36.6066
                    val plazaLon = -72.1034
                    val distanceKm = haversineDistanceKm(location.latitude, location.longitude, plazaLat, plazaLon)
                    distanceText.value = "Distancia dispositivo → bodega: %.4f km".format(distanceKm)
                } else {
                    try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}
                    try { locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener) } catch (_: Exception) {}
                }
            }

            requestLocation()
        }
    }

    private fun convertirARadianes(valor: Double) = Math.toRadians(valor)

    private fun haversineDistanceKm(lat1Deg: Double, lon1Deg: Double, lat2Deg: Double, lon2Deg: Double): Double {
        val R = 6371.0
        val dLat = convertirARadianes(lat2Deg - lat1Deg)
        val dLon = convertirARadianes(lon2Deg - lon1Deg)
        val lat1 = convertirARadianes(lat1Deg)
        val lat2 = convertirARadianes(lat2Deg)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1.0 - a))
        return R * c
    }
}
