package com.crow.tradewolf.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.crow.tradewolf.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class EditProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val etNombres = findViewById<EditText>(R.id.etNombres)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etTelefono = findViewById<EditText>(R.id.etTelefono)
        val etFacultad = findViewById<EditText>(R.id.etFacultad)
        val etCodigoUniversitario = findViewById<EditText>(R.id.etCodigoUniversitario)
        val tvEmail = findViewById<TextView>(R.id.tvEmail)
        val btnGuardarPerfil = findViewById<MaterialButton>(R.id.btnGuardarPerfil)

        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "No hay usuario activo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val uid = user.uid
        val email = user.email ?: ""

        tvEmail.text = "Correo: $email"

        db.child("usuarios")
            .child(uid)
            .get()
            .addOnSuccessListener { snapshot ->

                etNombres.setText(snapshot.child("nombres").getValue(String::class.java) ?: "")
                etApellidos.setText(snapshot.child("apellidos").getValue(String::class.java) ?: "")
                etUsuario.setText(snapshot.child("usuario").getValue(String::class.java) ?: "")
                etTelefono.setText(snapshot.child("telefono").getValue(String::class.java) ?: "")
                etFacultad.setText(snapshot.child("facultad").getValue(String::class.java) ?: "")
                etCodigoUniversitario.setText(snapshot.child("codigoUniversitario").getValue(String::class.java) ?: "")
            }

        btnGuardarPerfil.setOnClickListener {

            val data = hashMapOf(
                "uid" to uid,
                "email" to email,
                "nombres" to etNombres.text.toString().trim(),
                "apellidos" to etApellidos.text.toString().trim(),
                "usuario" to etUsuario.text.toString().trim(),
                "telefono" to etTelefono.text.toString().trim(),
                "facultad" to etFacultad.text.toString().trim(),
                "codigoUniversitario" to etCodigoUniversitario.text.toString().trim(),
                "updatedAt" to System.currentTimeMillis()
            )

            db.child("usuarios")
                .child(uid)
                .setValue(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}