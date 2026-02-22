package com.amc.celendinapp.componentes

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amc.celendinapp.model.Cliente

@Composable
fun TarjetaCliente(
    cliente: Cliente,
    yaVisitado: Boolean,
    miUbicacion: Location?,
    onVerMapa: () -> Unit, // Nuevo callback para el mapa interno
    onAction: () -> Unit
) {
    val statusColor = if (yaVisitado) Color(0xFF2ECC71) else Color(0xFF3498DB)
    val cardBg = if (yaVisitado) Color(0xFFF1F9F5) else Color.White

    var distanciaStr = ""

    if (miUbicacion != null) {
        val latCl = cliente.latitud?.toDoubleOrNull() ?: 0.0
        val lonCl = cliente.longitud?.toDoubleOrNull() ?: 0.0

        val results = FloatArray(1)
        Location.distanceBetween(miUbicacion.latitude, miUbicacion.longitude, latCl, lonCl, results)
        val metros = results[0]
        distanciaStr = if (metros > 1000) "%.1f km".format(metros / 1000) else "${metros.toInt()} m"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(statusColor))
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(text = "SUM: ${cliente.codigoSuministro ?: ""}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    if (distanciaStr.isNotEmpty()) {
                        Surface(color = Color(0xFF2C3E50), shape = RoundedCornerShape(4.dp)) {
                            Text(text = distanciaStr, color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(text = "${cliente.nombres ?: ""} ${cliente.apellidoPaterno ?: ""}", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text(text = cliente.localidad ?: "", fontSize = 13.sp, color = Color.Gray)
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onVerMapa, // Ahora abre el mapa interno
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(8.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFECF0F1), contentColor = Color.Black)
                    ) {
                        Text("Mapa", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onAction, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(8.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = if (yaVisitado) Color.Red else statusColor)
                    ) {
                        Text(if (yaVisitado) "Quitar" else "Visitar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
