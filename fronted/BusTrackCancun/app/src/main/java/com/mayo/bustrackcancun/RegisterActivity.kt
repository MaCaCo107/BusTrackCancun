package com.mayo.bustrackcancun

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mayo.bustrackcancun.API.ApiService
import com.mayo.bustrackcancun.API.UserRequest
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.82:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val etNombre = findViewById<TextInputEditText>(R.id.etNombre)
        val etCorreo = findViewById<TextInputEditText>(R.id.etCorreo)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val contrasena = etPassword.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userRequest = UserRequest(nombre, correo, contrasena)

            apiService.registrarUsuario(userRequest).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        mostrarDialogoCustom(
                            "Registro Exitoso",
                            "¡Cuenta creada con éxito!",
                            true
                        )
                    } else {
                        val errorMsg = try {
                            val jObjError = JSONObject(response.errorBody()?.string() ?: "")
                            jObjError.getString("mensaje")
                        } catch (e: Exception) {
                            "Ocurrió un error inesperado (${response.code()})"
                        }
                        
                        mostrarDialogoCustom(
                            "Error de Registro",
                            errorMsg,
                            false
                        )
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("API_ERROR", "Error: ${t.message}")
                    mostrarDialogoCustom(
                        "Sin Conexión",
                        "No se pudo conectar con el servidor. Verifica tu internet.",
                        false
                    )
                }
            })
        }
    }

    private fun mostrarDialogoCustom(titulo: String, mensaje: String, esExito: Boolean) {
        val iconRes = if (esExito) R.drawable.ic_check_green else R.drawable.ic_error_red
        
        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setIcon(iconRes)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
                if (esExito) finish() // Cerrar actividad si fue exitoso
            }
            .show()
    }
}