package com.amc.celendinapp.componentes

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amc.celendinapp.R
import com.amc.celendinapp.model.Cliente
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

@Composable
fun MapaGoogle(
    miUbicacion: Location?,
    clientes: List<Cliente>,
    visitadosIds: Set<String>,
    clienteSeleccionado: Cliente? = null,
    onToggleVisita: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val centroCelendin = LatLng(-6.8703, -78.1517)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centroCelendin, 10.5f)
    }

    var clienteParaDialogo by remember { mutableStateOf<Cliente?>(null) }
    var tipoMapa by remember { mutableStateOf(MapType.SATELLITE) }

    // EFECTO DE CÁMARA
    LaunchedEffect(clientes, clienteSeleccionado) {
        try {
            if (clienteSeleccionado != null) {
                val lat = clienteSeleccionado.latitud?.replace(",", ".")?.toDoubleOrNull()
                val lon = clienteSeleccionado.longitud?.replace(",", ".")?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 18f))
                }
            } else if (clientes.isNotEmpty()) {
                val builder = LatLngBounds.builder()
                var hayCoordenadas = false
                clientes.forEach { cl ->
                    val lat = cl.latitud?.replace(",", ".")?.toDoubleOrNull()
                    val lon = cl.longitud?.replace(",", ".")?.toDoubleOrNull()
                    if (lat != null && lon != null) {
                        builder.include(LatLng(lat, lon))
                        hayCoordenadas = true
                    }
                }
                if (hayCoordenadas) {
                    cameraPositionState.animate(update = CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
                }
            }
        } catch (e: Exception) { }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = miUbicacion != null, mapType = tipoMapa),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = true)
        ) {
            clientes.forEach { cliente ->
                val lat = cliente.latitud?.replace(",", ".")?.toDoubleOrNull()
                val lon = cliente.longitud?.replace(",", ".")?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    val esVisitado = visitadosIds.contains(cliente.codigoSuministro ?: "")
                    val esElSeleccionado = cliente.codigoSuministro == clienteSeleccionado?.codigoSuministro
                    
                    val nombreFull = listOfNotNull(cliente.nombres, cliente.apellidoPaterno, cliente.apellidoMaterno)
                        .filter { it.isNotBlank() }.joinToString(" ")

                    Marker(
                        state = rememberMarkerState(key = cliente.codigoSuministro, position = LatLng(lat, lon)),
                        title = "SUM: ${cliente.codigoSuministro?.removePrefix("KMZ-") ?: ""}",
                        snippet = "${nombreFull.uppercase()} | ${cliente.localidad?.uppercase() ?: ""}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when {
                                esElSeleccionado -> BitmapDescriptorFactory.HUE_AZURE
                                esVisitado -> BitmapDescriptorFactory.HUE_GREEN
                                else -> BitmapDescriptorFactory.HUE_RED
                            }
                        ),
                        onInfoWindowLongClick = { clienteParaDialogo = cliente }
                    )
                }
            }
        }
        
        // Botones superiores (Ahora aparecen inmediatamente)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmallFloatingActionButton(onClick = onDismiss, containerColor = Color.White, contentColor = Color.Black) { 
                Icon(Icons.Default.Close, null) 
            }
            SmallFloatingActionButton(
                onClick = { tipoMapa = if (tipoMapa == MapType.SATELLITE) MapType.NORMAL else MapType.SATELLITE }, 
                containerColor = Color.White, 
                contentColor = Color.Black
            ) {
                Icon(painterResource(id = R.drawable.ic_capa), null, modifier = Modifier.size(20.dp))
            }
        }

        // Diálogo para gestionar visita
        clienteParaDialogo?.let { cliente ->
            val esVisitado = visitadosIds.contains(cliente.codigoSuministro ?: "")
            AlertDialog(
                onDismissRequest = { clienteParaDialogo = null },
                title = { Text("Gestión de Visita") },
                text = { 
                    Column {
                        Text("Suministro: ${cliente.codigoSuministro}", fontWeight = FontWeight.Bold)
                        Text("¿Deseas marcar como ${if (esVisitado) "PENDIENTE" else "VISITADO"}?")
                    }
                },
                confirmButton = {
                    Button(onClick = { onToggleVisita(cliente.codigoSuministro ?: ""); clienteParaDialogo = null },
                        colors = if (esVisitado) ButtonDefaults.buttonColors(containerColor = Color.Red) else ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))) {
                        Text(if (esVisitado) "QUITAR VISITA" else "MARCAR VISITADO")
                    }
                },
                dismissButton = { TextButton(onClick = { clienteParaDialogo = null }) { Text("CANCELAR") } }
            )
        }
    }
}
