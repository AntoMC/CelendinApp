package com.amc.celendinapp.componentes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amc.celendinapp.R

@Composable
fun HeaderDrawer(tieneVisitados: Boolean, onDeleteAllClick: () -> Unit, onRefreshClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text("DISTRITOS", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (tieneVisitados) {
            IconButton(onClick = onDeleteAllClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = null,
                    tint = Color(0xFFE74C3C) // Rojo para advertencia
                )
            }
        }
        IconButton(onClick = onRefreshClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_nube),
                contentDescription = null,
                tint = Color(0xFF27AE60) // Verde para acción positiva
            )
        }
    }
    HorizontalDivider()
}
