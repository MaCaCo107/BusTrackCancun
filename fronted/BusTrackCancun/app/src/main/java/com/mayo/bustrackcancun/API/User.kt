package com.mayo.bustrackcancun.API

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class UserRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String
)

data class LoginRequest(
    @SerializedName("correo") val correo: String,
    @SerializedName("contrasena") val contrasena: String
)

data class LoginResponse(
    @SerializedName("mensaje") val mensaje: String,
    @SerializedName("usuario") val usuario: UserData?

)

data class UserData(
    @SerializedName("_id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("correo") val correo: String
)

data class Ruta(
    @SerializedName("_id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("origenNombre") val origenNombre: String?,
    @SerializedName("destinoNombre") val destinoNombre: String?,
    @SerializedName("trazado") val trazado: List<Coordenada>? = null
)

data class Coordenada(
    @SerializedName("latitud") val lat: Double,
    @SerializedName("longitud") val lng: Double
)

data class TrazadoResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("trazado") val coordenadas: List<Coordenada>
)

data class ReporteRequest(
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("latitud") val latitud: Double,
    @SerializedName("longitud") val longitud: Double,
    @SerializedName("nombreUsuario") val usuarioNombre: String,
    @SerializedName("fecha") val fecha: String
)

data class UbicacionReporte(
    @SerializedName("coordenadas") val coords: List<Double>
)

data class ReporteResponse(
    @SerializedName("_id") val id: String,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("nombreUsuario") val usuario: String,
    @SerializedName("fecha") val fecha: String,
    @SerializedName("ubicacion") val ubicacion: UbicacionReporte
)

interface ApiService {
    @POST("api/usuarios")
    fun registrarUsuario(@Body user: UserRequest): Call<ResponseBody>

    @POST("api/login")
    fun loginUsuario(@Body login: LoginRequest): Call<LoginResponse>

    @GET("api/rutas")
    fun obtenerRutas(): Call<List<Ruta>>

    @GET("api/rutas/{id}/trazado")
    fun obtenerTrazado(@Path("id") id: String): Call<TrazadoResponse>

    @GET("api/rutas/buscar")
    fun buscarRutas(@Query("q") query: String): Call<List<Ruta>>

    @GET("api/reportes")
    fun obtenerReportes(): Call<List<ReporteResponse>>

    @POST("api/reportes")
    fun crearReporte(@Body reporte: ReporteRequest): Call<ResponseBody>

    @DELETE("api/reportes/{id}")
    suspend fun eliminarReporte(
        @Path("id") id: String,
        @Query("usuario") usuario: String
    ): Response<ResponseBody>
}