package com.amc.celendinapp.network

import com.amc.celendinapp.model.RespuestaAdinelsa
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface AdinelsaApiService {
    @GET("api/movil/getinstalacionpaginado")
    suspend fun obtenerInstalaciones(
        @Query("n_idgen_grupo") grupo: Int = 38,
        @Query("num_page") pagina: Int = 0,
        @Query("n_idgen_tipoprograma") programa: Int = 3
    ): RespuestaAdinelsa
}

object RetrofitClient {
    private const val BASE_URL = "https://fotovoltaicos-api.adinelsa.com.pe/"

    // Esta función crea un cliente que ignora los errores de certificado SSL
    private fun getUnsafeOkHttpClient(): OkHttpClient {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    val instancia: AdinelsaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getUnsafeOkHttpClient()) // <--- Aquí inyectamos el permiso especial
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdinelsaApiService::class.java)
    }
}