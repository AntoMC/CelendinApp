package com.amc.celendinapp.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.amc.celendinapp.CacheEntry
import com.amc.celendinapp.R

@Composable
fun DialogoImportacion(
    distritosDisponibles: List<String>,
    cacheEntries: List<CacheEntry>,
    onDismiss: () -> Unit,
    onImportarNuevo: () -> Unit,
    onCargarRespaldo: (String) -> Unit,
    onDescargarNube: (String?) -> Unit,
    onAbrirWhatsApp: () -> Unit = {}
) {
    var vistaActual by remember { mutableStateOf("principal") } // principal, distritos, respaldo

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                when(vistaActual) {
                    "distritos" -> "Seleccionar Distrito"
                    "respaldo" -> "Seleccionar Respaldo"
                    else -> "Gestión de Padrón"
                }
            ) 
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                when(vistaActual) {
                    "principal" -> {
                        OpcionImportar(
                            titulo = "Seleccionar Archivo JSON/KMZ",
                            subtitulo = "Uso offline (Archivo GIS)",
                            icono = Icons.Default.Add,
                            onClick = { onImportarNuevo(); onDismiss() }
                        )
                        OpcionImportar(
                            titulo = "Abrir desde WhatsApp",
                            subtitulo = "Buscar archivos en chats",
                            icono = Icons.Default.Email,
                            isWhatsApp = true,
                            onClick = { onAbrirWhatsApp(); onDismiss() }
                        )
                        OpcionImportar(
                            titulo = "Usar Respaldo Local",
                            subtitulo = "Historial de archivos cargados",
                            icono = Icons.Default.Refresh,
                            onClick = { vistaActual = "respaldo" }
                        )
                        OpcionImportar(
                            titulo = "Descargar por Distrito",
                            subtitulo = "Descarga nube y prepara offline",
                            icono = Icons.Default.Info,
                            onClick = { vistaActual = "distritos" }
                        )
                    }
                    "distritos" -> {
                        LazyColumn(modifier = Modifier.height(300.dp)) {
                            items(distritosDisponibles) { distrito ->
                                TextButton(
                                    onClick = { onDescargarNube(distrito); onDismiss() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(distrito, modifier = Modifier.fillMaxWidth())
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }
                    }
                    "respaldo" -> {
                        if (cacheEntries.isEmpty()) {
                            Text("No hay archivos en caché", modifier = Modifier.padding(16.dp), color = Color.Gray)
                        } else {
                            LazyColumn(modifier = Modifier.height(300.dp)) {
                                items(cacheEntries) { entry ->
                                    TextButton(
                                        onClick = { onCargarRespaldo(entry.id); onDismiss() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.fillMaxWidth()) {
                                            Text(entry.nombre, style = MaterialTheme.typography.bodyLarge)
                                            Text("${entry.clientes.size} registros", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { 
                if (vistaActual != "principal") vistaActual = "principal" else onDismiss() 
            }) { 
                Text(if (vistaActual != "principal") "VOLVER" else "CANCELAR")
            }
        }
    )
}

@Composable
fun OpcionImportar(
    titulo: String, 
    subtitulo: String, 
    icono: ImageVector, 
    isWhatsApp: Boolean = false,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isWhatsApp) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_whatsapp),
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
