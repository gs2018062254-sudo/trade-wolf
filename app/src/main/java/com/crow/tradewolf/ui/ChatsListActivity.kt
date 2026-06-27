package com.crow.tradewolf.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crow.tradewolf.R
import com.crow.tradewolf.data.repository.ChatRepository
import com.crow.tradewolf.model.Message
import com.crow.tradewolf.utils.NotificationHelper
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.ValueEventListener

class ChatsListActivity : AppCompatActivity() {

    private lateinit var chatRepository: ChatRepository
    private lateinit var chatsAdapter: ChatsAdapter
    private lateinit var rvChats: RecyclerView
    private lateinit var btnBack: MaterialButton
    private lateinit var tvNoChats: TextView
    private lateinit var notificationHelper: NotificationHelper

    private val chats = mutableListOf<Map<String, Any>>()
    private val messageListeners = mutableMapOf<String, ValueEventListener>()
    private val lastProcessedMessage = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats_list)

        chatRepository = ChatRepository()
        notificationHelper = NotificationHelper(this)
        initViews()
        setupRecyclerView()
        listenToUserChats()
    }

    private fun initViews() {
        rvChats = findViewById(R.id.rvChats)
        btnBack = findViewById(R.id.btnBack)
        tvNoChats = findViewById(R.id.tvNoChats)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        chatsAdapter = ChatsAdapter(chats) { chatData ->
            openChat(chatData)
        }
        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = chatsAdapter
    }

    private fun listenToUserChats() {
        chatRepository.listenToUserChats(
            onChatsUpdated = { updatedChats ->
                chats.clear()
                chats.addAll(updatedChats)
                chatsAdapter.notifyDataSetChanged()

                // Iniciar escucha de mensajes para cada chat
                setupMessageListeners()

                if (chats.isEmpty()) {
                    tvNoChats.visibility = View.VISIBLE
                    rvChats.visibility = View.GONE
                } else {
                    tvNoChats.visibility = View.GONE
                    rvChats.visibility = View.VISIBLE
                }
            },
            onError = { error ->
                // Manejar error
            }
        )
    }

    private fun setupMessageListeners() {
        val currentUserId = chatRepository.getCurrentUserId() ?: return

        // Eliminar listeners antiguos
        messageListeners.values.forEach { listener ->
            chatRepository.getCurrentUserId()?.let {
                // No podemos remover listeners aquí directamente, simplificamos
            }
        }
        messageListeners.clear()

        for (chatData in chats) {
            val chatId = chatData["chatId"] as? String ?: continue
            val users = chatData["users"] as? List<String> ?: continue
            val otherUserId = users.find { it != currentUserId } ?: continue

            val listener = chatRepository.listenToMessages(
                otherUserId,
                onNewMessage = { messages ->
                    handleNewMessages(chatId, otherUserId, messages)
                },
                onError = {}
            )
            messageListeners[chatId] = listener
        }
    }

    private fun handleNewMessages(
        chatId: String,
        otherUserId: String,
        messages: List<Message>
    ) {
        val currentUserId = chatRepository.getCurrentUserId() ?: return

        // Obtener el último mensaje
        val lastMessage = messages.lastOrNull() ?: return

        // Verificar que el mensaje sea nuevo y no del usuario actual
        if (lastMessage.senderId == currentUserId) return

        // Verificar si ya procesamos este mensaje
        val lastId = lastProcessedMessage[chatId]
        if (lastId == lastMessage.messageId) return

        lastProcessedMessage[chatId] = lastMessage.messageId

        // Verificar si el chat está actualmente abierto
        if (ChatActivity.isChatOpen && ChatActivity.currentReceiverId == otherUserId) return

        // Obtener el nombre del remitente y mostrar notificación
        chatRepository.getUserInfo(
            otherUserId,
            onSuccess = { userData ->
                val nombres = userData["nombres"] as? String ?: ""
                val apellidos = userData["apellidos"] as? String ?: ""
                val fullName = "$nombres $apellidos".ifBlank { "Usuario" }

                val messageText = if (lastMessage.imageUrl.isNotEmpty()) {
                    "📷 Imagen"
                } else {
                    lastMessage.text
                }

                notificationHelper.showNewMessageNotification(
                    lastMessage.messageId,
                    fullName,
                    messageText,
                    otherUserId
                )
            },
            onError = {
                notificationHelper.showNewMessageNotification(
                    lastMessage.messageId,
                    "Usuario",
                    if (lastMessage.imageUrl.isNotEmpty()) "📷 Imagen" else lastMessage.text,
                    otherUserId
                )
            }
        )
    }

    private fun openChat(chatData: Map<String, Any>) {
        val currentUserId = chatRepository.getCurrentUserId() ?: return
        val users = chatData["users"] as? List<String> ?: return

        // Obtener el ID del otro usuario
        val otherUserId = users.find { it != currentUserId } ?: return

        // Obtener información del otro usuario
        chatRepository.getUserInfo(
            otherUserId,
            onSuccess = { userData ->
                val nombres = userData["nombres"] as? String ?: ""
                val apellidos = userData["apellidos"] as? String ?: ""
                val fullName = "$nombres $apellidos"

                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("RECEIVER_ID", otherUserId)
                intent.putExtra("RECEIVER_NAME", fullName)
                startActivity(intent)
            },
            onError = {
                // Fallback: abrir con ID
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("RECEIVER_ID", otherUserId)
                startActivity(intent)
            }
        )
    }

    inner class ChatsAdapter(
        private val chats: List<Map<String, Any>>,
        private val onChatClick: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

        inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvAvatar: TextView = itemView.findViewById(R.id.tvAvatar)
            private val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
            private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)

            fun bind(chatData: Map<String, Any>) {
                val currentUserId = chatRepository.getCurrentUserId() ?: ""
                val users = chatData["users"] as? List<String> ?: return
                val otherUserId = users.find { it != currentUserId } ?: return

                // Cargar información del usuario
                chatRepository.getUserInfo(
                    otherUserId,
                    onSuccess = { userData ->
                        val nombres = userData["nombres"] as? String ?: ""
                        val apellidos = userData["apellidos"] as? String ?: ""
                        val fullName = "$nombres $apellidos"
                        tvUserName.text = fullName

                        // Avatar con iniciales
                        val initials = if (nombres.isNotEmpty() && apellidos.isNotEmpty()) {
                            "${nombres.first()}${apellidos.first()}"
                        } else {
                            "U"
                        }
                        tvAvatar.text = initials.uppercase()
                    },
                    onError = {
                        tvUserName.text = "Usuario"
                        tvAvatar.text = "U"
                    }
                )

                tvLastMessage.text = chatData["lastMessage"] as? String ?: ""

                itemView.setOnClickListener {
                    onChatClick(chatData)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            holder.bind(chats[position])
        }

        override fun getItemCount(): Int = chats.size
    }
}
