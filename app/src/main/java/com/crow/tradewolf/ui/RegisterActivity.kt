package com.crow.tradewolf.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crow.tradewolf.R
import com.crow.tradewolf.data.repository.AuthRepository

class RegisterActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombres = findViewById<EditText>(R.id.etNombres)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etFacultad = findViewById<EditText>(R.id.etFacultad)
        val etCodigoUniversitario = findViewById<EditText>(R.id.etCodigoUniversitario)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {

            authRepository.registrarUsuario(
                nombres = etNombres.text.toString().trim(),
                apellidos = etApellidos.text.toString().trim(),
                usuario = etUsuario.text.toString().trim(),
                email = etEmail.text.toString().trim(),
                password = etPassword.text.toString().trim(),
                telefono = etTelefono.text.toString().trim(),
                facultad = etFacultad.text.toString().trim(),
                codigoUniversitario = etCodigoUniversitario.text.toString().trim(),
                onSuccess = {},
                onError = {}
            )

            Toast.makeText(
                this,
                "Registro enviado. Ahora inicia sesión.",
                Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}