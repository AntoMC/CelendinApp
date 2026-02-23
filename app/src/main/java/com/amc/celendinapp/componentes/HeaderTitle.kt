package com.amc.celendinapp.componentes

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeaderTitle(
    distrito: String,
    found: Int,
    total: Int,
    searching: Boolean,
    textoBusqueda: String,
    localidadSeleccionada: String,
    listaLocalidades: List<String>,
    onSearch: (String) -> Unit,
    onLocalidadChange: (String) -> Unit,
    onCloseSearch: () -> Unit
) {
    if (!searching) {
        Column {
            Text(
                text = distrito.uppercase(), 
                color = Color.White, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "$total REGISTROS", 
                color = Color(0xFFE0E0E0), 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .animateContentSize()
        ) {
            // SELECTOR LOCALIDAD
            var expandedLoc by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .weight(if (textoBusqueda.isEmpty()) 0.35f else 0.2f)
                    .fillMaxHeight()
                    .padding(end = 4.dp)
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .clickable { expandedLoc = true },
                contentAlignment = Alignment.Center
            ) {
                Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        localidadSeleccionada.uppercase(), 
                        color = Color.Black, 
                        fontSize = 9.sp, 
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = expandedLoc, onDismissRequest = { expandedLoc = false }, modifier = Modifier.background(Color.White).width(200.dp)) {
                    listaLocalidades.forEach { loc ->
                        DropdownMenuItem(text = { Text(loc.uppercase(), color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }, onClick = { onLocalidadChange(loc); expandedLoc = false })
                    }
                }
            }

            // BUSCADOR PERSONALIZADO (Evita fuente cortada)
            Box(
                modifier = Modifier
                    .weight(if (textoBusqueda.isEmpty()) 0.65f else 0.8f)
                    .fillMaxHeight()
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = textoBusqueda,
                        onValueChange = onSearch,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        cursorBrush = SolidColor(Color.Black),
                        textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        decorationBox = { innerTextField ->
                            if (textoBusqueda.isEmpty()) {
                                Text("BUSCAR...", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            innerTextField()
                        }
                    )
                    
                    if (textoBusqueda.isNotEmpty()) {
                        Surface(color = Color(0xFFF1C40F), shape = CircleShape, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text(found.toString(), color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }
                    
                    IconButton(onClick = onCloseSearch, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
