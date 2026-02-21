package com.amc.celendinapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.amc.celendinapp.model.Cliente
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken // Import correcto para GSON
import java.text.SimpleDateFormat
import java.util.Date
import androidx.core.content.edit
import android.location.Location


object JsonUtils {

    fun leerClientesDesdeAssets(context: Context, fileName: String): List<Cliente> {
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val tipoLista = object : TypeToken<List<Cliente>>() {}.type
            Gson().fromJson(jsonString, tipoLista)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun abrirMapa(context: Context, latitud: String?, longitud: String?, nombreCliente: String) {
        if (latitud.isNullOrEmpty() || longitud.isNullOrEmpty()) {
            Toast.makeText(context, "El suministro no tiene coordenadas GPS", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Usamos el formato 'dir' (directions).
            // Al NO poner 'saddr' (origen), Maps usa automáticamente "Tu ubicación".
            // 'daddr' es el destino.
            // '&t=k' fuerza la vista de Satélite que te gustó ayer.
            // '&dirflg=w' establece que la ruta sea a pie (puedes quitarlo para auto).
            val uri = "http://maps.google.com/maps?daddr=$latitud,$longitud&t=k&dirflg=w"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)

        } catch (e: Exception) {
            // Si hay error, usamos el pin simple que ya teníamos
            val fallbackUri = "geo:$latitud,$longitud?q=$latitud,$longitud($nombreCliente)"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri)))
        }
    }



    fun enviarReporteWhatsApp(context: Context, todosLosClientes: List<Cliente>, visitadosIds: Set<String>) {
        val visitados = todosLosClientes.filter { visitadosIds.contains(it.CÓDIGO_DE_SUMINISTRO2) }
        if (visitados.isEmpty()) {
            Toast.makeText(context, "No hay visitas para reportar", Toast.LENGTH_SHORT).show()
            return
        }

        var mensaje = "🚀 *REPORTE DE CAMPO - CELENDÍN*%0A"
        mensaje += "📅 *Fecha:* ${SimpleDateFormat("dd/MM/yyyy").format(Date())}%0A"
        mensaje += "----------------------------%0A"

        visitados.forEachIndexed { index, v ->
            mensaje += "*${index + 1}.* Suministro: ${v.CÓDIGO_DE_SUMINISTRO2}%0A"
            mensaje += "   Beneficiario: ${v.NOMBRES} ${v.APELLIDO_PATERNO}%0A%0A"
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://wa.me/?text=$mensaje")
        context.startActivity(intent)
    }
    // Añade esto a tu clase o objeto JsonUtils
    fun guardarCacheLocal(context: Context, clientes: List<Cliente>, nombreArchivo: String) {
        try {
            val gson = com.google.gson.Gson()
            val jsonString = gson.toJson(clientes)
            context.openFileOutput(nombreArchivo, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
            println("CACHE: Guardado exitoso en memoria interna")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun leerCacheLocal(context: Context, nombreArchivo: String): List<Cliente>? {
        return try {
            val file = context.getFileStreamPath(nombreArchivo)
            if (file.exists()) {
                val jsonString = file.bufferedReader().use { it.readText() }
                val type = object : com.google.gson.reflect.TypeToken<List<Cliente>>() {}.type
                com.google.gson.Gson().fromJson(jsonString, type)
            } else null
        } catch (e: Exception) { null }
    }
    fun calcularDistanciaEnMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    fun abrirRutaRadar(context: Context, suministrosCercanos: List<Cliente>) {
        if (suministrosCercanos.isEmpty()) {
            Toast.makeText(context, "No hay suministros cerca", Toast.LENGTH_SHORT).show()
            return
        }

        // Volvemos al plan de los 10 más cercanos (Letras A hasta J)
        val listaReducida = suministrosCercanos.take(10)

        // El último de la lista será el destino (Letra J)
        val destino = listaReducida.last()
        val latDest = destino.LATITUD2
        val lonDest = destino.LONGITUD2

        // Las paradas intermedias (Waypoints: Letras A hasta I)
        val waypoints = listaReducida.dropLast(1).joinToString("|") {
            "${it.LATITUD2},${it.LONGITUD2}"
        }

        try {
            // Usamos el formato de "dir" (directions) que genera las letras
            // travelmode=walking hace que la línea sea de puntos, menos invasiva
            val uriString = "https://www.google.com/maps/dir/?api=1&destination=$latDest,$lonDest&waypoints=$waypoints&travelmode=walking&t=k"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al abrir la ruta con letras", Toast.LENGTH_SHORT).show()
        }
    }

    fun agruparYOrdenarNorteSur(lista: List<Cliente>): List<Cliente> {
        if (lista.isEmpty()) return lista

        // 1. Primero ordenamos toda la lista de Norte a Sur.
        // En Cajamarca (Latitud negativa), el "Norte" es el número mayor (ej: -6.5 es más norte que -6.8).
        val listaOrdenadaNorteSur = lista.sortedByDescending { it.LATITUD2.toDouble() }

        // 2. Opcional: Si quieres agruparlos por cercanía (ej. bloques de 50 metros)
        // para que el técnico no salte de un cerro a otro:
        val listaAgrupada = mutableListOf<Cliente>()
        val listaRestante = listaOrdenadaNorteSur.toMutableList()

        while (listaRestante.isNotEmpty()) {
            val pivote = listaRestante.removeAt(0) // Tomamos el más al Norte disponible
            listaAgrupada.add(pivote)

            // Buscamos "vecinos" cercanos a menos de 100 metros del pivote
            val vecinos = listaRestante.filter { candidato ->
                val dist = FloatArray(1)
                Location.distanceBetween(
                    pivote.LATITUD2.toDouble(), pivote.LONGITUD2.toDouble(),
                    candidato.LATITUD2.toDouble(), candidato.LONGITUD2.toDouble(),
                    dist
                )
                dist[0] < 100 // Radio de agrupación de 100 metros
            }

            // Los añadimos a la lista final y los quitamos de los pendientes
            listaAgrupada.addAll(vecinos)
            listaRestante.removeAll(vecinos)
        }

        return listaAgrupada
    }


}

object VisitaManager {
    private const val PREFS_NAME = "visitas_prefs"
    private const val KEY_VISITADOS = "visitados_ids"

    fun guardarVisita(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val actuales = obtenerVisitados(context).toMutableSet()
        actuales.add(id)
        prefs.edit { putStringSet(KEY_VISITADOS, actuales) }
    }

    fun quitarVisita(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val actuales = obtenerVisitados(context).toMutableSet()
        actuales.remove(id)
        prefs.edit { putStringSet(KEY_VISITADOS, actuales) }
    }

    fun obtenerVisitados(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_VISITADOS, emptySet()) ?: emptySet()
    }

    fun borrarTodasLasVisitas(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_VISITADOS) }
    }
}