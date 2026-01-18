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

    fun abrirMapa(context: Context, latitud: String, longitud: String, nombreCliente: String) {
        if (latitud.isNotEmpty() && longitud.isNotEmpty()) {
            val uri = "geo:0,0?q=$latitud,$longitud($nombreCliente)"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No se encontró aplicación de mapas", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Sin coordenadas registradas", Toast.LENGTH_SHORT).show()
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