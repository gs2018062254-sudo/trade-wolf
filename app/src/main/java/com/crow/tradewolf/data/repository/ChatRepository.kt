package com.crow.tradewolf.data.repository

import android.net.Uri
import com.crow.tradewolf.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

class ChatRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Genera ID único para el chat entre dos usuarios (solo para ordenarlos)
    fun getChatId(userId1: String, userId2: String): String {
        return if (userId1 < userId2) {
            "${userId1}_$userId2"
        } else {
            "${userId2}_$userId1"
        }
    }

    //imagen a Firebase Storage
    fun uploadImageToStorage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val imageId = UUID.randomUUID().toString()
        val imageRef = storage.child("chat_images/$imageId.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error al obtener URL de la imagen")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al subir la imagen")
            }
    }

    // Enviar mensaje (solo texto)
    fun sendMessage(
        receiverId: String,
        text: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = getCurrentUserId() ?: run {
            onError("Usuario no autenticado")
            return
        }

        val chatId = getChatId(currentUserId, receiverId)
        val messageId = db.child("chats").child(chatId).child("messages").push().key ?: return

        val message = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "text" to text,
            "imageUrl" to "",
            "type" to "text",
            "visto" to false,
            "timestamp" to ServerValue.TIMESTAMP
        )

        db.child("chats").child(chatId).child("messages").child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                // Actualizar el último mensaje en la lista de chats de ambos usuarios
                updateLastMessage(chatId, currentUserId, receiverId, if(text.isNotEmpty()) text else "📷 Imagen")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al enviar mensaje")
            }
    }

    // Enviar mensaje con imagen
    fun sendMessageWithImage(
        receiverId: String,
        text: String,
        imageUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = getCurrentUserId() ?: run {
            onError("Usuario no autenticado")
            return
        }

        val chatId = getChatId(currentUserId, receiverId)
        val messageId = db.child("chats").child(chatId).child("messages").push().key ?: return

        val message = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "text" to text,
            "imageUrl" to imageUrl,
            "type" to "image",
            "visto" to false,
            "timestamp" to ServerValue.TIMESTAMP
        )

        db.child("chats").child(chatId).child("messages").child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                // Actualizar el último mensaje en la lista de chats de ambos usuarios
                updateLastMessage(chatId, currentUserId, receiverId, if(text.isNotEmpty()) text else "📷 Imagen")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al enviar mensaje")
            }
    }
    
    // Subir audio a Firebase Storage
    fun uploadAudioToStorage(
        audioFile: File,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            onError("Usuario no autenticado")
            return
        }
        
        val audioId = UUID.randomUUID().toString()
        // Carpeta separada para audios de chat
        val audioRef = storage.child("chat_audio/$audioId.m4a")
        
        audioRef.putFile(Uri.fromFile(audioFile))
            .addOnSuccessListener {
                audioRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Error al obtener URL del audio")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al subir el audio")
            }
    }
    
    // Enviar mensaje con audio
    fun sendMessageWithAudio(
        receiverId: String,
        audioUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = getCurrentUserId() ?: run {
            onError("Usuario no autenticado")
            return
        }

        val chatId = getChatId(currentUserId, receiverId)
        val messageId = db.child("chats").child(chatId).child("messages").push().key ?: return

        val message = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "text" to "",
            "imageUrl" to audioUrl,
            "type" to "audio",
            "visto" to false,
            "timestamp" to ServerValue.TIMESTAMP
        )

        db.child("chats").child(chatId).child("messages").child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                // Actualizar el último mensaje en la lista de chats de ambos usuarios
                updateLastMessage(chatId, currentUserId, receiverId, "🎤 Audio")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al enviar mensaje")
            }
    }
    
    // Enviar mensaje con ubicación
    fun sendMessageWithLocation(
        receiverId: String,
        latitude: Double,
        longitude: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUserId = getCurrentUserId() ?: run {
            onError("Usuario no autenticado")
            return
        }

        val chatId = getChatId(currentUserId, receiverId)
        val messageId = db.child("chats").child(chatId).child("messages").push().key ?: return

        val message = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "receiverId" to receiverId,
            "text" to "📍 Ubicación",
            "imageUrl" to "",
            "type" to "location",
            "latitude" to latitude,
            "longitude" to longitude,
            "visto" to false,
            "timestamp" to ServerValue.TIMESTAMP
        )

        db.child("chats").child(chatId).child("messages").child(messageId)
            .setValue(message)
            .addOnSuccessListener {
                // Actualizar el último mensaje en la lista de chats de ambos usuarios
                updateLastMessage(chatId, currentUserId, receiverId, "📍 Ubicación")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Error al enviar ubicación")
            }
    }

    // Actualizar el último mensaje del chat
    private fun updateLastMessage(
        chatId: String,
        userId1: String,
        userId2: String,
        lastMessage: String
    ) {
        val chatData = hashMapOf<String, Any>(
            "lastMessage" to lastMessage,
            "timestamp" to System.currentTimeMillis(),
            "users" to listOf(userId1, userId2)
        )

        db.child("user_chats").child(userId1).child(chatId).setValue(chatData)
        db.child("user_chats").child(userId2).child(chatId).setValue(chatData)
    }

    // Escuchar mensajes en tiempo real
    fun listenToMessages(
        receiverId: String,
        onNewMessage: (List<Message>) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val currentUserId = getCurrentUserId() ?: run {
            onError("Usuario no autenticado")
            return object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {}
                override fun onCancelled(error: DatabaseError) {}
            }
        }

        val chatId = getChatId(currentUserId, receiverId)

        val listener = db.child("chats").child(chatId).child("messages")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    for (messageSnapshot in snapshot.children) {
                        // Obtener valores con seguridad
                        val messageId = messageSnapshot.child("messageId").getValue(String::class.java) ?: ""
                        val senderId = messageSnapshot.child("senderId").getValue(String::class.java) ?: ""
                        val receiverId = messageSnapshot.child("receiverId").getValue(String::class.java) ?: ""
                        val text = messageSnapshot.child("text").getValue(String::class.java) ?: ""
                        val imageUrl = messageSnapshot.child("imageUrl").getValue(String::class.java) ?: ""
                        val type = messageSnapshot.child("type").getValue(String::class.java) ?: "text"
                        val latitude = messageSnapshot.child("latitude").getValue(Double::class.java)
                        val longitude = messageSnapshot.child("longitude").getValue(Double::class.java)
                        val visto = messageSnapshot.child("visto").getValue(Boolean::class.java) ?: false
                        val timestamp = messageSnapshot.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis()

                        val message = Message(messageId, senderId, receiverId, text, imageUrl, type, latitude, longitude, visto, timestamp)
                        messages.add(message)
                    }
                    onNewMessage(messages)
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.message)
                }
            })

        return listener
    }

    // Escuchar lista de chats del usuario
    fun listenToUserChats(
        onChatsUpdated: (List<Map<String, Any>>) -> Unit,
        onError: (String) -> Unit
    ): ValueEventListener {
        val currentUserId = getCurrentUserId() ?: run {
            onError("Usuario no autenticado")
            return object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {}
                override fun onCancelled(error: DatabaseError) {}
            }
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = mutableListOf<Map<String, Any>>()
                for (chatSnapshot in snapshot.children) {
                    val chatData = chatSnapshot.value as? Map<String, Any>
                    chatData?.let {
                        chats.add(it.toMutableMap().apply { put("chatId", chatSnapshot.key ?: "") })
                    }
                }
                chats.sortByDescending { it["timestamp"] as? Long ?: 0L }
                onChatsUpdated(chats)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        }

        db.child("user_chats").child(currentUserId).addValueEventListener(listener)
        return listener
    }

    // Obtener información de un usuario por su UID
    fun getUserInfo(
        uid: String,
        onSuccess: (Map<String, Any>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.child("usuarios").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userData = snapshot.value as? Map<String, Any>
                    if (userData != null) {
                        onSuccess(userData)
                    } else {
                        onError("Usuario no encontrado")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    onError(error.message)
                }
            })
    }
}
