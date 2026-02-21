package com.amc.celendinapp.componentes

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amc.celendinapp.JsonUtils.abrirMapa
import com.amc.celendinapp.model.Cliente

@Composable
fun TarjetaCliente(
    cliente: Cliente,
    yaVisitado: Boolean,
    miUbicacion: Location?,
    onAction: () -> Unit
) {
    val context = LocalContext.current

    val statusColor = if (yaVisitado) Color(0xFF2ECC71) else Color(0xFF3498DB)
    val cardBg = if (yaVisitado) Color(0xFFF1F9F5) else Color.White

    var distanciaStr = ""
    var orientacionStr = ""

    if (miUbicacion != null) {
        val latCl = cliente.LATITUD2?.toDoubleOrNull() ?: 0.0
        val lonCl = cliente.LONGITUD2?.toDoubleOrNull() ?: 0.0

        val results = FloatArray(1)
        Location.distanceBetween(
            miUbicacion.latitude, miUbicacion.longitude,
            latCl, lonCl, results
        )

        val metros = results[0]
        distanciaStr = if (metros > 1000) "%.1f km".format(metros / 1000) else "${metros.toInt()} m"

        val latDif = latCl - miUbicacion.latitude
        val lonDif = lonCl - miUbicacion.longitude

        orientacionStr = when {
            latDif > 0.0001 && Math.abs(lonDif) < 0.0001 -> "al Norte ↑"
            latDif < -0.0001 && Math.abs(lonDif) < 0.0001 -> "al Sur ↓"
            lonDif > 0.0001 && Math.abs(latDif) < 0.0001 -> "al Este →"
            lonDif < -0.0001 && Math.abs(latDif) < 0.0001 -> "al Oeste ←"
            latDif > 0 && lonDif > 0 -> "al Noreste ↗"
            latDif > 0 && lonDif < 0 -> "al Noroeste ↖"
            latDif < 0 && lonDif > 0 -> "al Sureste ↘"
            else -> "al Suroeste ↙"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(statusColor))

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SUM: ${cliente.CÓDIGO_DE_SUMINISTRO2}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor.copy(alpha = 0.8f)
                    )

                    if (distanciaStr.isNotEmpty()) {
                        Surface(
                            color = Color(0xFF2C3E50),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "A $distanciaStr",
                                color = Color.White,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${cliente.NOMBRES} ${cliente.APELLIDO_PATERNO}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2C3E50)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(text = cliente.LOCALIDAD, fontSize = 13.sp, color = Color.Gray)
                }

                if (orientacionStr.isNotEmpty()) {
                    Text(
                        text = "Caminar $orientacionStr",
                        fontSize = 12.sp,
                        color = Color(0xFFE67E22),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { abrirMapa(context, cliente.LATITUD2, cliente.LONGITUD2, cliente.NOMBRES) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECF0F1), contentColor = Color(0xFF2C3E50))
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mapa", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onAction,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (yaVisitado) Color(0xFFE74C3C) else statusColor)
                    ) {
                        Icon(if (yaVisitado) Icons.Default.Close else Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (yaVisitado) "Quitar" else "Visitar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
