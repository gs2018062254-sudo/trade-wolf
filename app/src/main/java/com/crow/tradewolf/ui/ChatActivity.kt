package com.crow.tradewolf.ui

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.crow.tradewolf.R
import com.crow.tradewolf.data.repository.ChatRepository
import com.crow.tradewolf.model.Message
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
        var isChatOpen = false
        var currentReceiverId: String? = null
    }

    private lateinit var chatRepository: ChatRepository
    private lateinit var messagesAdapter: MessagesAdapter

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: TextInputEditText
    private lateinit var btnSend: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var btnGallery: MaterialButton
    private lateinit var btnCamera: MaterialButton
    private lateinit var btnAudio: MaterialButton
    private lateinit var btnLocation: MaterialButton
    private lateinit var btnComprar: MaterialButton
    private lateinit var tvChatUser: TextView

    private var receiverId: String = ""
    private var receiverName: String = ""
    private var selectedImageUri: Uri? = null
    private var messageListener: ValueEventListener? = null
    private var isNewChatWithProduct = false // Bandera para enviar el mensaje solo una vez

    private val messages = mutableListOf<Message>()
    
    // Variables para grabación de audio
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var handler = Handler(Looper.getMainLooper())
    private var recordingStartTime: Long = 0
    
    // Variable para reproducción de audio
    private var mediaPlayer: MediaPlayer? = null
    
    // Variables para ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocationAndSend()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            mostrarVistaPreviaImagen(uri)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val uri = guardarBitmapTemporal(bitmap)

            if (uri != null) {
                selectedImageUri = uri
                mostrarVistaPreviaImagen(uri)
            } else {
                Toast.makeText(this, "No se pudo preparar la foto", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No se tomó ninguna foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->

        val galleryGranted =
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
                    permissions[Manifest.permission.READ_MEDIA_IMAGES] == true

        val cameraGranted =
            permissions[Manifest.permission.CAMERA] == true

        if (galleryGranted) {
            openGallery()
        }

        if (cameraGranted) {
            openCamera()
        }

        if (!galleryGranted && !cameraGranted) {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatRepository = ChatRepository()

        receiverId = intent.getStringExtra("RECEIVER_ID") ?: ""
        receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "Usuario"
        
        // Verificar si hay datos de producto para enviar el mensaje automático
        val productId = intent.getStringExtra("PRODUCT_ID")
        val productTitle = intent.getStringExtra("PRODUCT_TITLE")
        val productPrice = intent.getStringExtra("PRODUCT_PRICE")
        val productImageUrl = intent.getStringExtra("PRODUCT_IMAGE_URL")
        
        isNewChatWithProduct = productId != null

        if (receiverId.isBlank()) {
            Toast.makeText(this, "No se encontró el usuario del chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentReceiverId = receiverId

        initViews()
        setupRecyclerView()
        loadReceiverInfo()
        listenToMessages()
        
        // Enviar mensaje automático solo si hay datos de producto y es la primera vez
        if (isNewChatWithProduct && productTitle != null) {
            val productMessage = "¡Hola! Estoy interesado/a en: $productTitle\nPrecio: S/ ${productPrice ?: "0.00"}"
            sendAutoMessage(productMessage, productImageUrl)
        }
    }

    override fun onStart() {
        super.onStart()
        isChatOpen = true
    }

    override fun onStop() {
        super.onStop()
        isChatOpen = false
        currentReceiverId = null
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBack = findViewById(R.id.btnBack)
        btnGallery = findViewById(R.id.btnGallery)
        btnCamera = findViewById(R.id.btnCamera)
        btnAudio = findViewById(R.id.btnAudio)
        btnLocation = findViewById(R.id.btnLocation)
        btnComprar = findViewById(R.id.btnComprar)
        tvChatUser = findViewById(R.id.tvChatUser)
        
        // Inicializar cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tvChatUser.text = receiverName

        btnBack.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()

            if (text.isNotEmpty()) {
                sendTextMessage(text)
            }
        }

        btnGallery.setOnClickListener {
            checkGalleryPermissions()
        }

        btnCamera.setOnClickListener {
            checkCameraPermissions()
        }
        
        btnAudio.setOnClickListener {
            toggleAudioRecording()
        }
        
        btnLocation.setOnClickListener {
            checkLocationPermissionsAndSend()
        }

        btnComprar.setOnClickListener {
            mostrarOpcionesDePago()
        }
    }
    
    private fun checkLocationPermissionsAndSend() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocationAndSend()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    private fun getCurrentLocationAndSend() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            sendLocationMessage(location.latitude, location.longitude)
                        } else {
                            Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al obtener ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun sendLocationMessage(latitude: Double, longitude: Double) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Enviando ubicación...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        chatRepository.sendMessageWithLocation(
            receiverId,
            latitude,
            longitude,
            onSuccess = {
                progressDialog.dismiss()
                Toast.makeText(this, "Ubicación enviada", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun openLocationInMaps(latitude: Double, longitude: Double) {
        try {
            val uri = android.net.Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Ubicación)")
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val uriWeb = android.net.Uri.parse("https://www.google.com/maps?q=$latitude,$longitude")
                val intentWeb = android.content.Intent(android.content.Intent.ACTION_VIEW, uriWeb)
                startActivity(intentWeb)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al abrir ubicación: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun mostrarOpcionesDePago() {
        val opciones = arrayOf("Yape", "Efectivo", "Transferencia bancaria")
        
        AlertDialog.Builder(this)
            .setTitle("Método de pago")
            .setItems(opciones) { _, which ->
                val metodoSeleccionado = opciones[which]
                // Enviamos un mensaje en el chat con el metodo que selecciono
                enviarMensajeMetodoDePago(metodoSeleccionado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun enviarMensajeMetodoDePago(metodo: String) {
        val mensaje = "¡Hola! Quiero pagar con $metodo"
        
        // Enviamos el mensaje como texto normal
        chatRepository.sendMessage(
            receiverId,
            mensaje,
            onSuccess = {
                // El mensaje se agrega automáticamente a través del listener en tiempo real
                Toast.makeText(this, "Método de pago enviado: $metodo", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(this, "Error al enviar mensaje: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(
            messages,
            chatRepository.getCurrentUserId() ?: ""
        )

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true

        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = messagesAdapter
    }

    private fun loadReceiverInfo() {
        chatRepository.getUserInfo(
            receiverId,
            onSuccess = { userData ->
                val nombres = userData["nombres"] as? String ?: ""
                val apellidos = userData["apellidos"] as? String ?: ""

                val nombreCompleto = "$nombres $apellidos".trim()

                tvChatUser.text = nombreCompleto.ifBlank {
                    receiverName.ifBlank { "Usuario" }
                }
            },
            onError = {
                tvChatUser.text = receiverName.ifBlank { "Usuario" }
            }
        )
    }

    private fun listenToMessages() {
        messageListener = chatRepository.listenToMessages(
            receiverId,
            onNewMessage = { newMessages ->
                messages.clear()
                messages.addAll(newMessages)

                messagesAdapter.notifyDataSetChanged()

                if (messages.isNotEmpty()) {
                    rvMessages.scrollToPosition(messages.size - 1)
                }
            },
            onError = { error ->
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        messageListener?.let {
            val currentUserId = chatRepository.getCurrentUserId()

            if (currentUserId != null) {
                val chatId = chatRepository.getChatId(currentUserId, receiverId)
                val db = FirebaseDatabase.getInstance().reference

                db.child("chats")
                    .child(chatId)
                    .child("messages")
                    .removeEventListener(it)
            }
        }
        
        isChatOpen = false
        currentReceiverId = null
        
        // Detener la grabación si la actividad se destruye
        if (isRecording) {
            stopRecording()
        }
        // Detener la reproducción de audio
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun sendTextMessage(text: String) {
        btnSend.isEnabled = false

        chatRepository.sendMessage(
            receiverId,
            text,
            onSuccess = {
                etMessage.text?.clear()
                btnSend.isEnabled = true
            },
            onError = { error ->
                btnSend.isEnabled = true
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun sendAutoMessage(text: String, imageUrl: String?) {
        btnSend.isEnabled = false

        chatRepository.sendMessage(
            receiverId,
            text,
            onSuccess = {
                etMessage.text?.clear()
                btnSend.isEnabled = true
                isNewChatWithProduct = false // Resetear la bandera despues de enviar
            },
            onError = { error ->
                btnSend.isEnabled = true
                isNewChatWithProduct = false
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun sendImageMessage() {
        val imageUri = selectedImageUri ?: return
        val text = etMessage.text.toString().trim()

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Subiendo imagen...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        chatRepository.uploadImageToStorage(
            imageUri,
            onSuccess = { publicUrl ->
                progressDialog.setMessage("Enviando mensaje...")

                chatRepository.sendMessageWithImage(
                    receiverId,
                    text,
                    publicUrl,
                    onSuccess = {
                        progressDialog.dismiss()

                        etMessage.text?.clear()
                        selectedImageUri = null

                        Toast.makeText(this, "Mensaje enviado", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        progressDialog.dismiss()

                        Toast.makeText(
                            this,
                            "Error al enviar mensaje: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            onError = { error ->
                progressDialog.dismiss()

                Toast.makeText(
                    this,
                    "Error al subir imagen: $error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    private fun checkGalleryPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            openGallery()
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun checkCameraPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun guardarBitmapTemporal(bitmap: Bitmap): Uri? {
        return try {
            val file = File(cacheDir, "chat_photo_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)

            outputStream.flush()
            outputStream.close()

            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun mostrarVistaPreviaImagen(uri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_preview_chat, null)

        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivPreviewChatImage)
        val etCaption = dialogView.findViewById<TextInputEditText>(R.id.etCaptionImage)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancelImage)
        val btnSendImage = dialogView.findViewById<MaterialButton>(R.id.btnSendImage)

        Glide.with(this)
            .load(uri)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(ivPreview)

        etCaption.setText(etMessage.text.toString().trim())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            selectedImageUri = null
            dialog.dismiss()
        }

        btnSendImage.setOnClickListener {
            val caption = etCaption.text.toString().trim()

            etMessage.setText(caption)

            dialog.dismiss()
            sendImageMessage()
        }

        dialog.show()
    }
    
    private fun checkAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1001)
            }
        } else {
            startRecording()
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(this, "Se necesita permiso para grabar audio", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun toggleAudioRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            checkAudioPermissions()
        }
    }
    
    private fun startRecording() {
        try {
            val audioDir = File(getExternalFilesDir(null), "audio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            audioFile = File(audioDir, "audio_$timestamp.m4a")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
            
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            
            // Cambiar el ícono del botón cuando está grabando
            btnAudio.setIconResource(android.R.drawable.ic_menu_save)
            btnAudio.setIconTintResource(android.R.color.holo_red_dark)
            
            Toast.makeText(this, "Grabando audio...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al grabar audio", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        mediaRecorder = null
        isRecording = false
        
        // Restaurar el ícono del botón
        btnAudio.setIconResource(android.R.drawable.ic_btn_speak_now)
        btnAudio.setIconTintResource(android.R.color.holo_green_dark)
        
        // Enviar el audio grabado
        audioFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                sendAudioMessage(file)
            }
        }
    }
    
    private fun sendAudioMessage(audioFile: File) {
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Enviando audio...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        chatRepository.uploadAudioToStorage(
            audioFile,
            onSuccess = { audioUrl ->
                chatRepository.sendMessageWithAudio(
                    receiverId,
                    audioUrl,
                    onSuccess = {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Audio enviado", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onError = { error ->
                progressDialog.dismiss()
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    // Función para reproducir audio mejorada
    private fun playAudio(audioUri: Uri) {
        try {
            // Detener cualquier reproducción anterior
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            
            // Crear nuevo MediaPlayer
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setDataSource(this@ChatActivity, audioUri)
                
                setOnPreparedListener {
                    it.start()
                    Toast.makeText(this@ChatActivity, "Reproduciendo audio...", Toast.LENGTH_SHORT).show()
                }
                
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                
                setOnErrorListener { _, what, extra ->
                    Toast.makeText(this@ChatActivity, "Error al reproducir: $what", Toast.LENGTH_SHORT).show()
                    true
                }
                
                prepareAsync()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al reproducir audio: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class MessagesAdapter(
        private val messages: List<Message>,
        private val currentUserId: String
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemViewType(position: Int): Int {
            val message = messages[position]

            return if (message.senderId == currentUserId) {
                VIEW_TYPE_SENT
            } else {
                VIEW_TYPE_RECEIVED
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            return if (viewType == VIEW_TYPE_SENT) {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)

                SentMessageViewHolder(view)
            } else {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)

                ReceivedMessageViewHolder(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val message = messages[position]

            if (holder is SentMessageViewHolder) {
                holder.bind(message)
            } else if (holder is ReceivedMessageViewHolder) {
                holder.bind(message)
            }
        }

        override fun getItemCount(): Int = messages.size

        inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val tvMessageText: TextView =
                itemView.findViewById(R.id.tvMessageText)

            private val tvMessageTime: TextView =
                itemView.findViewById(R.id.tvMessageTime)

            private val ivMessageImage: ImageView =
                itemView.findViewById(R.id.ivMessageImage)
                
            private val llAudioMessage: LinearLayout =
                itemView.findViewById(R.id.llAudioMessage)
                
            private val btnPlayAudio: MaterialButton =
                itemView.findViewById(R.id.btnPlayAudio)
                
            private val tvAudioDuration: TextView =
                itemView.findViewById(R.id.tvAudioDuration)
                
            private val llLocationMessage: LinearLayout =
                itemView.findViewById(R.id.llLocationMessage)
                
            private val btnOpenLocation: MaterialButton =
                itemView.findViewById(R.id.btnOpenLocation)
                
            private val tvLocationText: TextView =
                itemView.findViewById(R.id.tvLocationText)

            fun bind(message: Message) {
                bindMessageContent(
                    itemView = itemView,
                    message = message,
                    tvMessageText = tvMessageText,
                    tvMessageTime = tvMessageTime,
                    ivMessageImage = ivMessageImage,
                    llAudioMessage = llAudioMessage,
                    btnPlayAudio = btnPlayAudio,
                    tvAudioDuration = tvAudioDuration,
                    llLocationMessage = llLocationMessage,
                    btnOpenLocation = btnOpenLocation,
                    tvLocationText = tvLocationText
                )
            }
        }

        inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val tvMessageText: TextView =
                itemView.findViewById(R.id.tvMessageText)

            private val tvMessageTime: TextView =
                itemView.findViewById(R.id.tvMessageTime)

            private val ivMessageImage: ImageView =
                itemView.findViewById(R.id.ivMessageImage)
                
            private val llAudioMessage: LinearLayout =
                itemView.findViewById(R.id.llAudioMessage)
                
            private val btnPlayAudio: MaterialButton =
                itemView.findViewById(R.id.btnPlayAudio)
                
            private val tvAudioDuration: TextView =
                itemView.findViewById(R.id.tvAudioDuration)
                
            private val llLocationMessage: LinearLayout =
                itemView.findViewById(R.id.llLocationMessage)
                
            private val btnOpenLocation: MaterialButton =
                itemView.findViewById(R.id.btnOpenLocation)
                
            private val tvLocationText: TextView =
                itemView.findViewById(R.id.tvLocationText)

            fun bind(message: Message) {
                bindMessageContent(
                    itemView = itemView,
                    message = message,
                    tvMessageText = tvMessageText,
                    tvMessageTime = tvMessageTime,
                    ivMessageImage = ivMessageImage,
                    llAudioMessage = llAudioMessage,
                    btnPlayAudio = btnPlayAudio,
                    tvAudioDuration = tvAudioDuration,
                    llLocationMessage = llLocationMessage,
                    btnOpenLocation = btnOpenLocation,
                    tvLocationText = tvLocationText
                )
            }
        }

        private fun bindMessageContent(
            itemView: View,
            message: Message,
            tvMessageText: TextView,
            tvMessageTime: TextView,
            ivMessageImage: ImageView,
            llAudioMessage: LinearLayout,
            btnPlayAudio: MaterialButton,
            tvAudioDuration: TextView,
            llLocationMessage: LinearLayout,
            btnOpenLocation: MaterialButton,
            tvLocationText: TextView
        ) {
            if (message.type == "audio") {
                llAudioMessage.visibility = View.VISIBLE
                ivMessageImage.visibility = View.GONE
                tvMessageText.visibility = View.GONE
                llLocationMessage.visibility = View.GONE
                
                btnPlayAudio.setOnClickListener {
                    playAudio(Uri.parse(message.imageUrl))
                }
                
            } else if (message.type == "location" && message.latitude != null && message.longitude != null) {
                llLocationMessage.visibility = View.VISIBLE
                ivMessageImage.visibility = View.GONE
                tvMessageText.visibility = View.GONE
                llAudioMessage.visibility = View.GONE
                
                tvLocationText.text = "📍 Ubicación: ${String.format("%.4f", message.latitude)}, ${String.format("%.4f", message.longitude)}"
                
                btnOpenLocation.setOnClickListener {
                    openLocationInMaps(message.latitude, message.longitude)
                }
                
            } else if (message.imageUrl.isNotEmpty()) {
                llAudioMessage.visibility = View.GONE
                ivMessageImage.visibility = View.VISIBLE
                tvMessageText.visibility = View.VISIBLE
                llLocationMessage.visibility = View.GONE

                Glide.with(itemView.context)
                    .load(Uri.parse(message.imageUrl))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivMessageImage)

                ivMessageImage.setOnClickListener {
                    mostrarImagenGrande(message.imageUrl)
                }
            } else {
                llAudioMessage.visibility = View.GONE
                ivMessageImage.visibility = View.GONE
                llLocationMessage.visibility = View.GONE
            }

            if (message.text.isNotEmpty()) {
                tvMessageText.text = message.text
                tvMessageText.visibility = View.VISIBLE
            } else {
                tvMessageText.visibility = View.GONE
            }

            tvMessageTime.text = formatTimestamp(message.timestamp)
        }

        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private fun mostrarImagenGrande(imageUrl: String) {
        val imageView = ImageView(this)

        imageView.adjustViewBounds = true
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setPadding(8, 8, 8, 8)

        Glide.with(this)
            .load(Uri.parse(imageUrl))
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_report_image)
            .into(imageView)

        AlertDialog.Builder(this)
            .setView(imageView)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}