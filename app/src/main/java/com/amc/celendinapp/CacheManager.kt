package com.amc.celendinapp

import android.content.Context
import com.amc.celendinapp.model.Cliente
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

data class CacheEntry(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val fecha: Long = System.currentTimeMillis(),
    val clientes: List<Cliente>
)

object CacheManager {
    private const val PREFS_NAME = "celendin_cache_v2"
    private const val KEY_ENTRIES = "cache_entries"
    private const val KEY_SELECTED_ID = "selected_entry_id"

    fun guardarNuevaEntrada(context: Context, nombre: String, clientes: List<Cliente>) {
        val entries = leerTodasLasEntradas(context).toMutableList()
        val nuevaEntrada = CacheEntry(nombre = nombre, clientes = clientes)
        entries.add(nuevaEntrada)
        
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(entries)
        sharedPreferences.edit()
            .putString(KEY_ENTRIES, json)
            .putString(KEY_SELECTED_ID, nuevaEntrada.id)
            .apply()
    }

    fun leerTodasLasEntradas(context: Context): List<CacheEntry> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(KEY_ENTRIES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<CacheEntry>>() {}.type
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun leerEntradaSeleccionada(context: Context): CacheEntry? {
        val entries = leerTodasLasEntradas(context)
        if (entries.isEmpty()) return null
        
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedId = sharedPreferences.getString(KEY_SELECTED_ID, null)
        
        return entries.find { it.id == selectedId } ?: entries.last()
    }

    fun seleccionarEntrada(context: Context, id: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_SELECTED_ID, id).apply()
    }

    fun eliminarEntrada(context: Context, id: String) {
        val entries = leerTodasLasEntradas(context).filter { it.id != id }
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(entries)
        sharedPreferences.edit().putString(KEY_ENTRIES, json).apply()
    }
}
