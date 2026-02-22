package com.amc.celendinapp

import android.content.Context
import com.amc.celendinapp.model.Cliente
import com.google.gson.Gson

object FileUtils {
    fun leerDesdeAssets(context: Context, fileName: String): List<Cliente> {
        return try {
            val json = context.assets.open(fileName).bufferedReader().readText()
            Gson().fromJson(json, Array<Cliente>::class.java).toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
