package com.crow.tradewolf.model

data class AppUser(
    val uid: String = "",
    val nombres: String = "",
    val apellidos: String = "",
    val usuario: String = "",
    val email: String = "",
    val telefono: String = "",
    val facultad: String = "",
    val codigoUniversitario: String = "",
    val codigoUnico: String = "",
    val correoVerificado: Boolean = false,
    val rol: String = "usuario",
    val createdAt: Long = System.currentTimeMillis()
)