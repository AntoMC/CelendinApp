package com.amc.celendinapp.model
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
@Keep
data class Cliente(
    @SerializedName("codigo_suministro") val CÓDIGO_DE_SUMINISTRO2: String,
    @SerializedName("nombres") val NOMBRES: String,
    @SerializedName("apellido_p") val APELLIDO_PATERNO: String,
    @SerializedName("apellido_m") val APELLIDO_MATERNO: String,
    @SerializedName("dni") val N__DNI: String,
    @SerializedName("localidad") val LOCALIDAD: String,
    @SerializedName("distrito") val DISTRITO: String,
    @SerializedName("latitud") val LATITUD2: String,
    @SerializedName("longitud") val LONGITUD2: String,
    @SerializedName("estado") val ESTADO__SFD2: String,
    @SerializedName("is_visitado") var isVisitado: Boolean = false
)

// RespuestaAdinelsa también necesita protección
@Keep
data class RespuestaAdinelsa(
    @SerializedName("size") val size: Int,
    @SerializedName("instalaciones") val instalaciones: List<InstalacionApi>
)
@Keep
data class InstalacionApi(
    @SerializedName("c_codigosuministro") val suministro: String,
    @SerializedName("c_nombrepersona") val nombreCompleto: String,
    @SerializedName("c_nrodni") val dni: String,
    @SerializedName("c_latitud") val lat: String,
    @SerializedName("c_longitud") val lon: String,
    @SerializedName("c_centropoblado") val centroPoblado: String,
    @SerializedName("c_distrito") val distrito: String,
    @SerializedName("b_activo") val estaActivo: Boolean
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