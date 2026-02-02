package com.amc.celendinapp

// --- IMPORTS ORGANIZADOS ---
import android.os.Bundle
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource // <--- NUEVO IMPORT
import com.amc.celendinapp.R //
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Tus modelos y utilidades
import com.amc.celendinapp.model.Cliente
import com.amc.celendinapp.model.toCliente
import com.amc.celendinapp.network.RetrofitClient
import com.amc.celendinapp.ui.theme.CelendinAppTheme
import com.amc.celendinapp.JsonUtils.abrirMapa
import com.amc.celendinapp.JsonUtils.enviarReporteWhatsApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// 1. ACTIVIDAD PRINCIPAL (Punto de Entrada)
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CelendinAppTheme {
                MainAppContainer()
            }
        }
    }
}

// ==========================================
// 2. CONTENEDOR DE ESTADO GLOBAL
// ==========================================
@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados de datos
    var listaMutable by remember { mutableStateOf<List<Cliente>>(emptyList()) }
    var estaCargando by remember { mutableStateOf(true) }
    var mensajeCarga by remember { mutableStateOf("Iniciando conexión...\n") }

    // Función de carga masiva
    fun ejecutarCargaDatos() {
        estaCargando = true
        mensajeCarga = "Iniciando descarga masiva del servidor... \n"
        scope.launch {
            try {
                val listaAcumulada = mutableListOf<Cliente>()
                var paginaActual = 0
                var hayMasDatos = true

                while (hayMasDatos) {
                    mensajeCarga += "Recibiendo página $paginaActual...\n"
                    val respuesta = RetrofitClient.instancia.obtenerInstalaciones(pagina = paginaActual)
                    Log.d("API_SUCCESS", "Datos recibidos: ${respuesta.instalaciones.size}")
                    if (respuesta.instalaciones.isNotEmpty()) {
                        listaAcumulada.addAll(respuesta.instalaciones.map { it.toCliente() })
                        if (respuesta.instalaciones.size < 100) hayMasDatos = false else paginaActual++
                    } else hayMasDatos = false
                }
                listaMutable = listaAcumulada
                JsonUtils.guardarCacheLocal(context, listaAcumulada, "cache_clientes.json")
            } catch (e: Exception) {
                // AQUÍ ES DONDE VEREMOS EL ERROR REAL EN EL LOGCAT
                Log.e("API_ERROR", "Fallo total en la petición: ${e.message}")
                e.printStackTrace()
                listaMutable = JsonUtils.leerCacheLocal(context, "cache_clientes.json") ?: emptyList()
                mensajeCarga = "Error de red. Usando respaldo local."
                delay(2000)
            } finally {
                estaCargando = false
            }
        }
    }

    // Efecto de inicio: Cargar caché o descargar
    LaunchedEffect(Unit) {
        val cache = JsonUtils.leerCacheLocal(context, "cache_clientes.json")
        if (!cache.isNullOrEmpty()) {
            listaMutable = cache
            estaCargando = false
        } else {
            ejecutarCargaDatos()
        }
    }

    if (estaCargando) {
        PantallaCargaLogs(mensajeCarga)
    } else {
        CelendinDrawerWrapper(
            clientes = listaMutable,
            onRefrescar = { ejecutarCargaDatos() }
        )
    }
}

// ==========================================
// 3. COMPONENTES DE INTERFAZ (UI)
// ==========================================

