package com.amc.celendinapp

import com.amc.celendinapp.model.Cliente
import com.amc.celendinapp.model.ClienteLocal
import com.amc.celendinapp.model.InstalacionApi
import com.amc.celendinapp.model.toCliente
import com.google.gson.Gson
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

object ImportManager {
    fun procesarJson(jsonString: String): List<Cliente> {
        val gson = Gson()
        return try {
            val arrayLocal = gson.fromJson(jsonString, Array<ClienteLocal>::class.java)
            if (arrayLocal != null && arrayLocal.isNotEmpty() && arrayLocal[0].suministro != null) {
                return arrayLocal.map { it.toCliente() }
            }
            val arrayApi = gson.fromJson(jsonString, Array<InstalacionApi>::class.java)
            if (arrayApi != null && arrayApi.isNotEmpty() && arrayApi[0].suministro != null) {
                return arrayApi.map { it.toCliente() }
            }
            val arrayCli = gson.fromJson(jsonString, Array<Cliente>::class.java)
            arrayCli?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun procesarKmz(inputStream: InputStream): List<Cliente> {
        return try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            var clientes = emptyList<Cliente>()
            while (entry != null) {
                if (entry.name.endsWith(".kml", ignoreCase = true)) {
                    clientes = parseKml(zipInputStream)
                    break
                }
                entry = zipInputStream.nextEntry
            }
            clientes
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseKml(inputStream: InputStream): List<Cliente> {
        val clientes = mutableListOf<Cliente>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var nombreTag: String? = null
        var descripcionTag: String? = null
        var latitud: String? = null
        var longitud: String? = null
        var currentTag: String? = null
        val datosExtendidos = mutableMapOf<String, String>()
        var lastDataName: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = tagName
                    if (tagName == "Data" || tagName == "SimpleData") {
                        lastDataName = parser.getAttributeValue(null, "name")
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.trim()
                    if (text.isNotEmpty()) {
                        when (currentTag) {
                            "name" -> nombreTag = text
                            "description" -> descripcionTag = text
                            "value", "SimpleData" -> if (lastDataName != null) datosExtendidos[lastDataName?.uppercase() ?: ""] = text
                            "coordinates" -> {
                                val cleanText = text.replace("\\s".toRegex(), "")
                                val coords = cleanText.split(",")
                                if (coords.size >= 2) {
                                    longitud = coords[0].trim()
                                    latitud = coords[1].trim()
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "Placemark") {
                        if (latitud != null && longitud != null) {
                            val descLimpia = descripcionTag?.replace("<[^>]*>".toRegex(), "")?.replace("&nbsp;", " ")?.trim()
                            
                            // Búsqueda exhaustiva de campos
                            val nombreKmz = datosExtendidos["NOMBRE"] ?: datosExtendidos["NOMBRES"] ?: ""
                            val apePatKmz = datosExtendidos["APELLIDO_PATERNO"] ?: datosExtendidos["APELLIDO_P"] ?: ""
                            val apeMatKmz = datosExtendidos["APELLIDO_MATERNO"] ?: datosExtendidos["APELLIDO_M"] ?: ""
                            val localidadKmz = datosExtendidos["LOCALIDAD"] ?: datosExtendidos["CENTRO_POBLADO"] ?: "KMZ"
                            
                            // Detectar si name o description son el número de suministro
                            val nameEsNumero = nombreTag?.all { it.isDigit() || it == '-' || it == '.' } == true
                            val descEsNumero = descLimpia?.all { it.isDigit() || it == '-' || it == '.' } == true
                            val suministro = if (nameEsNumero) nombreTag else if (descEsNumero) descLimpia else nombreTag
                            
                            // Si no encontramos nombre en ExtendedData, usamos el tag 'name' o 'description' si no son números
                            var nombreFinal = nombreKmz
                            if (nombreFinal.isBlank()) {
                                nombreFinal = if (!nameEsNumero && !nombreTag.isNullOrBlank()) nombreTag 
                                             else if (!descEsNumero && !descLimpia.isNullOrBlank()) descLimpia 
                                             else "Punto ${clientes.size + 1}"
                            }

                            clientes.add(
                                Cliente(
                                    codigoSuministro = suministro ?: "KMZ-${clientes.size + 1}",
                                    nombres = nombreFinal.trim(),
                                    apellidoPaterno = apePatKmz.trim(),
                                    apellidoMaterno = apeMatKmz.trim(),
                                    dni = datosExtendidos["DNI"] ?: "",
                                    localidad = localidadKmz.trim(),
                                    distrito = "KMZ",
                                    latitud = latitud,
                                    longitud = longitud,
                                    estado = "ACTIVO",
                                    isVisitado = false
                                )
                            )
                        }
                        nombreTag = null; descripcionTag = null; latitud = null; longitud = null; datosExtendidos.clear()
                        lastDataName = null
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }
        return clientes
    }
}
