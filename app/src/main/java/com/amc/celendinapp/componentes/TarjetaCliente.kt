package com.amc.celendinapp.componentes

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
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
    onVerMapa: () -> Unit,
    onAction: () -> Unit
) {
    val esKmz = cliente.distrito == "KMZ"
    
    // Colores suaves para las tarjetas (DÍA)
    val cardBg = if (yaVisitado) Color(0xFFE8F5E9) else Color(0xFFFFEBEE) // Verde suave vs Rojo suave
    val statusColor = if (yaVisitado) Color(0xFF2E7D32) else Color(0xFFC62828) // Verde oscuro vs Rojo oscuro para acentos
    val colorKmzAccent = Color(0xFF7B1FA2) // Púrpura para KMZ acento

    var distanciaStr = ""

    if (miUbicacion != null) {
        val latCl = cliente.latitud?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
        val lonCl = cliente.longitud?.replace(",", ".")?.toDoubleOrNull() ?: 0.0

        val results = FloatArray(1)
        Location.distanceBetween(miUbicacion.latitude, miUbicacion.longitude, latCl, lonCl, results)
        val metros = results[0]
        distanciaStr = if (metros > 1000) "%.1f km".format(metros / 1000) else "${metros.toInt()} m"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Barra lateral con el color sólido para identificar KMZ o estado
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(if(esKmz) colorKmzAccent else statusColor))
            
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val labelId = if (esKmz) {
                        "SUM: ${cliente.codigoSuministro?.removePrefix("KMZ-") ?: ""}"
                    } else {
                        "SUM: ${cliente.codigoSuministro ?: ""}"
                    }
                    
                    Text(
                        text = labelId, 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Black, 
                        color = if(esKmz) colorKmzAccent else statusColor
                    )
                    
                    if (distanciaStr.isNotEmpty()) {
                        Surface(color = Color(0xFF455A64), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = distanciaStr, 
                                color = Color.White, 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(4.dp))
                
                val nombreMostrado = listOfNotNull(cliente.nombres, cliente.apellidoPaterno, cliente.apellidoMaterno)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .trim()
                
                Text(
                    text = if (nombreMostrado.isEmpty()) "SIN NOMBRE" else nombreMostrado.uppercase(), 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    lineHeight = 22.sp
                )
                
                Spacer(Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (esKmz) Icons.Default.Place else Icons.Default.Home, 
                        null, 
                        modifier = Modifier.size(16.dp), 
                        tint = Color.DarkGray
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = (cliente.localidad ?: "SIN LOCALIDAD").uppercase(), 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray,
                        maxLines = 2
                    )
                }
                
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onVerMapa,
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                    ) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(18.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("MAPA", fontSize = 13.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = onAction, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(8.dp), 
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (yaVisitado) Color(0xFF2E7D32) else (if(esKmz) colorKmzAccent else Color(0xFFC62828))
                        )
                    ) {
                        Text(
                            text = if (yaVisitado) "QUITAR" else (if (esKmz) "MARCAR" else "VISITAR"), 
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
