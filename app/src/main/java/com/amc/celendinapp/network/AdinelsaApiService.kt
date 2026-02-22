package com.amc.celendinapp.network

import android.util.Log
import androidx.annotation.Keep
import com.amc.celendinapp.model.RespuestaAdinelsa
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Keep
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
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    val instancia: AdinelsaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdinelsaApiService::class.java)
    }
}
