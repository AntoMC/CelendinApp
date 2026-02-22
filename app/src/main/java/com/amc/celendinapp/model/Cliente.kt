package com.amc.celendinapp.model
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Cliente(
    @SerializedName("codigo_suministro") val codigoSuministro: String?,
    @SerializedName("nombres") val nombres: String?,
    @SerializedName("apellido_p") val apellidoPaterno: String?,
    @SerializedName("apellido_m") val apellidoMaterno: String?,
    @SerializedName("dni") val dni: String?,
    @SerializedName("localidad") val localidad: String?,
    @SerializedName("distrito") val distrito: String?,
    @SerializedName("latitud") val latitud: String?,
    @SerializedName("longitud") val longitud: String?,
    @SerializedName("estado") val estado: String?,
    @SerializedName("visitado") var isVisitado: Boolean = false
)

@Keep
data class ClienteLocal(
    @SerializedName("DISTRITO") val distrito: String?,
    @SerializedName("LOCALIDAD") val localidad: String?,
    @SerializedName("N__DNI") val dni: String?,
    @SerializedName("CÓDIGO_DE_SUMINISTRO2") val suministro: String?,
    @SerializedName("NOMBRES") val nombres: String?,
    @SerializedName("APELLIDO_PATERNO") val apellidoP: String?,
    @SerializedName("APELLIDO_MATERNO") val apellidoM: String?,
    @SerializedName("LATITUD2") val lat: String?,
    @SerializedName("LONGITUD2") val lon: String?,
    @SerializedName("ESTADO__SFD2") val estado: String?
)

fun ClienteLocal.toCliente(): Cliente = Cliente(
    codigoSuministro = this.suministro,
    nombres = this.nombres,
    apellidoPaterno = this.apellidoP,
    apellidoMaterno = this.apellidoM,
    dni = this.dni,
    localidad = this.localidad,
    distrito = this.distrito,
    latitud = this.lat,
    longitud = this.lon,
    estado = this.estado,
    isVisitado = false
)

@Keep
data class RespuestaAdinelsa(
    @SerializedName("size") val size: Int,
    @SerializedName("instalaciones") val instalaciones: List<InstalacionApi>
)

@Keep
data class InstalacionApi(
    @SerializedName("c_codigosuministro") val suministro: String?,
    @SerializedName("c_nombrepersona") val nombreCompleto: String?,
    @SerializedName("c_nrodni") val dni: String?,
    @SerializedName("c_latitud") val lat: String?,
    @SerializedName("c_longitud") val lon: String?,
    @SerializedName("c_centropoblado") val centroPoblado: String?,
    @SerializedName("c_distrito") val distrito: String?,
    @SerializedName("b_activo") val estaActivo: Boolean
)

fun InstalacionApi.toCliente(): Cliente = Cliente(
    codigoSuministro = this.suministro,
    nombres = this.nombreCompleto?.trim(),
    apellidoPaterno = "",
    apellidoMaterno = "",
    dni = this.dni,
    localidad = this.centroPoblado,
    distrito = this.distrito,
    latitud = this.lat,
    longitud = this.lon,
    estado = if (this.estaActivo) "ACTIVO" else "INACTIVO",
    isVisitado = false
)
