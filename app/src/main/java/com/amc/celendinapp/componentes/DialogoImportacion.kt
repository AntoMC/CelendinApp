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
import androidx.compose.ui.unit.dp

@Composable
fun DialogoImportacion(
    distritosDisponibles: List<String>,
    onDismiss: () -> Unit,
    onImportarNuevo: () -> Unit,
    onCargarRespaldo: () -> Unit,
    onDescargarNube: (String?) -> Unit
) {
    var mostrarSelectorDistrito by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mostrarSelectorDistrito) "Seleccionar Distrito" else "Gestión de Padrón") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!mostrarSelectorDistrito) {
                    OpcionImportar(
                        titulo = "Seleccionar Archivo JSON",
                        subtitulo = "Uso offline (Archivo GIS)",
                        icono = Icons.Default.Add,
                        onClick = { onImportarNuevo(); onDismiss() }
                    )
                    OpcionImportar(
                        titulo = "Usar Respaldo Local",
                        subtitulo = "Recupera la última carga",
                        icono = Icons.Default.Refresh,
                        onClick = { onCargarRespaldo(); onDismiss() }
                    )
                    OpcionImportar(
                        titulo = "Descargar por Distrito",
                        subtitulo = "Descarga nube y prepara offline",
                        icono = Icons.Default.Info, // Cambiado de CloudDownload a Info para evitar error de referencia
                        onClick = { mostrarSelectorDistrito = true }
                    )
                } else {
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
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { 
                if (mostrarSelectorDistrito) mostrarSelectorDistrito = false else onDismiss() 
            }) { 
                Text(if (mostrarSelectorDistrito) "VOLVER" else "CANCELAR") 
            }
        }
    )
}

@Composable
fun OpcionImportar(titulo: String, subtitulo: String, icono: ImageVector, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}
