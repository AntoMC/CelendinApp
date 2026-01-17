package com.amc.celendinapp.model
import com.google.gson.annotations.SerializedName
data class Cliente(
    val CÓDIGO_DE_SUMINISTRO2: String,
    val NOMBRES: String,
    val APELLIDO_PATERNO: String,
    val APELLIDO_MATERNO: String,
    val N__DNI: String,
    val LOCALIDAD: String,
    val DISTRITO: String,
    val LATITUD2: String,
    val LONGITUD2: String,
    val ESTADO__SFD2: String,
    var isVisitado: Boolean = false
)


data class RespuestaAdinelsa(
    val size: Int,
    val instalaciones: List<InstalacionApi>
)

data class InstalacionApi(
    @SerializedName("c_codigosuministro") val suministro: String,
    @SerializedName("c_nombrepersona") val nombreCompleto: String,
    @SerializedName("c_nrodni") val dni: String,
    @SerializedName("c_latitud") val lat: String,
    @SerializedName("c_longitud") val lon: String,
    @SerializedName("c_centropoblado") val centroPoblado: String,
    @SerializedName("c_distrito") val distrito: String,
    @SerializedName("b_activo") val estaActivo: Boolean // <--- Agregamos esto
)

// --- EL TRADUCTOR (Función de extensión) ---
fun InstalacionApi.toCliente(): Cliente {
    return Cliente(
        CÓDIGO_DE_SUMINISTRO2 = this.suministro,
        NOMBRES = this.nombreCompleto.trim(),
        APELLIDO_PATERNO = "",
        APELLIDO_MATERNO = "",
        N__DNI = this.dni,
        LOCALIDAD = this.centroPoblado,
        DISTRITO = this.distrito,
        //LOCALIDAD = "${this.distrito} / ${this.centroPoblado}",
        LATITUD2 = this.lat,
        LONGITUD2 = this.lon,
        // Si b_activo es true pone "ACTIVO", si es false pone "INACTIVO"
        ESTADO__SFD2 = if (this.estaActivo) "ACTIVO" else "INACTIVO",

        isVisitado = false
    )
}