package com.crow.tradewolf.data.repository

import com.crow.tradewolf.utils.CodeGenerator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun login(
        email: String,
        password: String,
        callback: (Boolean, String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                callback(true, "Inicio de sesión exitoso")
            }
            .addOnFailureListener { e ->
                callback(false, "Error al iniciar sesión: ${e.message}")
            }
    }

    fun registrarUsuario(
        nombres: String,
        apellidos: String,
        usuario: String,
        email: String,
        password: String,
        telefono: String,
        facultad: String,
        codigoUniversitario: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val userFirebase = result.user

                if (userFirebase == null) {
                    onError("No se pudo crear el usuario.")
                    return@addOnSuccessListener
                }

                val uid = userFirebase.uid

                val data = hashMapOf(
                    "uid" to uid,
                    "email" to email,
                    "nombres" to nombres,
                    "apellidos" to apellidos,
                    "usuario" to usuario,
                    "telefono" to telefono,
                    "facultad" to facultad,
                    "codigoUniversitario" to codigoUniversitario,
                    "codigoUnico" to CodeGenerator.generarCodigoUnico(),
                    "rol" to "usuario",
                    "createdAt" to System.currentTimeMillis()
                )

                db.child("usuarios")
                    .child(uid)
                    .setValue(data)
                    .addOnSuccessListener {
                        auth.signOut()
                        onSuccess("Registro exitoso. Ahora inicia sesión.")
                    }
                    .addOnFailureListener { e ->
                        auth.signOut()
                        onError("Usuario creado, pero falló guardar perfil: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->

                if (e is FirebaseAuthUserCollisionException) {
                    onError("Esta cuenta ya existe. Inicia sesión.")
                } else {
                    onError("Error registrando usuario: ${e.message}")
                }
            }
    }

    fun logout() {
        auth.signOut()
    }
}