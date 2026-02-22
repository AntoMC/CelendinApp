package com.amc.celendinapp

import com.amc.celendinapp.model.Cliente
import com.amc.celendinapp.model.ClienteLocal
import com.amc.celendinapp.model.InstalacionApi
import com.amc.celendinapp.model.toCliente
import com.google.gson.Gson

object ImportManager {
    fun procesarJson(jsonString: String): List<Cliente> {
        val gson = Gson()
        return try {
            // Intento 1: Tu JSON local
            val arrayLocal = gson.fromJson(jsonString, Array<ClienteLocal>::class.java)
            if (arrayLocal != null && arrayLocal.isNotEmpty() && arrayLocal[0].suministro != null) {
                return arrayLocal.map { it.toCliente() }
            }
            
            // Intento 2: JSON del Servidor
            val arrayApi = gson.fromJson(jsonString, Array<InstalacionApi>::class.java)
            if (arrayApi != null && arrayApi.isNotEmpty() && arrayApi[0].suministro != null) {
                return arrayApi.map { it.toCliente() }
            }
            
            // Intento 3: Formato interno
            val arrayCli = gson.fromJson(jsonString, Array<Cliente>::class.java)
            arrayCli?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
