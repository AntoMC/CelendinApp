package com.amc.celendinapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.amc.celendinapp.model.Cliente
import java.text.SimpleDateFormat
import java.util.Date
import android.location.Location

object MapaUtils {

    fun abrirMapa(context: Context, latitud: String?, longitud: String?, nombreCliente: String?) {
        if (latitud.isNullOrEmpty() || longitud.isNullOrEmpty()) {
            Toast.makeText(context, "El suministro no tiene coordenadas GPS", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = "http://maps.google.com/maps?daddr=$latitud,$longitud&t=k&dirflg=w"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackUri = "geo:$latitud,$longitud?q=$latitud,$longitud(${nombreCliente ?: ""})"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUri)))
        }
    }

    fun enviarReporteWhatsApp(context: Context, todosLosClientes: List<Cliente>, visitadosIds: Set<String>) {
        val visitados = todosLosClientes.filter { visitadosIds.contains(it.codigoSuministro ?: "") }
        if (visitados.isEmpty()) {
            Toast.makeText(context, "No hay visitas para reportar", Toast.LENGTH_SHORT).show()
            return
        }

        var mensaje = "🚀 *REPORTE DE CAMPO - CELENDÍN*%0A"
        mensaje += "📅 *Fecha:* ${SimpleDateFormat("dd/MM/yyyy").format(Date())}%0A"
        mensaje += "----------------------------%0A"

        visitados.forEachIndexed { index, v ->
            mensaje += "*${index + 1}.* Suministro: ${v.codigoSuministro ?: ""}%0A"
            mensaje += "   Beneficiario: ${v.nombres ?: ""} ${v.apellidoPaterno ?: ""}%0A%0A"
        }

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://wa.me/?text=$mensaje")
        context.startActivity(intent)
    }

    fun agruparYOrdenarNorteSur(lista: List<Cliente>): List<Cliente> {
        if (lista.isEmpty()) return lista
        val listaValida = lista.filter { !it.latitud.isNullOrEmpty() && !it.longitud.isNullOrEmpty() }
        val listaOrdenadaNorteSur = listaValida.sortedByDescending { it.latitud?.toDoubleOrNull() ?: 0.0 }

        val listaAgrupada = mutableListOf<Cliente>()
        val listaRestante = listaOrdenadaNorteSur.toMutableList()

        while (listaRestante.isNotEmpty()) {
            val pivote = listaRestante.removeAt(0)
            listaAgrupada.add(pivote)

            val vecinos = listaRestante.filter { candidato ->
                val dist = FloatArray(1)
                Location.distanceBetween(
                    pivote.latitud?.toDoubleOrNull() ?: 0.0, pivote.longitud?.toDoubleOrNull() ?: 0.0,
                    candidato.latitud?.toDoubleOrNull() ?: 0.0, candidato.longitud?.toDoubleOrNull() ?: 0.0,
                    dist
                )
                dist[0] < 100
            }

            listaAgrupada.addAll(vecinos)
            listaRestante.removeAll(vecinos)
        }

        return listaAgrupada
    }
}
