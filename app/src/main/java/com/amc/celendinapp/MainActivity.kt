package com.amc.celendinapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.amc.celendinapp.componentes.*
import com.amc.celendinapp.model.*
import com.amc.celendinapp.network.RetrofitClient
import com.amc.celendinapp.ui.theme.CelendinAppTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var listaMutable by remember { mutableStateOf<List<Cliente>>(emptyList()) }
    var estaCargando by remember { mutableStateOf(false) }
    var mensajeCarga by remember { mutableStateOf("") }
    var mostrarDialogoOpciones by remember { mutableStateOf(false) }
    var mapaInternoActivado by remember { mutableStateOf(false) }
    var clienteSeleccionadoMapa by remember { mutableStateOf<Cliente?>(null) }

    val distritosCelendin = remember {
        listOf(
            "CELENDIN", "HUASMIN", "CHUMUCH", "CORTEGANA", 
            "HUASMIN", "JORGE CHAVEZ", "JOSE GALVEZ", "LA LIBERTAD DE PALLAN",
            "MIGUEL IGLESIAS", "OXAMARCA", "SOROCHUCO", "SUCRE", "UTCO"
        ).sorted()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                estaCargando = true
                mensajeCarga = "Procesando archivo...\n"
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() }
                    if (jsonString != null) {
                        val clientes = ImportManager.procesarJson(jsonString)
                        if (clientes.isNotEmpty()) {
                            listaMutable = clientes
                            CacheManager.guardar(context, clientes)
                            Toast.makeText(context, "Cargado correctamente", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al cargar", Toast.LENGTH_SHORT).show()
                } finally {
                    estaCargando = false
                }
            }
        }
    }

    fun ejecutarCargaDatos(filtroDistrito: String? = null) {
        estaCargando = true
        mensajeCarga = "Conectando...\n"
        scope.launch {
            try {
                val listaAcumulada = mutableListOf<Cliente>()
                var paginaActual = 0
                var hayMasDatos = true

                while (hayMasDatos) {
                    mensajeCarga = "Descargando... (Pág $paginaActual)"
                    val respuesta = RetrofitClient.instancia.obtenerInstalaciones(pagina = paginaActual)
                    if (respuesta.instalaciones.isNotEmpty()) {
                        val clientesNuevos = respuesta.instalaciones.map { it.toCliente() }
                        if (filtroDistrito != null) {
                            listaAcumulada.addAll(clientesNuevos.filter { it.distrito?.contains(filtroDistrito, ignoreCase = true) == true })
                        } else {
                            listaAcumulada.addAll(clientesNuevos)
                        }
                        if (respuesta.instalaciones.size < 100) hayMasDatos = false else paginaActual++
                        delay(50)
                    } else hayMasDatos = false
                }
                
                if (listaAcumulada.isNotEmpty()) {
                    listaMutable = listaAcumulada
                    CacheManager.guardar(context, listaAcumulada)
                    Toast.makeText(context, "Descarga completada", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                mensajeCarga = "Error de red"
                delay(2000)
            } finally {
                estaCargando = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val cache = CacheManager.leer(context)
        if (!cache.isNullOrEmpty()) {
            listaMutable = cache
        } else {
            delay(500)
            mostrarDialogoOpciones = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (estaCargando) {
            PantallaCargaLogs(mensajeCarga)
        } else {
            CelendinDrawerWrapper(
                clientes = listaMutable, 
                onAbrirOpcionesImportacion = { mostrarDialogoOpciones = true },
                mapaAbierto = mapaInternoActivado,
                onToggleMapa = { 
                    mapaInternoActivado = it 
                    if (!it) clienteSeleccionadoMapa = null // Limpia selección al cerrar
                },
                clienteSeleccionado = clienteSeleccionadoMapa,
                onSeleccionarCliente = { clienteSeleccionadoMapa = it }
            )
        }

        if (mostrarDialogoOpciones) {
            DialogoImportacion(
                distritosDisponibles = distritosCelendin,
                onDismiss = { mostrarDialogoOpciones = false },
                onImportarNuevo = { filePickerLauncher.launch("application/json") },
                onCargarRespaldo = {
                    val cache = CacheManager.leer(context)
                    if (!cache.isNullOrEmpty()) {
                        listaMutable = cache
                        Toast.makeText(context, "Respaldo recuperado", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Sin respaldo guardado", Toast.LENGTH_SHORT).show()
                    }
                },
                onDescargarNube = { distrito -> 
                    ejecutarCargaDatos(distrito)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinDrawerWrapper(
    clientes: List<Cliente>, 
    onAbrirOpcionesImportacion: () -> Unit,
    mapaAbierto: Boolean,
    onToggleMapa: (Boolean) -> Unit,
    clienteSeleccionado: Cliente?,
    onSeleccionarCliente: (Cliente?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var distritoSeleccionado by remember { mutableStateOf("Todos") }
    var refreshCounter by remember { mutableIntStateOf(0) }
    val visitadosIds = remember(refreshCounter) { VisitaManager.obtenerVisitados(context) }

    val conteoPorDistrito = remember(clientes) { 
        clientes.groupingBy { it.distrito ?: "Sin Distrito" }.eachCount() 
    }
    val listaDistritos = remember(conteoPorDistrito) { 
        listOf("Todos") + conteoPorDistrito.keys.sorted() 
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !mapaAbierto,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.75f)) {
                HeaderDrawer(
                    tieneVisitados = visitadosIds.isNotEmpty(),
                    onDeleteAllClick = { 
                        VisitaManager.borrarTodasLasVisitas(context)
                        refreshCounter++
                    },
                    onRefreshClick = onAbrirOpcionesImportacion
                )
                LazyColumn {
                    items(listaDistritos) { distrito ->
                        val cantidad = if (distrito == "Todos") clientes.size else conteoPorDistrito[distrito] ?: 0
                        NavigationDrawerItem(
                            label = {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    @Suppress("DEPRECATION")
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
    ) {
        val filtradosPorDistrito = if (distritoSeleccionado == "Todos") clientes
        else clientes.filter { (it.distrito ?: "Sin Distrito") == distritoSeleccionado }

        Box(modifier = Modifier.fillMaxSize()) {
            CelendinScreen(
                clientes = filtradosPorDistrito,
                distritoActual = distritoSeleccionado,
                visitadosIdsState = visitadosIds,
                onAbrirDrawer = { scope.launch { drawerState.open() } },
                onUpdateVisitados = { refreshCounter++ },
                onAbrirOpcionesImportacion = onAbrirOpcionesImportacion,
                mapaInternoActivado = mapaAbierto,
                onToggleMapa = onToggleMapa,
                clienteSeleccionado = clienteSeleccionado,
                onSeleccionarCliente = onSeleccionarCliente
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinScreen(
    clientes: List<Cliente>,
    distritoActual: String,
    visitadosIdsState: Set<String>,
    onAbrirDrawer: () -> Unit,
    onUpdateVisitados: () -> Unit,
    onAbrirOpcionesImportacion: () -> Unit,
    mapaInternoActivado: Boolean,
    onToggleMapa: (Boolean) -> Unit,
    clienteSeleccionado: Cliente?,
    onSeleccionarCliente: (Cliente?) -> Unit
) {
    val context = LocalContext.current
    var textoBusqueda by remember { mutableStateOf("") }
    var localidadSeleccionada by remember { mutableStateOf("Todos") }
    var buscadorActivado by remember { mutableStateOf(false) }
    var visitadosIds by remember { mutableStateOf(visitadosIdsState) }
    var tabSeleccionada by remember { mutableStateOf("inicio") }
    var buscandoGps by remember { mutableStateOf(false) }
    var menuMenuDesplegable by remember { mutableStateOf(false) }

    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var miUbicacion by remember { mutableStateOf<Location?>(null) }
    var radarExpandido by remember { mutableStateOf(false) }
    var radarActivo by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun obtenerUbicacionReal() {
        buscandoGps = true
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                buscandoGps = false
                if (location != null) {
                    miUbicacion = location
                }
            }
            .addOnFailureListener {
                buscandoGps = false
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) obtenerUbicacionReal()
    }

    fun iniciarUbicacion() {
        val finePermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (finePermission == PackageManager.PERMISSION_GRANTED) obtenerUbicacionReal()
        else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(visitadosIdsState) { visitadosIds = visitadosIdsState }

    val filtrados = remember(clientes, textoBusqueda, localidadSeleccionada, tabSeleccionada, visitadosIds, miUbicacion, radarActivo) {
        val base = clientes.filter { cl ->
            val nombresCompletos = "${cl.nombres ?: ""} ${cl.apellidoPaterno ?: ""} ${cl.codigoSuministro ?: ""}"
            val matchesText = textoBusqueda.isEmpty() || nombresCompletos.contains(textoBusqueda, ignoreCase = true)
            val matchesLoc = localidadSeleccionada == "Todos" || (cl.localidad ?: "Sin Localidad") == localidadSeleccionada
            val matchesTab = if (tabSeleccionada == "visitas") visitadosIds.contains(cl.codigoSuministro ?: "") else true
            matchesText && matchesLoc && matchesTab
        }

        if (radarActivo && miUbicacion != null) {
            base.map { cl ->
                val results = FloatArray(1)
                Location.distanceBetween(miUbicacion!!.latitude, miUbicacion!!.longitude, cl.latitud?.toDoubleOrNull() ?: 0.0, cl.longitud?.toDoubleOrNull() ?: 0.0, results)
                Pair(cl, results[0])
            }
                .filter { it.second <= 1000 }
                .sortedByDescending { it.first.latitud?.toDoubleOrNull() ?: 0.0 }
                .map { it.first }
        } else {
            base
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { HeaderTitle(distritoActual, filtrados.size, clientes.size, buscadorActivado, textoBusqueda) { textoBusqueda = it } },
                    navigationIcon = { IconButton(onClick = onAbrirDrawer) { Icon(Icons.Default.Menu, null, tint = Color.White) } },
                    actions = {
                        IconButton(onClick = {
                            buscadorActivado = !buscadorActivado
                            if (!buscadorActivado) { textoBusqueda = ""; localidadSeleccionada = "Todos"; miUbicacion = null }
                        }) { Icon(if (buscadorActivado || miUbicacion != null) Icons.Default.Close else Icons.Default.Search, null, tint = Color.White) }

                        Box {
                            IconButton(onClick = { menuMenuDesplegable = true }) {
                                Icon(Icons.Default.MoreVert, null, tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = menuMenuDesplegable,
                                onDismissRequest = { menuMenuDesplegable = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Ver en Mapa Google") },
                                    onClick = {
                                        menuMenuDesplegable = false
                                        onToggleMapa(true)
                                        iniciarUbicacion()
                                    },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (radarActivo) "Desactivar Radar" else "Activar Radar") },
                                    onClick = {
                                        menuMenuDesplegable = false
                                        radarActivo = !radarActivo
                                        if (radarActivo) iniciarUbicacion()
                                    },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Opciones de Padrón") },
                                    onClick = {
                                        menuMenuDesplegable = false
                                        onAbrirOpcionesImportacion()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF575775))
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = tabSeleccionada == "inicio", onClick = { tabSeleccionada = "inicio"; miUbicacion = null }, label = { Text(distritoActual) }, icon = { Icon(painterResource(id = R.drawable.ic_home), null, tint = Color.Unspecified) })
                    NavigationBarItem(selected = tabSeleccionada == "visitas", onClick = { tabSeleccionada = "visitas"; miUbicacion = null }, label = { Text("Visitas") }, icon = { Icon(painterResource(id = R.drawable.ic_check_circle_outline), null, tint = Color.Unspecified) })
                    NavigationBarItem(selected = false, onClick = { MapaUtils.enviarReporteWhatsApp(context, clientes, visitadosIds) }, label = { Text("Reporte") }, icon = { Icon(painterResource(id = R.drawable.ic_whatsapp), null, tint = Color.Unspecified) })
                }
            }
        ) { padding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFC3CED4))
            ) {
                Column(Modifier.fillMaxSize()) {
                    if (buscadorActivado) SelectorLocalidadComponent(localidadSeleccionada, remember(clientes) { listOf("Todos") + clientes.map { it.localidad ?: "Sin Localidad" }.distinct().sorted() }) { localidadSeleccionada = it }

                    if (radarActivo && miUbicacion != null) {
                        if (filtrados.isNotEmpty()) {
                            Button(onClick = {
                                radarExpandido = true
                            }, modifier = Modifier.fillMaxWidth().padding(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9)), shape = RoundedCornerShape(8.dp)) {
                                Icon(Icons.Default.Place, null)
                                Spacer(Modifier.width(8.dp))
                                Text("BARRIDO NORTE A SUR")
                            }
                        }
                    }

                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                        items(filtrados) { cliente ->
                            TarjetaCliente(
                                cliente = cliente, 
                                yaVisitado = visitadosIds.contains(cliente.codigoSuministro ?: ""), 
                                miUbicacion = miUbicacion, 
                                onVerMapa = { 
                                    onSeleccionarCliente(cliente) // MARCA ESTE CLIENTE
                                    onToggleMapa(true)
                                    iniciarUbicacion()
                                },
                                onAction = {
                                    val id = cliente.codigoSuministro ?: return@TarjetaCliente
                                    if (visitadosIds.contains(id)) VisitaManager.quitarVisita(context, id)
                                    else VisitaManager.guardarVisita(context, id)
                                    visitadosIds = VisitaManager.obtenerVisitados(context)
                                    onUpdateVisitados()
                                }
                            )
                        }
                    }
                }

                if (radarExpandido && miUbicacion != null) {
                    RadarMinimap(miUbicacion = miUbicacion!!, clientesCercanos = filtrados, visitadosIds = visitadosIds, onDismiss = { radarExpandido = false })
                }
            }
        }

        if (mapaInternoActivado) {
            MapaGoogle(
                miUbicacion = miUbicacion, 
                clientes = filtrados, 
                visitadosIds = visitadosIds,
                clienteSeleccionado = clienteSeleccionado, // PASAMOS LA SELECCIÓN
                onToggleVisita = { id ->
                    if (visitadosIds.contains(id)) VisitaManager.quitarVisita(context, id)
                    else VisitaManager.guardarVisita(context, id)
                    onUpdateVisitados()
                },
                onDismiss = { onToggleMapa(false) }
            )
        }
    }
}
