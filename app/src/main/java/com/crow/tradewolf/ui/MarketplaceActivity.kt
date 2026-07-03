package com.crow.tradewolf.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.crow.tradewolf.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.net.URL
import kotlin.concurrent.thread

class MarketplaceActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var etBuscarMarketplace: EditText
    private lateinit var spFiltroCategoriaMarketplace: Spinner
    private lateinit var spFiltroTipoMarketplace: Spinner
    private lateinit var btnLimpiarFiltroMarketplace: Button
    private lateinit var contenedorMarketplace: LinearLayout

    private val listaPosts = mutableListOf<MutableMap<String, String>>()

    private val categorias = listOf(
        "Todas las categorías",
        "Ropa",
        "Accesorios",
        "Tecnología",
        "Libros",
        "Apuntes",
        "Asesorías",
        "Tutorías",
        "Servicios",
        "Eventos",
        "Otros"
    )

    private val tipos = listOf(
        "Todos los tipos",
        "Producto nuevo",
        "Producto usado",
        "Servicio",
        "Emprendimiento estable",
        "Emprendimiento itinerante",
        "Producto único",
        "Solicitud / ayuda",
        "A coordinar"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marketplace)

        etBuscarMarketplace = findViewById(R.id.etBuscarMarketplace)
        spFiltroCategoriaMarketplace = findViewById(R.id.spFiltroCategoriaMarketplace)
        spFiltroTipoMarketplace = findViewById(R.id.spFiltroTipoMarketplace)
        btnLimpiarFiltroMarketplace = findViewById(R.id.btnLimpiarFiltroMarketplace)
        contenedorMarketplace = findViewById(R.id.contenedorMarketplace)

        configurarFiltros()
        cargarPublicacionesDeOtrosUsuarios()
    }

    private fun configurarFiltros() {
        val categoriaAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            categorias
        )
        categoriaAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spFiltroCategoriaMarketplace.adapter = categoriaAdapter

        val tipoAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            tipos
        )
        tipoAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spFiltroTipoMarketplace.adapter = tipoAdapter

        etBuscarMarketplace.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                mostrarPostsFiltrados()
            }

            override fun afterTextChanged(editable: Editable?) {}
        })

        val listenerFiltros = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mostrarPostsFiltrados()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spFiltroCategoriaMarketplace.onItemSelectedListener = listenerFiltros
        spFiltroTipoMarketplace.onItemSelectedListener = listenerFiltros

        btnLimpiarFiltroMarketplace.setOnClickListener {
            etBuscarMarketplace.setText("")
            spFiltroCategoriaMarketplace.setSelection(0)
            spFiltroTipoMarketplace.setSelection(0)
            mostrarPostsFiltrados()
        }
    }

    private fun cargarPublicacionesDeOtrosUsuarios() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId == null) {
            Toast.makeText(this, "No hay usuario activo", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db.child("publicaciones")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaPosts.clear()

                    for (postSnapshot in snapshot.children) {
                        val postUserId = postSnapshot.child("userId").getValue(String::class.java) ?: ""

                        if (postUserId.isBlank()) {
                            continue
                        }

                        if (postUserId == currentUserId) {
                            continue
                        }

                        val postId = postSnapshot.key
                            ?: postSnapshot.child("id").getValue(String::class.java)
                            ?: ""

                        val post = mutableMapOf<String, String>()

                        post["id"] = postId
                        post["userId"] = postUserId
                        post["titulo"] = postSnapshot.child("titulo").getValue(String::class.java) ?: ""
                        post["descripcion"] = postSnapshot.child("descripcion").getValue(String::class.java) ?: ""
                        post["categoria"] = postSnapshot.child("categoria").getValue(String::class.java) ?: ""
                        post["tipo"] = postSnapshot.child("tipo").getValue(String::class.java) ?: ""
                        post["precio"] = postSnapshot.child("precio").getValue(String::class.java) ?: ""
                        post["whatsapp"] = postSnapshot.child("whatsapp").getValue(String::class.java) ?: ""
                        post["imagenUrl"] = postSnapshot.child("imagenUrl").getValue(String::class.java) ?: ""
                        post["vendido"] = postSnapshot.child("vendido").getValue(String::class.java) ?: "false"
                        val latitud = postSnapshot.child("latitud").getValue(Double::class.java)
                        val longitud = postSnapshot.child("longitud").getValue(Double::class.java)
                        if (latitud != null && longitud != null) {
                            post["latitud"] = latitud.toString()
                            post["longitud"] = longitud.toString()
                        }

                        listaPosts.add(post)
                    }

                    mostrarPostsFiltrados()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MarketplaceActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun mostrarPostsFiltrados() {
        contenedorMarketplace.removeAllViews()

        val textoBuscar = etBuscarMarketplace.text.toString().trim().lowercase()
        val filtroCategoria =
            spFiltroCategoriaMarketplace.selectedItem?.toString() ?: "Todas las categorías"
        val filtroTipo =
            spFiltroTipoMarketplace.selectedItem?.toString() ?: "Todos los tipos"

        val filtrados = listaPosts.filter { post ->
            val titulo = post["titulo"].orEmpty()
            val descripcion = post["descripcion"].orEmpty()
            val categoria = post["categoria"].orEmpty()
            val tipo = post["tipo"].orEmpty()
            val precio = post["precio"].orEmpty()

            val cumpleBusqueda =
                textoBuscar.isBlank() ||
                        titulo.lowercase().contains(textoBuscar) ||
                        descripcion.lowercase().contains(textoBuscar) ||
                        categoria.lowercase().contains(textoBuscar) ||
                        tipo.lowercase().contains(textoBuscar) ||
                        precio.lowercase().contains(textoBuscar)

            val cumpleCategoria =
                filtroCategoria == "Todas las categorías" || categoria == filtroCategoria

            val cumpleTipo =
                filtroTipo == "Todos los tipos" || tipo == filtroTipo

            cumpleBusqueda && cumpleCategoria && cumpleTipo
        }

        if (listaPosts.isEmpty()) {
            mostrarMensajeVacio("No hay publicaciones de otros usuarios disponibles.")
            return
        }

        if (filtrados.isEmpty()) {
            mostrarMensajeVacio("No se encontraron publicaciones con esos filtros.")
            return
        }

        for (post in filtrados) {
            agregarCardPost(post)
        }
    }

    private fun mostrarMensajeVacio(mensaje: String) {
        val tv = TextView(this)
        tv.text = mensaje
        tv.setTextColor(Color.WHITE)
        tv.textSize = 16f
        tv.gravity = android.view.Gravity.CENTER
        tv.setPadding(dp(16), dp(40), dp(16), dp(16))

        contenedorMarketplace.addView(tv)
    }

    private fun agregarCardPost(post: MutableMap<String, String>) {
        val postUserId = post["userId"].orEmpty()

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(16), dp(16), dp(16), dp(16))
        card.background = crearFondoCard()

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dp(16))
        card.layoutParams = params

        val imagenUrl = post["imagenUrl"].orEmpty()

        val imageView = ImageView(this)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(180)
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        imageView.background = crearFondoImagen()

        if (imagenUrl.isNotBlank()) {
            cargarImagen(imagenUrl, imageView)
        }

        val tvTitulo = TextView(this)
        tvTitulo.text = post["titulo"].orEmpty().ifBlank { "Publicación sin título" }
        tvTitulo.setTextColor(Color.WHITE)
        tvTitulo.textSize = 20f
        tvTitulo.setTypeface(null, Typeface.BOLD)
        tvTitulo.setPadding(0, dp(14), 0, dp(6))

        val esVendido = post["vendido"] == "true"
        
        val tvVendido = TextView(this)
        tvVendido.text = "✅ Producto vendido"
        tvVendido.setTextColor(Color.rgb(0, 200, 83))
        tvVendido.textSize = 16f
        tvVendido.setTypeface(null, Typeface.BOLD)
        tvVendido.setPadding(0, 0, 0, dp(10))
        tvVendido.visibility = if (esVendido) View.VISIBLE else View.GONE

        val tvInfo = TextView(this)
        val tieneUbicacion = post.containsKey("latitud") && post.containsKey("longitud")
        val infoTexto = buildString {
            append("Categoría: ${post["categoria"].orEmpty().ifBlank { "Sin categoría" }}\n")
            append("Tipo: ${post["tipo"].orEmpty().ifBlank { "Sin tipo" }}\n")
            append("Precio: S/ ${post["precio"].orEmpty().ifBlank { "0.00" }}")
            if (tieneUbicacion) {
                append("\n📍 Ubicación disponible")
            }
        }
        tvInfo.text = infoTexto
        tvInfo.setTextColor(Color.rgb(180, 190, 220))
        tvInfo.textSize = 14f

        val btnContainer = LinearLayout(this)
        btnContainer.orientation = LinearLayout.HORIZONTAL
        btnContainer.setPadding(0, dp(14), 0, 0)

        val btnChat = Button(this)
        btnChat.text = "Chat"
        btnChat.setTextColor(Color.WHITE)
        btnChat.background = if (esVendido) {
            crearFondoBoton(Color.rgb(150, 150, 150))
        } else {
            crearFondoBoton(Color.rgb(91, 61, 245))
        }
        btnChat.isEnabled = !esVendido
        btnChat.layoutParams = LinearLayout.LayoutParams(
            0,
            dp(48),
            1f
        )

        val espacio1 = Space(this)
        espacio1.layoutParams = LinearLayout.LayoutParams(dp(10), 1)
        
        val btnWhatsApp = Button(this)
        btnWhatsApp.text = "WhatsApp"
        btnWhatsApp.setTextColor(Color.WHITE)
        btnWhatsApp.background = if (esVendido || post["whatsapp"].isNullOrBlank()) {
            crearFondoBoton(Color.rgb(150, 150, 150))
        } else {
            crearFondoBoton(Color.rgb(37, 211, 102))
        }
        btnWhatsApp.isEnabled = !esVendido && !post["whatsapp"].isNullOrBlank()
        btnWhatsApp.layoutParams = LinearLayout.LayoutParams(
            0,
            dp(48),
            1f
        )

        val espacio2 = Space(this)
        espacio2.layoutParams = LinearLayout.LayoutParams(dp(10), 1)

        val btnDetalle = Button(this)
        btnDetalle.text = "Ver detalle"
        btnDetalle.setTextColor(Color.WHITE)
        btnDetalle.background = if (esVendido) {
            crearFondoBoton(Color.rgb(150, 150, 150))
        } else {
            crearFondoBoton(Color.rgb(0, 200, 83))
        }
        btnDetalle.isEnabled = !esVendido
        btnDetalle.layoutParams = LinearLayout.LayoutParams(
            0,
            dp(48),
            1f
        )

        btnChat.setOnClickListener {
            if (!esVendido) {
                db.child("usuarios").child(postUserId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val nombres = snapshot.child("nombres").getValue(String::class.java) ?: "Usuario"
                            val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                            val nombreCompleto = "$nombres $apellidos".trim().ifBlank { "Usuario" }
                            abrirChatEnApp(postUserId, post, nombreCompleto)
                        }
                        
                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@MarketplaceActivity, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
        
        btnWhatsApp.setOnClickListener {
            if (!esVendido && !post["whatsapp"].isNullOrBlank()) {
                abrirWhatsApp(post["whatsapp"]!!, post)
            }
        }

        btnDetalle.setOnClickListener {
            if (!esVendido) {
                mostrarDetallePost(post)
            }
        }

        btnContainer.addView(btnChat)
        btnContainer.addView(espacio1)
        btnContainer.addView(btnWhatsApp)
        btnContainer.addView(espacio2)
        btnContainer.addView(btnDetalle)

        card.addView(imageView)
        card.addView(tvTitulo)
        card.addView(tvVendido)
        card.addView(tvInfo)
        card.addView(btnContainer)

        contenedorMarketplace.addView(card)
    }

    private fun mostrarOpcionesContacto(userId: String, post: MutableMap<String, String>) {
        if (userId.isBlank()) {
            Toast.makeText(this, "No se encontró el usuario de la publicación", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("usuarios").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = snapshot.child("nombres").getValue(String::class.java) ?: "Usuario"
                    val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                    val nombreCompleto = "$nombres $apellidos".trim().ifBlank { "Usuario" }
                    val whatsapp = post["whatsapp"].orEmpty()

                    val opciones = mutableListOf<String>()
                    opciones.add("Chat en la app")
                    if (whatsapp.isNotBlank()) {
                        opciones.add("WhatsApp")
                    }

                    AlertDialog.Builder(this@MarketplaceActivity)
                        .setTitle("¿Cómo quieres contactar?")
                        .setItems(opciones.toTypedArray()) { _, which ->
                            when (opciones[which]) {
                                "Chat en la app" -> abrirChatEnApp(userId, post, nombreCompleto)
                                "WhatsApp" -> abrirWhatsApp(whatsapp, post)
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MarketplaceActivity,
                        "Error al cargar datos del usuario",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
    
    private fun abrirChatEnApp(userId: String, post: MutableMap<String, String>, nombreCompleto: String) {
        val intent = Intent(this@MarketplaceActivity, ChatActivity::class.java)
        intent.putExtra("RECEIVER_ID", userId)
        intent.putExtra("RECEIVER_NAME", nombreCompleto)
        intent.putExtra("PRODUCT_ID", post["id"])
        intent.putExtra("PRODUCT_TITLE", post["titulo"])
        intent.putExtra("PRODUCT_PRICE", post["precio"])
        intent.putExtra("PRODUCT_IMAGE_URL", post["imagenUrl"])
        startActivity(intent)
    }
    
    private fun abrirWhatsApp(whatsapp: String, post: MutableMap<String, String>) {
        if (whatsapp.isBlank()) {
            Toast.makeText(this, "El usuario no ha registrado un número de WhatsApp", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Limpiar el número (quitar espacios, guiones, etc.)
        val numeroLimpio = whatsapp.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
        
        // Crear el mensaje automático para WhatsApp
        val mensaje = "¡Hola! Estoy interesado/a en: ${post["titulo"] ?: "producto"}\nPrecio: S/ ${post["precio"] ?: "0.00"}"
        
        // Codificar el mensaje para URL
        val mensajeCodificado = mensaje.replace(" ", "%20").replace("\n", "%0A")
        
        try {
            // Crear la URI para WhatsApp
            val uri = Uri.parse("https://wa.me/$numeroLimpio?text=$mensajeCodificado")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirUbicacionEnMaps(latitud: String, longitud: String) {
        try {
            val uri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud(Ubicación)")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val uriWeb = Uri.parse("https://www.google.com/maps?q=$latitud,$longitud")
                val intentWeb = Intent(Intent.ACTION_VIEW, uriWeb)
                startActivity(intentWeb)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir Google Maps", Toast.LENGTH_SHORT).show()
        }
    }}

    private fun mostrarDetallePost(post: MutableMap<String, String>) {
        val postUserId = post["userId"].orEmpty()

        db.child("usuarios").child(postUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val nombres = snapshot.child("nombres").getValue(String::class.java) ?: "Usuario"
                    val apellidos = snapshot.child("apellidos").getValue(String::class.java) ?: ""
                    val nombreCompleto = "$nombres $apellidos".trim().ifBlank { "Usuario no registrado" }

                    val tieneUbicacion = post.containsKey("latitud") && post.containsKey("longitud")

                    val detalle = buildString {
                        append("Nombre: ${post["titulo"].orEmpty().ifBlank { "Sin título" }}\n\n")
                        append("Descripción:\n${post["descripcion"].orEmpty().ifBlank { "Sin descripción" }}\n\n")
                        append("Categoría: ${post["categoria"].orEmpty().ifBlank { "Sin categoría" }}\n\n")
                        append("Tipo: ${post["tipo"].orEmpty().ifBlank { "Sin tipo" }}\n\n")
                        append("Precio: S/ ${post["precio"].orEmpty().ifBlank { "0.00" }}\n\n")
                        if (tieneUbicacion) {
                            append("Ubicación: ${post["latitud"]}, ${post["longitud"]}\n\n")
                        }
                        append("WhatsApp: ${post["whatsapp"].orEmpty().ifBlank { "No registrado" }}\n\n")
                        append("Publicado por: $nombreCompleto")
                    }

                    val builder = AlertDialog.Builder(this@MarketplaceActivity)
                        .setTitle("Detalle de publicación")
                        .setMessage(detalle)
                        .setPositiveButton("Contactar") { _, _ ->
                            mostrarOpcionesContacto(postUserId, post)
                        }
                        .setNegativeButton("Cerrar", null)

                    if (tieneUbicacion) {
                        builder.setNeutralButton("Ver ubicación") { _, _ ->
                            abrirUbicacionEnMaps(post["latitud"]!!, post["longitud"]!!)
                        }
                    }

                    builder.show()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MarketplaceActivity,
                        "Error al cargar detalle del usuario",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun cargarImagen(imagenUrl: String, imageView: ImageView) {
        thread {
            try {
                val url = URL(imagenUrl)
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val bitmap = BitmapFactory.decodeStream(connection.getInputStream())

                imageView.post {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                imageView.post {
                    imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }
    }

    private fun crearFondoCard(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(20).toFloat()
            setColor(Color.rgb(22, 28, 48))
            setStroke(dp(1), Color.rgb(65, 75, 110))
        }
    }

    private fun crearFondoImagen(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(Color.rgb(38, 47, 78))
        }
    }

    private fun crearFondoBoton(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14).toFloat()
            setColor(color)
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}