@Composable
fun PantallaCargaLogs(mensaje: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2B2B2B)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFF1C40F))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = mensaje,
                color = Color.Green,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinDrawerWrapper(
    clientes: List<Cliente>,
    onRefrescar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Estados internos
    var distritoSeleccionado by remember { mutableStateOf("Todos") }
    var refreshCounter by remember { mutableStateOf(0) }
    val visitadosIds = remember(refreshCounter) { VisitaManager.obtenerVisitados(context) }

    // Diálogos
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val conteoPorDistrito = remember(clientes) { clientes.groupingBy { it.DISTRITO }.eachCount() }
    val listaDistritos = remember(conteoPorDistrito) { listOf("Todos") + conteoPorDistrito.keys.sorted() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent =
            {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.75f) //ancho en la pantalla
                ){
                ModalDrawerSheet {
                    HeaderDrawer(
                        tieneVisitados = visitadosIds.isNotEmpty(),
                        onDeleteAllClick = { showDeleteAllDialog = true },
                        onRefreshClick = { showUpdateDialog = true }
                    )
                    LazyColumn {
                        items(listaDistritos) { distrito ->
                            val cantidad = if (distrito == "Todos") clientes.size else conteoPorDistrito[distrito] ?: 0
                            NavigationDrawerItem(
                                label = {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text(distrito)
                                        Badge(containerColor = Color(0xFF575775)) { Text("$cantidad", color = Color.White) }
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
                }
            }

        }
    ) {
        val filtradosPorDistrito = if (distritoSeleccionado == "Todos") clientes
        else clientes.filter { it.DISTRITO == distritoSeleccionado }

        CelendinScreen(
            clientes = filtradosPorDistrito,
            distritoActual = distritoSeleccionado,
            visitadosIniciales = visitadosIds,
            onAbrirDrawer = { scope.launch { drawerState.open() } },
            onUpdateVisitados = { refreshCounter++ }
        )
    }

    // --- Lógica de Diálogos ---
    if (showUpdateDialog) {
        ConfirmDialog(
            titulo = "Actualizar Padrón",
            mensaje = "Se descargarán cerca de  1,800 registros de la nube.\n Esto puede tardar un minuto.\n Puede fallar si se corta el intenet  ¿Continuar?",
            onConfirm = { showUpdateDialog = false; onRefrescar() },
            onDismiss = { showUpdateDialog = false }
        )
    }
    if (showDeleteAllDialog) {
        ConfirmDialog(
            titulo = "Reiniciar Visitas",
            mensaje = "¿Quitar todas visitas registradas?",
            confirmText = "BORRAR TODO",
            isDanger = true,
            onConfirm = {
                VisitaManager.borrarTodasLasVisitas(context)
                refreshCounter++
                showDeleteAllDialog = false
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinScreen(
    clientes: List<Cliente>,
    distritoActual: String,
    visitadosIniciales: Set<String>,
    onAbrirDrawer: () -> Unit,
    onUpdateVisitados: () -> Unit
) {
    val context = LocalContext.current
    var textoBusqueda by remember { mutableStateOf("") }
    var localidadSeleccionada by remember { mutableStateOf("Todos") }
    var expandidoLocalidad by remember { mutableStateOf(false) }
    var buscadorActivado by remember { mutableStateOf(false) }
    var visitadosIds by remember { mutableStateOf(visitadosIniciales) }
    var tabSeleccionada by remember { mutableStateOf("inicio") }


    // Sincronizar visitas cuando cambian externamente (desde el Drawer)
    LaunchedEffect(visitadosIniciales) { visitadosIds = visitadosIniciales }

    // LÓGICA DE FILTRADO (Asegúrate de incluir tabSeleccionada en los argumentos del remember)
    val filtrados = remember(clientes, textoBusqueda, localidadSeleccionada, tabSeleccionada, visitadosIds) {
        clientes.filter { cl ->
            // A) Filtro por Texto (Nombre o Suministro)
            val matchesText = textoBusqueda.isEmpty() ||
                    "${cl.NOMBRES} ${cl.APELLIDO_PATERNO} ${cl.CÓDIGO_DE_SUMINISTRO2}".contains(textoBusqueda, ignoreCase = true)

            // B) Filtro por Localidad
            val matchesLoc = localidadSeleccionada == "Todos" || cl.LOCALIDAD == localidadSeleccionada

            // C) FILTRO DE PESTAÑA: Si estamos en 'visitas', solo mostramos los que están en la lista de IDs
            val matchesTab = if (tabSeleccionada == "visitas") {
                visitadosIds.contains(cl.CÓDIGO_DE_SUMINISTRO2)
            } else {
                true // En la pestaña 'inicio' mostramos todos
            }

            // El cliente debe cumplir las 3 condiciones
            matchesText && matchesLoc && matchesTab
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    HeaderTitle(distritoActual,
                        filtrados.size,
                        clientes.size,
                        buscadorActivado,
                        textoBusqueda = textoBusqueda,
                    ) {
                        textoBusqueda = it
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onAbrirDrawer) { Icon(Icons.Default.Menu, null, tint = Color.White) }
                },
                actions = {
                    IconButton(onClick = {
                        buscadorActivado = !buscadorActivado
                        if (!buscadorActivado) { textoBusqueda = ""; localidadSeleccionada = "Todos" }
                    }) {
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
                    onClick = {
                        tabSeleccionada = "inicio"
                        textoBusqueda = ""
                        localidadSeleccionada = "Todos"
                              }, // Cambia el estado aquí
                    label = { Text(text = distritoActual)},
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_home),
                            contentDescription = "Inicio",
                            tint = Color.Unspecified // Para que mantenga su color verde original
                        )
                    }
                )
                NavigationBarItem(
                    selected = tabSeleccionada == "visitas",
                    onClick = { tabSeleccionada = "visitas" },
                    label = { Text("Visitas") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check_circle_outline),
                            contentDescription = "Visitas",
                            tint = Color.Unspecified // Para que mantenga su color verde original
                        )
                    }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { enviarReporteWhatsApp(context, clientes, visitadosIds) },
                    label = { Text("Reporte") },
                    // LA FORMA CORRECTA ES ESTA:
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp",
                            tint = Color.Unspecified // Para que mantenga su color verde original
                        )
                    }
                )
            }
        }
    ) { padding ->
        Column(Modifier
            .padding(padding)
            .background(Color(0xFFC3CED4))) {
            if (buscadorActivado) {
                SelectorLocalidadComponent(
                    selected = localidadSeleccionada,
                    items = remember(clientes) { listOf("Todos") + clientes.map { it.LOCALIDAD }.distinct().sorted() },
                    onSelect = { localidadSeleccionada = it }
                )
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                items(filtrados) { cliente ->
                    TarjetaCliente(
                        cliente = cliente,
                        yaVisitado = visitadosIds.contains(cliente.CÓDIGO_DE_SUMINISTRO2),
                        onAction = {
                            if (visitadosIds.contains(cliente.CÓDIGO_DE_SUMINISTRO2))
                                VisitaManager.quitarVisita(context, cliente.CÓDIGO_DE_SUMINISTRO2)
                            else
                                VisitaManager.guardarVisita(context, cliente.CÓDIGO_DE_SUMINISTRO2)

                            visitadosIds = VisitaManager.obtenerVisitados(context)
                            onUpdateVisitados()
                        }
                    )
                }
            }
        }
    }
}

