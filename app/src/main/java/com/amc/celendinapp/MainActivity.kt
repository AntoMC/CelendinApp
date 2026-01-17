package com.amc.celendinapp

// 1. Android & Activity (El "motor" de la App)
import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

// 2. Compose Foundation & Layout (Posicionamiento y Listas)
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

// 3. Compose UI, Estilos y Gráficos (Colores, Letras y Dibujo)
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 4. Material Design 3 (Botones, Buscadores, Menús y Tabs)
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem

// 5. Iconos (Asegúrate de tener la librería 'material-icons-extended' en el gradle)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import kotlinx.coroutines.launch

// 6. Gestión de Estado (Lo que hace que la pantalla reaccione)
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// 7. Tu Lógica Personalizada (Modelos y Funciones de Celendín)
import com.amc.celendinapp.model.Cliente
import com.amc.celendinapp.ui.theme.CelendinAppTheme
import com.amc.celendinapp.JsonUtils.abrirMapa
import com.amc.celendinapp.JsonUtils.enviarReporteWhatsApp
import com.amc.celendinapp.model.toCliente
import com.amc.celendinapp.network.RetrofitClient

// ... (Manten tus imports organizados aquí, asegúrate de incluir Search y Close) ...

// Dentro de tu MainActivity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var listaMutable by remember { mutableStateOf<List<Cliente>>(emptyList()) }
            var estaCargando by remember { mutableStateOf(true) }
            var mensajeCarga by remember { mutableStateOf("Iniciando conexión...") }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            // Función reutilizable para cargar datos
            fun cargarDatos() {
                estaCargando = true
                mensajeCarga = "Iniciando descarga masiva..."

                scope.launch {
                    try {
                        val listaAcumulada = mutableListOf<Cliente>()
                        var paginaActual = 0
                        var hayMasDatos = true

                        while (hayMasDatos) {
                            mensajeCarga = "Descargando página $paginaActual..."
                            val respuesta = RetrofitClient.instancia.obtenerInstalaciones(pagina = paginaActual)

                            if (respuesta.instalaciones.isNotEmpty()) {
                                val mapeados = respuesta.instalaciones.map { it.toCliente() }
                                listaAcumulada.addAll(mapeados)

                                if (respuesta.instalaciones.size < 100) hayMasDatos = false
                                else paginaActual++
                            } else {
                                hayMasDatos = false
                            }
                        }

                        listaMutable = listaAcumulada
                        // Guardamos para que la próxima vez no necesite internet
                        JsonUtils.guardarCacheLocal(context, listaAcumulada, "cache_clientes.json")

                    } catch (e: Exception) {
                        // Si falla la descarga manual, al menos recuperamos lo que teníamos
                        listaMutable = JsonUtils.leerCacheLocal(context, "cache_clientes.json") ?: emptyList()
                        mensajeCarga = "Error de red. Se mantuvo la base de datos anterior."
                        kotlinx.coroutines.delay(2000)
                    } finally {
                        estaCargando = false
                    }
                }
            }


            // Carga inicial
            LaunchedEffect(Unit) {
                // 1. Intentamos leer la caché primero
                val cache = JsonUtils.leerCacheLocal(context, "cache_clientes.json")

                if (cache != null && cache.isNotEmpty()) {
                    // ¡Ya hay datos! Los cargamos al instante
                    listaMutable = cache
                    estaCargando = false
                    println("API_TEST: Cargado desde caché local")
                } else {
                    // No hay caché (primera vez), descargamos de la API
                    cargarDatos()
                }
            }

            CelendinAppTheme {
                if (estaCargando) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFF1C40F))

                            Spacer(modifier = Modifier.height(24.dp))

                            // ESTO ES LO QUE MUESTRA EL LOG EN LA PANTALLA
                            Text(
                                text = mensajeCarga,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 30.dp)
                            )

                            Text(
                                text = "Procesando.. espere un minuto esto se hara solo una vez!!",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                } else {
                    // Pasamos la función cargarDatos al Wrapper
                    CelendinDrawerWrapper(
                        clientes = listaMutable,
                        onRefrescar = { cargarDatos() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinScreen(
clientes: List<Cliente>,
distritoActual: String,
onAbrirDrawer: () -> Unit
) {
    val context = LocalContext.current
    var textoBusqueda by remember { mutableStateOf("") }
    var localidadSeleccionada by remember { mutableStateOf("Todos") }
    var expandidoLocalidad by remember { mutableStateOf(false) }
    var visitadosIds by remember { mutableStateOf(VisitaManager.obtenerVisitados(context)) }
    var tabSeleccionada by remember { mutableStateOf("inicio") }
    var buscadorActivado by remember { mutableStateOf(false) }
    var mostrarAlertaBorrado by remember { mutableStateOf(false) }

    // IMPORTANTE: Solo mostramos las localidades que pertenecen al distrito seleccionado
    val listaLocalidades = remember(clientes) {
        val unicas = clientes.map { it.LOCALIDAD }.distinct().sorted()
        listOf("Todos") + unicas
    }

    // Reiniciar selector de localidad si cambiamos de distrito
    LaunchedEffect(clientes) {
        localidadSeleccionada = "Todos"
    }

    // LÓGICA DE FILTRADO
    val filtrados = remember(clientes, textoBusqueda, localidadSeleccionada, tabSeleccionada, visitadosIds) {
        clientes.filter { cliente ->
            // A) Filtro por Texto (Nombre, DNI o Suministro)
            val coincideTexto = if (textoBusqueda.isEmpty()) true else {
                val palabras = textoBusqueda.trim().lowercase().split("\\s+".toRegex())
                val nombreCompleto = "${cliente.NOMBRES} ${cliente.APELLIDO_PATERNO} ${cliente.APELLIDO_MATERNO}".lowercase()
                palabras.all { palabra ->
                    nombreCompleto.contains(palabra) ||
                            cliente.N__DNI.contains(palabra) ||
                            cliente.CÓDIGO_DE_SUMINISTRO2.contains(palabra)
                }
            }

            // B) Filtro por Localidad (del Selector superior)
            val coincideLocalidad = localidadSeleccionada == "Todos" || cliente.LOCALIDAD == localidadSeleccionada

            // C) Filtro por Tab (Inicio o Visitas marcadas)
            val coincideTab = if (tabSeleccionada == "visitas") {
                visitadosIds.contains(cliente.CÓDIGO_DE_SUMINISTRO2)
            } else true

            // RESULTADO: Debe cumplir las tres condiciones al mismo tiempo
            coincideTexto && coincideLocalidad && coincideTab
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onAbrirDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    }
                },
                title = {
                    if (!buscadorActivado) {
                        Column {
                            Text(distritoActual, color = Color.White, fontSize = 14.sp)
                            val subtitulo = if (localidadSeleccionada != "Todos") {
                                "${filtrados.size} de ${clientes.size} en $localidadSeleccionada"
                            } else {
                                "${clientes.size} instalaciones"
                            }
                            Text(subtitulo, color = Color.LightGray, fontSize = 12.sp)
                        }
                    } else {
                        // MODO BUSCADOR ACTIVO
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            // INDICADOR ENCIMA DEL CAMPO
                            Text(
                                text = "Encontrados: ${filtrados.size}",
                                color = Color(0xFFF1C40F), // Amarillo llamativo
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                            )

                            TextField(
                                value = textoBusqueda,
                                onValueChange = { textoBusqueda = it },
                                placeholder = { Text("Buscar nombre, DNI...", color = Color.LightGray, fontSize = 14.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedIndicatorColor = Color.White.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { buscadorActivado = !buscadorActivado; if(!buscadorActivado) textoBusqueda = "";localidadSeleccionada = "Todos";}) {
                        Icon(if (buscadorActivado) Icons.Default.Close else Icons.Default.Search, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF575775))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = tabSeleccionada == "inicio",
                    onClick = { tabSeleccionada = "inicio" },
                    label = { Text("Inicio") },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = tabSeleccionada == "visitas",
                    onClick = { tabSeleccionada = "visitas" },
                    label = { Text("Visitas") },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { enviarReporteWhatsApp(context, clientes, visitadosIds) },
                    label = { Text("Reporte") },
                    icon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
            }
        }
    ) { paddingValues ->

        // --- CONTENIDO PRINCIPAL ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFC3CED4))
        ) {
            // 1. Selector de Localidad (Solo si el buscador está activo)
            if (buscadorActivado) {
                Box(modifier = Modifier.background(Color(0xFF575775)).padding(12.dp)) {
                    SelectorLocalidad(
                        localidadSeleccionada = localidadSeleccionada,
                        expandido = expandidoLocalidad,
                        onExpandChange = { expandidoLocalidad = it },
                        onLocalidadSelect = { localidadSeleccionada = it },
                        listaLocalidades = listaLocalidades
                    )
                }
            }


            // 3. Lista de Tarjetas
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(filtrados) { cliente ->
                    TarjetaCliente(
                        cliente = cliente,
                        visitadosIds = visitadosIds,
                        onCheckClick = { id, wasVisited ->
                            if (wasVisited) VisitaManager.quitarVisita(context, id)
                            else VisitaManager.guardarVisita(context, id)
                            visitadosIds = VisitaManager.obtenerVisitados(context)
                        }
                    )
                }
            }
        }
    }

    if (mostrarAlertaBorrado) {
        AlertDialog(
            onDismissRequest = { mostrarAlertaBorrado = false }, // Si tocan fuera, se cierra
            title = { Text(text = "Confirmar reinicio") },
            text = { Text(text = "¿Estás seguro de que deseas borrar todas las visitas marcadas? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // AQUÍ va la lógica de borrado real
                        VisitaManager.borrarTodasLasVisitas(context)
                        visitadosIds = emptySet()
                        mostrarAlertaBorrado = false // Cerramos el diálogo
                    }
                ) {
                    Text("SÍ, BORRAR TODO", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarAlertaBorrado = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorLocalidad(
    localidadSeleccionada: String,
    expandido: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onLocalidadSelect: (String) -> Unit,
    listaLocalidades: List<String>
) {
    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = onExpandChange
    ) {
        TextField(
            value = localidadSeleccionada,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filtrar Localidad", color = Color.White) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedContainerColor = Color(0xFF6C6C8E),
                unfocusedContainerColor = Color(0xFF6C6C8E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { onExpandChange(false) }
        ) {
            listaLocalidades.forEach { loc ->
                DropdownMenuItem(
                    text = { Text(loc) },
                    onClick = { onLocalidadSelect(loc); onExpandChange(false) }
                )
            }
        }
    }
}

// ... (Manten tu función TarjetaCliente abajo tal cual la tenías) ...

@Composable
fun TarjetaCliente(cliente: Cliente, visitadosIds: Set<String>, onCheckClick: (String, Boolean) -> Unit) {
    val context = LocalContext.current
    // Verificamos si este cliente específico está en la lista de visitados
    val yaVisitado = visitadosIds.contains(cliente.CÓDIGO_DE_SUMINISTRO2)

    // Color de borde y fondo dinámico
    val colorBorde = if (yaVisitado) Color(0xFF27AE60) else Color(0xFF3498DB)
    val colorFondo = if (yaVisitado) Color(0xFFF0FFF4) else Color.White

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).drawBehind {
                drawRect(color = colorBorde, size = Size(width = 4.dp.toPx(), height = size.height))
            }
        ) {
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                if (yaVisitado) {
                    Text("✓ VISITADO", color = Color(0xFF27AE60), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                Text(text = "Suministro: ${cliente.CÓDIGO_DE_SUMINISTRO2}", fontSize = 12.sp, color = Color.Gray)
                Text(text = "${cliente.NOMBRES} ${cliente.APELLIDO_PATERNO}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Localidad: ${cliente.LOCALIDAD}", fontSize = 14.sp)

                Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Botón Mapa
                    Button(onClick = { abrirMapa(context, cliente.LATITUD2, cliente.LONGITUD2, cliente.NOMBRES) }) {
                        Text("Mapa")
                    }
                    // Botón Visita
                    OutlinedButton(
                        onClick = { onCheckClick(cliente.CÓDIGO_DE_SUMINISTRO2, yaVisitado) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (yaVisitado) Color.Red else Color(0xFF27AE60)
                        )
                    ) {
                        Text(if (yaVisitado) "Quitar" else "Visitar")
                    }
                }
            }
        }
    }
}
@Composable
fun CelendinDrawerWrapper(clientes: List<Cliente>, onRefrescar: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var distritoSeleccionado by remember { mutableStateOf("Todos") }

    //  lógica de la lista de distritos
    val conteoPorDistrito = remember(clientes) {
        clientes.groupingBy { it.DISTRITO }.eachCount()
    }

    val listaDistritosOrdenada = remember(conteoPorDistrito) {
        listOf("Todos") + conteoPorDistrito.keys.sorted()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DISTRITOS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    // BOTÓN DE ACTUALIZAR
                    IconButton(onClick = {
                        onRefrescar()
                        scope.launch { drawerState.close() }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = Color(0xFF575775))
                    }
                }
                HorizontalDivider()

                LazyColumn(modifier = Modifier.weight(1f)) {

                    items(listaDistritosOrdenada) { distrito ->
                        NavigationDrawerItem(
                            label = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(distrito)
                                    // Mostramos el conteo (si es "Todos", sumamos todo)
                                    val cantidad = if (distrito == "Todos") {
                                        clientes.size
                                    } else {
                                        conteoPorDistrito[distrito] ?: 0
                                    }

                                    Badge(containerColor = Color(0xFF575775), contentColor = Color.White) {
                                        Text("$cantidad")
                                    }
                                }
                            },
                            selected = distrito == distritoSeleccionado,
                            onClick = {
                                distritoSeleccionado = distrito
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }

                // Pie del Drawer con info de la App
                Text(
                    "Versión 2.0 - Adinelsa Cloud",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    ) {
        val clientesFiltrados = if (distritoSeleccionado == "Todos") clientes
        else clientes.filter { it.DISTRITO == distritoSeleccionado }

        CelendinScreen(
            clientes = clientesFiltrados,
            distritoActual = distritoSeleccionado,
            onAbrirDrawer = { scope.launch { drawerState.open() } }
        )
    }
}