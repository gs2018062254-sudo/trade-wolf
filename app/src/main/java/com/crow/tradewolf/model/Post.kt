package com.crow.tradewolf.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: String = "",
    val tipo: String = "",
    val precio: String = "",
    val whatsapp: String = "",
    val imagenUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)