// --- SUB-COMPONENTES DE APOYO ---

@Composable
fun HeaderDrawer(tieneVisitados: Boolean, onDeleteAllClick: () -> Unit, onRefreshClick: () -> Unit) {
    Row(Modifier
        .fillMaxWidth()
        .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("DISTRITOS", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (tieneVisitados) {
            IconButton(onClick = onDeleteAllClick) { Icon(
                painter = painterResource(id = R.drawable.ic_delete),
                contentDescription = null,
                tint = Color(0xFFE74C3C) // Rojo para advertencia
            ) }
        }
        IconButton(onClick = onRefreshClick) { Icon(
            painter = painterResource(id = R.drawable.ic_nube),
            contentDescription = null,
            tint = Color(0xFF27AE60) // Verde para acción positiva
        ) }
    }
    HorizontalDivider()
}

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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E), // Fondo oscuro
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFB0BEC5),   // Borde claro al estar activo
                    unfocusedBorderColor = Color(0xFF546E7A), // Borde gris azulado claro
                    cursorColor = Color(0xFFF1C40F)
                ),
                trailingIcon = {
                    // El numerito dentro del mismo campo para ahorrar espacio
                    Surface(
                        color = Color(0xFFF1C40F),
                        shape = androidx.compose.foundation.shape.CircleShape,
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

@Composable
fun ConfirmDialog(titulo: String, mensaje: String, confirmText: String = "SÍ", isDanger: Boolean = false, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            Button(onClick = onConfirm, colors = if (isDanger) ButtonDefaults.buttonColors(containerColor = Color.Red) else ButtonDefaults.buttonColors()) {
                Text(confirmText)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@Composable
fun TarjetaCliente(
    cliente: Cliente,
    yaVisitado: Boolean,
    onAction: () -> Unit
) {
    val context = LocalContext.current

    // Colores basados en el estado
    val statusColor = if (yaVisitado) Color(0xFF2ECC71) else Color(0xFF3498DB)
    val cardBg = if (yaVisitado) Color(0xFFF1F9F5) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp), // Esquinas más redondeadas
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min) // Para que el indicador lateral mida igual que el texto
        ) {
            // 1. INDICADOR LATERAL DE ESTADO
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                // 2. CABECERA (Suministro y Tag de Visitado)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUM: ${cliente.CÓDIGO_DE_SUMINISTRO2}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )

                    if (yaVisitado) {
                        Surface(
                            color = statusColor,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "✓ COMPLETADO",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. NOMBRE DEL CLIENTE (Destacado)
                Text(
                    text = "${cliente.NOMBRES} ${cliente.APELLIDO_PATERNO} ${cliente.APELLIDO_MATERNO}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2C3E50),
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 4. INFO DE LOCALIZACIÓN
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = cliente.LOCALIDAD,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. BOTONES ACCIÓN (Estilo más moderno)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Botón Mapa con estilo "Tonal"
                    Button(
                        onClick = { abrirMapa(context, cliente.LATITUD2, cliente.LONGITUD2, cliente.NOMBRES) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFECF0F1),
                            contentColor = Color(0xFF2C3E50)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mapa", fontSize = 13.sp)
                    }

                    // Botón Visitar/Quitar
                    Button(
                        onClick = onAction,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (yaVisitado) Color(0xFFE74C3C) else statusColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            if (yaVisitado) Icons.Default.Close else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (yaVisitado) "Quitar" else "Visitar", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorLocalidadComponent(selected: String, items: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box(Modifier.background(Color(0xFF575775)).padding(12.dp)) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                label = { Text("Filtrar Localidad", color = Color(0xFFB0BEC5)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E), // Fondo oscuro
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,        // Borde blanco total al abrir
                    unfocusedBorderColor = Color(0xFFB0BEC5), // Borde claro
                    focusedLabelColor = Color(0xFFF1C40F)
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF1E1E1E)) // Fondo oscuro del menú desplegable
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item, color = Color.White) },
                        onClick = { onSelect(item); expanded = false }
                    )
                }
            }
        }
    }
}