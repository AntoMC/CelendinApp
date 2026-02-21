package com.amc.celendinapp.componentes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeaderTitle(
    distrito: String,
    found: Int,
    total: Int,
    searching: Boolean,
    textoBusqueda: String,
    onSearch: (String) -> Unit
) {
    if (!searching) {
        Column {
            Text(distrito, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("$total registros", color = Color.LightGray, fontSize = 12.sp)
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 0.dp)
        ) {
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = onSearch,
                placeholder = { Text("Buscar...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFB0BEC5),
                    unfocusedBorderColor = Color(0xFF546E7A),
                    cursorColor = Color(0xFFF1C40F)
                ),
                trailingIcon = {
                    Surface(
                        color = Color(0xFFF1C40F),
                        shape = CircleShape,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = found.toString(),
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            )
        }
    }
}
