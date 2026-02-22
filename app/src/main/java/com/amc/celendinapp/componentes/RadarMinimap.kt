package com.amc.celendinapp.componentes

import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.amc.celendinapp.model.Cliente
import kotlinx.coroutines.delay

@Composable
fun RadarMinimap(
    miUbicacion: Location,
    clientesCercanos: List<Cliente>,
    visitadosIds: Set<String>,
    onDismiss: () -> Unit
) {
    val radioMaximoMetros = 2000f // 2 Kilómetros
    var clienteSeleccionado by remember { mutableStateOf<Cliente?>(null) }
    var rotacionSuave by remember { mutableFloatStateOf(miUbicacion.bearing) }

    // Actualización cada 3 segundos
    LaunchedEffect(miUbicacion) {
        while(true) {
            rotacionSuave = miUbicacion.bearing
            delay(3000)
        }
    }

    var escala by remember { mutableFloatStateOf(1f) }
    var offsetMapa by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        escala = (escala * zoomChange).coerceIn(1f, 5f)
        offsetMapa += offsetChange
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = state)
                .pointerInput(escala, offsetMapa) {
                    detectTapGestures { tapOffset ->
                        val centroX = (size.width / 2f) + offsetMapa.x
                        val centroY = (size.height / 2f) + offsetMapa.y
                        val radioVisual = (size.width.coerceAtMost(size.height) / 2.2f) * escala

                        clientesCercanos.forEach { cliente ->
                            val latCl = cliente.latitud?.toDoubleOrNull()
                            val lonCl = cliente.longitud?.toDoubleOrNull()
                            
                            if (latCl != null && lonCl != null) {
                                val dist = FloatArray(1)
                                Location.distanceBetween(miUbicacion.latitude, miUbicacion.longitude, latCl, lonCl, dist)

                                if (dist[0] <= radioMaximoMetros) {
                                    val bearingAlCliente = miUbicacion.bearingTo(Location("").apply { latitude = latCl; longitude = lonCl })
                                    val anguloFinal = bearingAlCliente - rotacionSuave
                                    val anguloRad = Math.toRadians((anguloFinal - 90).toDouble())
                                    val distV = (dist[0] / radioMaximoMetros) * radioVisual
                                    val cX = centroX + (distV * Math.cos(anguloRad)).toFloat()
                                    val cY = centroY + (distV * Math.sin(anguloRad)).toFloat()

                                    val dx = tapOffset.x - cX
                                    val dy = tapOffset.y - cY
                                    if (Math.sqrt((dx * dx + dy * dy).toDouble()) < 50.0) {
                                        clienteSeleccionado = cliente
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            val centroX = (size.width / 2f) + offsetMapa.x
            val centroY = (size.height / 2f) + offsetMapa.y
            val radioVisualBase = size.width.coerceAtMost(size.height) / 2.2f
            val radioVisual = radioVisualBase * escala

            // 1. Guías (Círculos cada 500m)
            for (i in 1..4) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    radius = radioVisual * (i * 0.25f),
                    center = Offset(centroX, centroY),
                    style = Stroke(width = 1f)
                )
            }

            // 2. Norte
            val northLine = radioVisual + 40f
            drawLine(Color.Red.copy(alpha = 0.4f), Offset(centroX, centroY), Offset(centroX, centroY - northLine), 3f)

            // 3. Dibujar Clientes
            clientesCercanos.forEach { cliente ->
                val latCl = cliente.latitud?.toDoubleOrNull()
                val lonCl = cliente.longitud?.toDoubleOrNull()

                if (latCl != null && lonCl != null) {
                    val dist = FloatArray(1)
                    Location.distanceBetween(miUbicacion.latitude, miUbicacion.longitude, latCl, lonCl, dist)

                    if (dist[0] <= radioMaximoMetros) {
                        val bearingAlCliente = miUbicacion.bearingTo(Location("").apply { latitude = latCl; longitude = lonCl })
                        val anguloFinal = bearingAlCliente - rotacionSuave
                        val anguloRad = Math.toRadians((anguloFinal - 90).toDouble())
                        val distV = (dist[0] / radioMaximoMetros) * radioVisual
                        val posX = centroX + (distV * Math.cos(anguloRad)).toFloat()
                        val posY = centroY + (distV * Math.sin(anguloRad)).toFloat()

                        val esVisitado = visitadosIds.contains(cliente.codigoSuministro ?: "")

                        drawCircle(
                            color = if (esVisitado) Color(0xFF2ECC71) else Color.Red,
                            radius = 7.dp.toPx(),
                            center = Offset(posX, posY)
                        )

                        if (escala > 1.5f) {
                            drawContext.canvas.nativeCanvas.drawText(
                                cliente.codigoSuministro ?: "S/N",
                                posX, posY - 35f,
                                Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    textSize = 26f
                                    textAlign = Paint.Align.CENTER
                                    typeface = Typeface.DEFAULT_BOLD
                                }
                            )
                        }
                    }
                }
            }

            // 4. Técnico (Punto Azul)
            drawCircle(
                color = Color(0xFF2980B9),
                radius = 10.dp.toPx(),
                center = Offset(centroX, centroY)
            )
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(centroX, centroY),
                style = Stroke(4f)
            )
        }

        // Botones Superiores
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            SmallFloatingActionButton(onClick = onDismiss, containerColor = Color(0xFFE74C3C)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
            if (escala != 1f || offsetMapa != Offset.Zero) {
                SmallFloatingActionButton(onClick = { escala = 1f; offsetMapa = Offset.Zero }, containerColor = Color.White) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                }
            }
        }
    }

    // Modal de Información
    if (clienteSeleccionado != null) {
        AlertDialog(
            onDismissRequest = { clienteSeleccionado = null },
            title = { Text("Suministro: ${clienteSeleccionado?.codigoSuministro ?: ""}") },
            text = { Text("Nombre: ${clienteSeleccionado?.nombres ?: ""} ${clienteSeleccionado?.apellidoPaterno ?: ""}") },
            confirmButton = { TextButton(onClick = { clienteSeleccionado = null }) { Text("OK") } }
        )
    }
}
