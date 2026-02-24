package com.amc.celendinapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
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
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    
    // Estado reactivo para capturar archivos externos
    private val intentUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentUri.value = intent?.data
        setContent {
            CelendinAppTheme {
                MainAppContainer(intentUri)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentUri.value = intent.data
    }
}

@Composable
fun MainAppContainer(externalUri: MutableState<Uri?>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var listaMutable by remember { mutableStateOf<List<Cliente>>(emptyList()) }
    var estaCargando by remember { mutableStateOf(false) }
    var mensajeCarga by remember { mutableStateOf("") }
    var mostrarDialogoOpciones by remember { mutableStateOf(false) }
    var mapaInternoActivado by remember { mutableStateOf(false) }
    var clienteSeleccionadoMapa by remember { mutableStateOf<Cliente?>(null) }
    var seAbrioConArchivoExterno by remember { mutableStateOf(false) }

    val cacheEntries = remember { mutableStateListOf<CacheEntry>() }
    var selectedEntryId by remember { mutableStateOf("") }
    var idParaConfirmarCambio by remember { mutableStateOf<String?>(null) }
    
    var distritoSeleccionado by remember { mutableStateOf("Todos") }
    var buscadorActivado by remember { mutableStateOf(false) }
    var textoBusqueda by remember { mutableStateOf("") }

    val distritosCelendin = remember {
        listOf(
            "CELENDIN", "HUASMIN", "CHUMUCH", "CORTEGANA", 
            "HUASMIN", "JORGE CHAVEZ", "JOSE GALVEZ", "LA LIBERTAD DE PALLAN",
            "MIGUEL IGLESIAS", "OXAMARCA", "SOROCHUCO", "SUCRE", "UTCO"
        ).sorted()
    }

    fun cargarDatosDesdeCache() {
        val entry = CacheManager.leerEntradaSeleccionada(context)
        if (entry != null) {
            listaMutable = entry.clientes
            selectedEntryId = entry.id
            distritoSeleccionado = "Todos"
        }
        cacheEntries.clear()
        cacheEntries.addAll(CacheManager.leerTodasLasEntradas(context))
    }

    fun obtenerNombreArchivo(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    }
                }
            } catch (e: Exception) {}
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result?.substringBeforeLast(".") ?: "Archivo"
    }

    fun procesarUriExterna(uri: Uri) {
        scope.launch {
            estaCargando = true
            mensajeCarga = "Procesando archivo...\n"
            try {
                val contentResolver = context.contentResolver
                val type = contentResolver.getType(uri)
                val inputStream = contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    val clientes = if (type?.contains("kmz") == true || uri.toString().lowercase().endsWith(".kmz")) {
                        ImportManager.procesarKmz(inputStream)
                    } else {
                        val jsonString = inputStream.bufferedReader().use { it.readText() }
                        ImportManager.procesarJson(jsonString)
                    }

                    if (clientes.isNotEmpty()) {
                        val nombreBase = obtenerNombreArchivo(uri)
                        val distritoPrincipal = clientes.mapNotNull { it.distrito }
                            .groupingBy { it }
                            .eachCount()
                            .maxByOrNull { it.value }?.key ?: "Varios"
                        
                        val nombreFinal = "$nombreBase ($distritoPrincipal)"
                        
                        CacheManager.guardarNuevaEntrada(context, nombreFinal, clientes)
                        cargarDatosDesdeCache()
                        seAbrioConArchivoExterno = true
                        Toast.makeText(context, "Archivo cargado con éxito", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir archivo", Toast.LENGTH_SHORT).show()
            } finally {
                estaCargando = false
            }
        }
    }

    // EFECTO REACTIVO PARA ARCHIVOS EXTERNOS (WhatsApp, etc.)
    LaunchedEffect(externalUri.value) {
        externalUri.value?.let { uri ->
            procesarUriExterna(uri)
            externalUri.value = null // Limpiar para permitir recargas
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) procesarUriExterna(uri)
    }

    fun exportarCopiaSeguridad() {
        if (listaMutable.isEmpty()) {
            Toast.makeText(context, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val json = Gson().toJson(listaMutable)
            val fileName = "CelendinApp_Backup_${System.currentTimeMillis()}.json"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { it.write(json.toByteArray()) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            val intentShare = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intentShare, "Guardar copia de seguridad"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error al exportar", Toast.LENGTH_SHORT).show()
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
                    val nombreCache = "Nube - ${filtroDistrito ?: "Todo"}"
                    CacheManager.guardarNuevaEntrada(context, nombreCache, listaAcumulada)
                    cargarDatosDesdeCache()
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
        cargarDatosDesdeCache()
        if (listaMutable.isEmpty()) {
            if (!seAbrioConArchivoExterno) {
                delay(500)
                mostrarDialogoOpciones = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (estaCargando) {
            PantallaCargaLogs(mensajeCarga)
        } else {
            CelendinDrawerWrapper(
                clientes = listaMutable, 
                cacheEntries = cacheEntries,
                selectedEntryId = selectedEntryId,
                distritoSeleccionado = distritoSeleccionado,
                buscadorActivado = buscadorActivado,
                textoBusqueda = textoBusqueda,
                onSearchChange = { textoBusqueda = it },
                onToggleBuscador = { 
                    buscadorActivado = it 
                    if (!it) { textoBusqueda = "" }
                },
                onCambiarDistrito = { distritoSeleccionado = it },
                onSelectCache = { id ->
                    idParaConfirmarCambio = id
                },
                onDeleteCache = { id ->
                    CacheManager.eliminarEntrada(context, id)
                    cargarDatosDesdeCache()
                },
                onAbrirOpcionesImportacion = { mostrarDialogoOpciones = true },
                onExportarBackup = { exportarCopiaSeguridad() },
                mapaAbierto = mapaInternoActivado,
                onToggleMapa = { 
                    mapaInternoActivado = it 
                    if (!it) clienteSeleccionadoMapa = null
                },
                clienteSeleccionado = clienteSeleccionadoMapa,
                onSeleccionarCliente = { clienteSeleccionadoMapa = it }
            )
        }

        if (mostrarDialogoOpciones) {
            DialogoImportacion(
                distritosDisponibles = distritosCelendin,
                cacheEntries = cacheEntries,
                onDismiss = { mostrarDialogoOpciones = false },
                onImportarNuevo = { filePickerLauncher.launch("*/*") },
                onCargarRespaldo = { id ->
                    CacheManager.seleccionarEntrada(context, id)
                    cargarDatosDesdeCache()
                    Toast.makeText(context, "Respaldo cargado", Toast.LENGTH_SHORT).show()
                },
                onDescargarNube = { distrito -> 
                    ejecutarCargaDatos(distrito)
                },
                onAbrirWhatsApp = {
                    val pm = context.packageManager
                    val whatsappIntent = pm.getLaunchIntentForPackage("com.whatsapp") ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                    if (whatsappIntent != null) {
                        context.startActivity(whatsappIntent)
                    } else {
                        Toast.makeText(context, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        idParaConfirmarCambio?.let { id ->
            val entrada = cacheEntries.find { it.id == id }
            AlertDialog(
                onDismissRequest = { idParaConfirmarCambio = null },
                title = { Text("Cambiar Archivo") },
                text = { Text("¿Deseas cargar los datos de '${entrada?.nombre}'? Se reemplazará la vista actual.") },
                confirmButton = {
                    Button(onClick = {
                        CacheManager.seleccionarEntrada(context, id)
                        cargarDatosDesdeCache()
                        idParaConfirmarCambio = null
                        Toast.makeText(context, "Cargado: ${entrada?.nombre}", Toast.LENGTH_SHORT).show()
                    }) { Text("CARGAR") }
                },
                dismissButton = {
                    TextButton(onClick = { idParaConfirmarCambio = null }) { Text("CANCELAR") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinDrawerWrapper(
    clientes: List<Cliente>, 
    cacheEntries: List<CacheEntry>,
    selectedEntryId: String,
    distritoSeleccionado: String,
    buscadorActivado: Boolean,
    textoBusqueda: String,
    onSearchChange: (String) -> Unit,
    onToggleBuscador: (Boolean) -> Unit,
    onCambiarDistrito: (String) -> Unit,
    onSelectCache: (String) -> Unit,
    onDeleteCache: (String) -> Unit,
    onAbrirOpcionesImportacion: () -> Unit,
    onExportarBackup: () -> Unit,
    mapaAbierto: Boolean,
    onToggleMapa: (Boolean) -> Unit,
    clienteSeleccionado: Cliente?,
    onSeleccionarCliente: (Cliente?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var refreshCounter by remember { mutableIntStateOf(0) }
    val visitadosIds = remember(refreshCounter, selectedEntryId) { VisitaManager.obtenerVisitados(context) }

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
                
                Text("ARCHIVOS CARGADOS", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(cacheEntries) { entry ->
                        NavigationDrawerItem(
                            label = {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Text(entry.nombre, maxLines = 1, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onDeleteCache(entry.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            selected = entry.id == selectedEntryId,
                            onClick = { 
                                onSelectCache(entry.id)
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
                HorizontalDivider()
                Text("DISTRITOS", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = Color.Gray)
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
                                onCambiarDistrito(distrito)
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
                buscadorActivado = buscadorActivado,
                textoBusqueda = textoBusqueda,
                onSearchChange = onSearchChange,
                onToggleBuscador = onToggleBuscador,
                visitadosIdsState = visitadosIds,
                onAbrirDrawer = { scope.launch { drawerState.open() } },
                onUpdateVisitados = { refreshCounter++ },
                onAbrirOpcionesImportacion = onAbrirOpcionesImportacion,
                onExportarBackup = onExportarBackup,
                mapaInternoActivado = mapaAbierto,
                onToggleMapa = { onToggleMapa(it) },
                clienteSeleccionado = clienteSeleccionado,
                onSeleccionarCliente = { onSeleccionarCliente(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelendinScreen(
    clientes: List<Cliente>,
    distritoActual: String,
    buscadorActivado: Boolean,
    textoBusqueda: String,
    onSearchChange: (String) -> Unit,
    onToggleBuscador: (Boolean) -> Unit,
    visitadosIdsState: Set<String>,
    onAbrirDrawer: () -> Unit,
    onUpdateVisitados: () -> Unit,
    onAbrirOpcionesImportacion: () -> Unit,
    onExportarBackup: () -> Unit,
    mapaInternoActivado: Boolean,
    onToggleMapa: (Boolean) -> Unit,
    clienteSeleccionado: Cliente?,
    onSeleccionarCliente: (Cliente?) -> Unit
) {
    val context = LocalContext.current
    var localidadSeleccionada by remember { mutableStateOf("Todos") }
    var visitadosIds by remember { mutableStateOf(visitadosIdsState) }
    var tabSeleccionada by remember { mutableStateOf("inicio") }
    var menuMenuDesplegable by remember { mutableStateOf(false) }

    val fusedLocationClient: FusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var miUbicacion by remember { mutableStateOf<Location?>(null) }

    @SuppressLint("MissingPermission")
    fun obtenerUbicacionReal() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    miUbicacion = location
                }
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

    // FILTRO ÚNICO PARA LISTA Y MAPA
    val filtrados = remember(clientes, textoBusqueda, localidadSeleccionada, tabSeleccionada, visitadosIds, miUbicacion) {
        clientes.filter { cl ->
            val nombresCompletos = "${cl.nombres ?: ""} ${cl.apellidoPaterno ?: ""} ${cl.apellidoMaterno ?: ""} ${cl.codigoSuministro ?: ""}"
            val matchesText = textoBusqueda.isEmpty() || nombresCompletos.contains(textoBusqueda, ignoreCase = true)
            val matchesLoc = localidadSeleccionada == "Todos" || (cl.localidad ?: "Sin Localidad") == localidadSeleccionada
            val matchesTab = if (tabSeleccionada == "visitas") visitadosIds.contains(cl.codigoSuministro ?: "") else true
            matchesText && matchesLoc && matchesTab
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        HeaderTitle(
                            distrito = distritoActual, 
                            found = filtrados.size, 
                            total = clientes.size, 
                            searching = buscadorActivado, 
                            textoBusqueda = textoBusqueda,
                            localidadSeleccionada = localidadSeleccionada,
                            listaLocalidades = remember(clientes) { listOf("Todos") + clientes.map { it.localidad ?: "Sin Localidad" }.distinct().sorted() },
                            onSearch = onSearchChange,
                            onLocalidadChange = { localidadSeleccionada = it },
                            onCloseSearch = { 
                                onToggleBuscador(false) 
                                localidadSeleccionada = "Todos"
                                miUbicacion = null
                            }
                        ) 
                    },
                    navigationIcon = { IconButton(onClick = onAbrirDrawer) { Icon(Icons.Default.Menu, null, tint = Color.White) } },
                    actions = {
                        if (!buscadorActivado) {
                            IconButton(onClick = { onToggleBuscador(true) }) { 
                                Icon(Icons.Default.Search, null, tint = Color.White) 
                            }
                        }

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
                                    text = { Text("Opciones de Padrón") },
                                    onClick = {
                                        menuMenuDesplegable = false
                                        onAbrirOpcionesImportacion()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Settings, null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Exportar Copia Seguridad") },
                                    onClick = {
                                        menuMenuDesplegable = false
                                        onExportarBackup()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, null) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF455A64))
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = tabSeleccionada == "inicio", onClick = { tabSeleccionada = "inicio"; miUbicacion = null }, label = { Text(distritoActual) }, icon = { Icon(painterResource(id = R.drawable.ic_home), null, tint = Color.Unspecified) })
                    NavigationBarItem(selected = tabSeleccionada == "visitas", onClick = { tabSeleccionada = "visitas"; miUbicacion = null }, label = { Text("Visitas") }, icon = { Icon(painterResource(id = R.drawable.ic_check_circle_outline), null, tint = Color.Unspecified) })
                    NavigationBarItem(selected = false, onClick = { MapaUtils.enviarReporteWhatsApp(context, clientes, visitadosIds) }, label = { Text("Reporte") }, icon = { Icon(painterResource(id = R.drawable.ic_whatsapp), null, tint = Color.Unspecified) })
                }
            },
            floatingActionButton = {
                if (clientes.isNotEmpty() && !mapaInternoActivado) {
                    FloatingActionButton(
                        onClick = {
                            onToggleMapa(true)
                            iniciarUbicacion()
                        },
                        containerColor = Color(0xFF2196F3),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = "Ver Mapa")
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFC3CED4))
            ) {
                Column(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
                        items(filtrados) { cliente ->
                            TarjetaCliente(
                                cliente = cliente, 
                                yaVisitado = visitadosIds.contains(cliente.codigoSuministro ?: ""), 
                                miUbicacion = miUbicacion, 
                                onVerMapa = { 
                                    onSeleccionarCliente(cliente)
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
            }
        }

        // MAPA EN PANTALLA COMPLETA USANDO SOLO LOS REGISTROS FILTRADOS
        if (mapaInternoActivado) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                MapaGoogle(
                    miUbicacion = miUbicacion, 
                    clientes = filtrados, 
                    visitadosIds = visitadosIds,
                    clienteSeleccionado = clienteSeleccionado,
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
}
