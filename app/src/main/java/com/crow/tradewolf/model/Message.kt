package com.crow.tradewolf.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val type: String = "text",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val visto: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
