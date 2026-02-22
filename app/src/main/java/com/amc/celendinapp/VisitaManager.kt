package com.amc.celendinapp

import android.content.Context
import androidx.core.content.edit

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
