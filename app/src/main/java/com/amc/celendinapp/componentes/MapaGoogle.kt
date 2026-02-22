package com.amc.celendinapp.componentes

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // Efecto para encuadrar los suministros filtrados o el seleccionado
    LaunchedEffect(clientes, clienteSeleccionado) {
        if (clienteSeleccionado != null) {
            val lat = clienteSeleccionado.latitud?.toDoubleOrNull()
            val lon = clienteSeleccionado.longitud?.toDoubleOrNull()
            if (lat != null && lon != null) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 18f)
                )
            }
        } else if (clientes.isNotEmpty()) {
            val builder = LatLngBounds.builder()
            var hayCoordenadas = false
            clientes.forEach { cl ->
                val lat = cl.latitud?.toDoubleOrNull()
                val lon = cl.longitud?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    builder.include(LatLng(lat, lon))
                    hayCoordenadas = true
                }
            }
            if (hayCoordenadas) {
                val bounds = builder.build()
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngBounds(bounds, 150),
                    durationMs = 1000
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = miUbicacion != null,
                mapType = tipoMapa
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = true
            )
        ) {
            clientes.forEach { cliente ->
                val lat = cliente.latitud?.toDoubleOrNull()
                val lon = cliente.longitud?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    val esVisitado = visitadosIds.contains(cliente.codigoSuministro ?: "")
                    val esElSeleccionado = cliente.codigoSuministro == clienteSeleccionado?.codigoSuministro
                    
                    val markerState = rememberMarkerState(key = cliente.codigoSuministro, position = LatLng(lat, lon))
                    
                    // Abrir descripción automáticamente si es el seleccionado
                    if (esElSeleccionado) {
                        LaunchedEffect(Unit) {
                            markerState.showInfoWindow()
                        }
                    }

                    Marker(
                        state = markerState,
                        title = "SUM: ${cliente.codigoSuministro ?: ""}",
                        snippet = "${cliente.nombres ?: ""} ${cliente.apellidoPaterno ?: ""}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when {
                                esElSeleccionado -> BitmapDescriptorFactory.HUE_AZURE // Azul para el buscado individualmente
                                esVisitado -> BitmapDescriptorFactory.HUE_GREEN      // Verde para visitados
                                else -> BitmapDescriptorFactory.HUE_RED              // Rojo para pendientes
                            }
                        ),
                        onClick = {
                            false // Google Maps abrirá el InfoWindow automáticamente
                        },
                        onInfoWindowLongClick = {
                            clienteParaDialogo = cliente
                        }
                    )
                }
            }
        }
        
        // Botones superiores con padding para no tapar nada
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SmallFloatingActionButton(
                onClick = onDismiss,
                containerColor = Color.White
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar Mapa")
            }

            SmallFloatingActionButton(
                onClick = { 
                    tipoMapa = if (tipoMapa == MapType.SATELLITE) MapType.NORMAL else MapType.SATELLITE 
                },
                containerColor = Color.White
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Cambiar Tipo de Mapa")
            }
        }

        // Diálogo para marcar como visitado
        clienteParaDialogo?.let { cliente ->
            val esVisitado = visitadosIds.contains(cliente.codigoSuministro ?: "")
            AlertDialog(
                onDismissRequest = { clienteParaDialogo = null },
                title = { Text("Gestión de Visita") },
                text = { 
                    Column {
                        Text("Suministro: ${cliente.codigoSuministro ?: ""}", fontWeight = FontWeight.Bold)
                        Text("Beneficiario: ${cliente.nombres ?: ""} ${cliente.apellidoPaterno ?: ""}")
                        Spacer(Modifier.height(8.dp))
                        Text("¿Deseas marcar como ${if (esVisitado) "PENDIENTE" else "VISITADO"}?")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onToggleVisita(cliente.codigoSuministro ?: "")
                            clienteParaDialogo = null
                        },
                        colors = if (esVisitado) ButtonDefaults.buttonColors(containerColor = Color.Red) 
                                 else ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                    ) {
                        Text(if (esVisitado) "QUITAR VISITA" else "MARCAR VISITADO")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { clienteParaDialogo = null }) {
                        Text("CANCELAR")
                    }
                }
            )
        }
    }
}
