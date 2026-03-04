package com.mayo.bustrackcancun

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mayo.bustrackcancun.API.ApiService
import com.mayo.bustrackcancun.API.LoginRequest
import com.mayo.bustrackcancun.API.LoginResponse
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.82:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val etCorreo = findViewById<TextInputEditText>(R.id.etCorreo)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val contrasena = etPassword.text.toString().trim()

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Objeto para el Login
            val loginRequest = LoginRequest(correo, contrasena)

            apiService.loginUsuario(loginRequest).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()

                        // Guardar sesión en SharedPreferences
                        val sharedPref = getSharedPreferences("BusTrackPrefs", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putBoolean("isLoggedIn", true)
                            putString("userName", loginResponse?.usuario?.nombre)
                            putString("userEmail", loginResponse?.usuario?.correo)
                            apply()
                        }

                        Toast.makeText(this@LoginActivity, "¡Bienvenido, ${loginResponse?.usuario?.nombre}!", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        // Manejo de error de credenciales
                        val errorMsg = try {
                            val errorBody = response.errorBody()?.string()
                            val jObjError = JSONObject(errorBody ?: "")
                            jObjError.getString("mensaje")
                        } catch (e: Exception) {
                            "Credenciales incorrectas"
                        }
                        
                        mostrarDialogoError("Error de Acceso", errorMsg)
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("API_ERROR", "Error: ${t.message}")
                    mostrarDialogoError("Sin Conexión", "No se pudo conectar con el servidor.")
                }
            })
        }
    }

    private fun mostrarDialogoError(titulo: String, mensaje: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .setMessage(mensaje)
            .setIcon(R.drawable.ic_error_red)
            .setPositiveButton("Entendido") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}