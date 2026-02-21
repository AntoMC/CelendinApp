package com.amc.celendinapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amc.celendinapp.componentes.*
import com.amc.celendinapp.model.Cliente
import com.amc.celendinapp.model.toCliente
import com.amc.celendinapp.network.RetrofitClient
import com.amc.celendinapp.ui.theme.CelendinAppTheme
import com.amc.celendinapp.JsonUtils.enviarReporteWhatsApp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
    var estaCargando by remember { mutableStateOf(true) }
    var mensajeCarga by remember { mutableStateOf("Iniciando conexión...\n") }

    fun ejecutarCargaDatos() {
        estaCargando = true
        mensajeCarga = "Iniciando descarga masiva... \n"
        scope.launch {
            try {
                val listaAcumulada = mutableListOf<Cliente>()
                var paginaActual = 0
                var hayMasDatos = true

                while (hayMasDatos) {
                    val respuesta = RetrofitClient.instancia.obtenerInstalaciones(pagina = paginaActual)
                    if (respuesta.instalaciones.isNotEmpty()) {
                        listaAcumulada.addAll(respuesta.instalaciones.map { it.toCliente() })
                        if (respuesta.instalaciones.size < 100) hayMasDatos = false else paginaActual++
                    } else hayMasDatos = false
                }
                listaMutable = listaAcumulada
                JsonUtils.guardarCacheLocal(context, listaAcumulada, "cache_clientes.json")
            } catch (e: Exception) {
                listaMutable = JsonUtils.leerCacheLocal(context, "cache_clientes.json") ?: emptyList()
                mensajeCarga = "Error de red. Usando respaldo local."
                delay(2000)
            } finally {
                estaCargando = false
            }
        }
    }

    LaunchedEffect(Unit) {
        val cache = JsonUtils.leerCacheLocal(context, "cache_clientes.json")
        if (!cache.isNullOrEmpty()) {
            listaMutable = cache
            estaCargando = false
        } else {
            ejecutarCargaDatos()
        }
    }

    if (estaCargando) PantallaCargaLogs(mensajeCarga)
    else CelendinDrawerWrapper(clientes = listaMutable, onRefrescar = { ejecutarCargaDatos() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinDrawerWrapper(clientes: List<Cliente>, onRefrescar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var distritoSeleccionado by remember { mutableStateOf("Todos") }
    var refreshCounter by remember { mutableIntStateOf(0) }
    val visitadosIds = remember(refreshCounter) { VisitaManager.obtenerVisitados(context) }

    var showUpdateDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val conteoPorDistrito = remember(clientes) { clientes.groupingBy { it.DISTRITO }.eachCount() }
    val listaDistritos = remember(conteoPorDistrito) { listOf("Todos") + conteoPorDistrito.keys.sorted() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.75f)) {
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
        else clientes.filter { it.DISTRITO == distritoSeleccionado }

        CelendinScreen(
            clientes = filtradosPorDistrito,
            distritoActual = distritoSeleccionado,
            visitadosIniciales = visitadosIds,
            onAbrirDrawer = { scope.launch { drawerState.open() } },
            onUpdateVisitados = { refreshCounter++ }
        )
    }

    if (showUpdateDialog) {
        ConfirmDialog("Actualizar Padrón", "Se descargarán registros de la nube. ¿Continuar?", onConfirm = { showUpdateDialog = false; onRefrescar() }, onDismiss = { showUpdateDialog = false })
    }
    if (showDeleteAllDialog) {
        ConfirmDialog("Reiniciar Visitas", "¿Borrar todo?", confirmText = "BORRAR", isDanger = true, onConfirm = { VisitaManager.borrarTodasLasVisitas(context); refreshCounter++; showDeleteAllDialog = false }, onDismiss = { showDeleteAllDialog = false })
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
    var buscadorActivado by remember { mutableStateOf(false) }
    var visitadosIds by remember { mutableStateOf(visitadosIniciales) }
    var tabSeleccionada by remember { mutableStateOf("inicio") }
    var buscandoGps by remember { mutableStateOf(false) }

    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var miUbicacion by remember { mutableStateOf<Location?>(null) }
    var radarExpandido by remember { mutableStateOf(false) }

    fun obtenerUbicacionActual() {
        val finePermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (finePermission == PackageManager.PERMISSION_GRANTED) {
            buscandoGps = true
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    buscandoGps = false
                    if (location != null) {
                        miUbicacion = location
                        Toast.makeText(context, "Radar Activo: Barrido Norte a Sur", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Sin señal GPS. Intenta en cielo abierto.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    buscandoGps = false
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            ActivityCompat.requestPermissions(context as android.app.Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }

    LaunchedEffect(visitadosIniciales) { visitadosIds = visitadosIniciales }

    val filtrados = remember(clientes, textoBusqueda, localidadSeleccionada, tabSeleccionada, visitadosIds, miUbicacion) {
        val base = clientes.filter { cl ->
            val matchesText = textoBusqueda.isEmpty() || "${cl.NOMBRES} ${cl.APELLIDO_PATERNO} ${cl.CÓDIGO_DE_SUMINISTRO2}".contains(textoBusqueda, ignoreCase = true)
            val matchesLoc = localidadSeleccionada == "Todos" || cl.LOCALIDAD == localidadSeleccionada
            val matchesTab = if (tabSeleccionada == "visitas") visitadosIds.contains(cl.CÓDIGO_DE_SUMINISTRO2) else true
            matchesText && matchesLoc && matchesTab
        }

        if (miUbicacion != null) {
            base.map { cl ->
                val results = FloatArray(1)
                Location.distanceBetween(miUbicacion!!.latitude, miUbicacion!!.longitude, cl.LATITUD2?.toDoubleOrNull() ?: 0.0, cl.LONGITUD2?.toDoubleOrNull() ?: 0.0, results)
                Pair(cl, results[0])
            }
                .filter { it.second <= 1000 }
                .sortedByDescending { it.first.LATITUD2?.toDoubleOrNull() ?: 0.0 }
                .map { it.first }
        } else {
            base
        }
    }

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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF575775))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(selected = tabSeleccionada == "inicio", onClick = { tabSeleccionada = "inicio"; miUbicacion = null }, label = { Text(distritoActual) }, icon = { Icon(painterResource(id = R.drawable.ic_home), null, tint = Color.Unspecified) })
                NavigationBarItem(selected = tabSeleccionada == "visitas", onClick = { tabSeleccionada = "visitas"; miUbicacion = null }, label = { Text("Visitas") }, icon = { Icon(painterResource(id = R.drawable.ic_check_circle_outline), null, tint = Color.Unspecified) })
                NavigationBarItem(selected = false, onClick = { enviarReporteWhatsApp(context, clientes, visitadosIds) }, label = { Text("Reporte") }, icon = { Icon(painterResource(id = R.drawable.ic_whatsapp), null, tint = Color.Unspecified) })
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (miUbicacion == null) obtenerUbicacionActual() else miUbicacion = null },
                containerColor = when {
                    buscandoGps -> Color.Gray
                    miUbicacion != null -> Color(0xFFE74C3C)
                    else -> Color(0xFFF1C40F)
                },
                contentColor = if(miUbicacion != null || buscandoGps) Color.White else Color.Black,
                icon = {
                    if (buscandoGps) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(if(miUbicacion != null) Icons.Default.Close else Icons.Default.LocationOn, null)
                },
                text = { Text(when { buscandoGps -> "LOCALIZANDO..." ; miUbicacion != null -> "Apagar Radar" ; else -> "¿Qué hay cerca?" }) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(Color(0xFFC3CED4))
        ) {
            Column(Modifier.fillMaxSize()) {
                if (buscadorActivado) SelectorLocalidadComponent(localidadSeleccionada, remember(clientes) { listOf("Todos") + clientes.map { it.LOCALIDAD }.distinct().sorted() }) { localidadSeleccionada = it }

                if (miUbicacion != null) {
                    if (filtrados.isNotEmpty()) {
                        Button(onClick = {
                            radarExpandido = true
                        }, modifier = Modifier.fillMaxWidth().padding(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2980B9)), shape = RoundedCornerShape(8.dp)) {
                            Icon(Icons.Default.Place, null)
                            Spacer(Modifier.width(8.dp))
                            Text("VER RUTA DE BARRIDO (NORTE A SUR)")
                        }
                    } else {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No hay suministros a 500m. Camina un poco más.", color = Color.DarkGray, textAlign = TextAlign.Center)
                        }
                    }
                }

                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                    items(filtrados) { cliente ->
                        TarjetaCliente(cliente = cliente, yaVisitado = visitadosIds.contains(cliente.CÓDIGO_DE_SUMINISTRO2), miUbicacion = miUbicacion, onAction = {
                            if (visitadosIds.contains(cliente.CÓDIGO_DE_SUMINISTRO2)) VisitaManager.quitarVisita(context, cliente.CÓDIGO_DE_SUMINISTRO2)
                            else VisitaManager.guardarVisita(context, cliente.CÓDIGO_DE_SUMINISTRO2)
                            visitadosIds = VisitaManager.obtenerVisitados(context)
                            onUpdateVisitados()
                        })
                    }
                }
            }

            if (radarExpandido && miUbicacion != null) {
                RadarMinimap(
                    miUbicacion = miUbicacion!!,
                    clientesCercanos = filtrados,
                    visitadosIds = visitadosIds,
                    onDismiss = { radarExpandido = false }
                )
            }
        }
    }
}
