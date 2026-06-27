package com.crow.tradewolf.data.repository

import com.google.firebase.firestore.FirebaseFirestore

class PostRepository {

    private val db = FirebaseFirestore.getInstance()

    fun crearPublicacion(
        titulo: String,
        descripcion: String,
        categoria: String,
        tipo: String,
        precio: String,
        whatsapp: String,
        imagenUrl: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val docRef = db.collection("publicaciones").document()

        val data = hashMapOf(
            "id" to docRef.id,
            "userId" to "temporal",
            "titulo" to titulo,
            "descripcion" to descripcion,
            "categoria" to categoria,
            "tipo" to tipo,
            "precio" to precio,
            "whatsapp" to whatsapp,
            "imagenUrl" to imagenUrl,
            "createdAt" to System.currentTimeMillis()
        )

        docRef.set(data)
            .addOnSuccessListener {
                onSuccess("Publicación guardada en Firebase")
            }
            .addOnFailureListener { e ->
                onError("Error Firebase: ${e.message}")
            }
    }
}