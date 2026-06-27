package com.crow.tradewolf.ui

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

class MyPostsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private lateinit var etBuscar: EditText
    private lateinit var spFiltroCategoria: Spinner
    private lateinit var spFiltroTipo: Spinner
    private lateinit var btnAplicarFiltro: Button
    private lateinit var contenedorPosts: LinearLayout

    private val listaPosts = mutableListOf<MutableMap<String, String>>()

    private val categorias = listOf(
        "Todas las categorías",
        "Ropa",
        "Comida",
        "Bebidas",
        "Accesorios",
        "Tecnología",
        "Maquillaje",
        "Juguetes",
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
        "Comida por pedido",
        "Emprendimiento estable",
        "Emprendimiento itinerante",
        "Producto único",
        "Solicitud / ayuda",
        "A coordinar"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        etBuscar = findViewById(R.id.etBuscar)
        spFiltroCategoria = findViewById(R.id.spFiltroCategoria)
        spFiltroTipo = findViewById(R.id.spFiltroTipo)
        btnAplicarFiltro = findViewById(R.id.btnAplicarFiltro)
        contenedorPosts = findViewById(R.id.contenedorPosts)

        configurarFiltros()
        cargarMisPublicaciones()
    }

    private fun configurarFiltros() {
        val categoriaAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            categorias
        )
        categoriaAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spFiltroCategoria.adapter = categoriaAdapter

        val tipoAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            tipos
        )
        tipoAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spFiltroTipo.adapter = tipoAdapter

        etBuscar.hint = "Buscar por título, descripción, categoría o precio"

        etBuscar.addTextChangedListener(object : TextWatcher {
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

        spFiltroCategoria.onItemSelectedListener = listenerFiltros
        spFiltroTipo.onItemSelectedListener = listenerFiltros

        btnAplicarFiltro.text = "Limpiar filtros"
        btnAplicarFiltro.setOnClickListener {
            etBuscar.setText("")
            spFiltroCategoria.setSelection(0)
            spFiltroTipo.setSelection(0)
            mostrarPostsFiltrados()
        }
    }

    private fun cargarMisPublicaciones() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
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

                        if (postUserId != userId) {
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

                        listaPosts.add(post)
                    }

                    mostrarPostsFiltrados()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MyPostsActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun mostrarPostsFiltrados() {
        contenedorPosts.removeAllViews()

        val textoBuscar = etBuscar.text.toString().trim().lowercase()
        val filtroCategoria = spFiltroCategoria.selectedItem?.toString() ?: "Todas las categorías"
        val filtroTipo = spFiltroTipo.selectedItem?.toString() ?: "Todos los tipos"

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
            mostrarMensajeVacio("Aún no tienes publicaciones creadas.")
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
        tv.setPadding(16, 40, 16, 16)

        contenedorPosts.addView(tv)
    }

    private fun agregarCardPost(post: MutableMap<String, String>) {
        val id = post["id"].orEmpty()
        val esVendido = post["vendido"] == "true"

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

        val tvVendido = TextView(this)
        tvVendido.text = "✅ Producto vendido"
        tvVendido.setTextColor(Color.rgb(0, 200, 83))
        tvVendido.textSize = 16f
        tvVendido.setTypeface(null, Typeface.BOLD)
        tvVendido.setPadding(0, 0, 0, dp(10))
        tvVendido.visibility = if (esVendido) View.VISIBLE else View.GONE

        val tvDescripcion = TextView(this)
        tvDescripcion.text = post["descripcion"].orEmpty().ifBlank { "Sin descripción" }
        tvDescripcion.setTextColor(Color.rgb(210, 216, 235))
        tvDescripcion.textSize = 14f
        tvDescripcion.setPadding(0, 0, 0, dp(10))

        val tvInfo = TextView(this)
        tvInfo.text = """
            Categoría: ${post["categoria"].orEmpty().ifBlank { "Sin categoría" }}
            Tipo: ${post["tipo"].orEmpty().ifBlank { "Sin tipo" }}
            Precio: S/ ${post["precio"].orEmpty().ifBlank { "0.00" }}
            WhatsApp: ${post["whatsapp"].orEmpty().ifBlank { "No registrado" }}
        """.trimIndent()
        tvInfo.setTextColor(Color.rgb(180, 190, 220))
        tvInfo.textSize = 14f

        val btnContainer = LinearLayout(this)
        btnContainer.orientation = LinearLayout.HORIZONTAL
        btnContainer.setPadding(0, dp(14), 0, 0)

        val btnEditar = Button(this)
        btnEditar.text = "Editar"
        btnEditar.setTextColor(Color.WHITE)
        btnEditar.background = crearFondoBoton(Color.rgb(91, 61, 245))
        btnEditar.layoutParams = LinearLayout.LayoutParams(
            0,
            dp(48),
            1f
        )

        val espacio = Space(this)
        espacio.layoutParams = LinearLayout.LayoutParams(dp(10), 1)

        val btnEliminar = Button(this)
        btnEliminar.text = "Eliminar"
        btnEliminar.setTextColor(Color.WHITE)
        btnEliminar.background = crearFondoBoton(Color.rgb(255, 77, 109))
        btnEliminar.layoutParams = LinearLayout.LayoutParams(
            0,
            dp(48),
            1f
        )

        val btnVendido = Button(this)
        btnVendido.text = if (esVendido) "Desmarcar vendido" else "Vendido"
        btnVendido.setTextColor(Color.WHITE)
        btnVendido.background = crearFondoBoton(Color.rgb(0, 200, 83))
        btnVendido.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(48)
        )
        btnVendido.setPadding(0, 0, 0, 0)

        btnEditar.setOnClickListener {
            mostrarDialogEditar(post)
        }

        btnEliminar.setOnClickListener {
            eliminarPost(id)
        }

        btnVendido.setOnClickListener {
            marcarComoVendido(id, !esVendido)
        }

        btnContainer.addView(btnEditar)
        btnContainer.addView(espacio)
        btnContainer.addView(btnEliminar)

        card.addView(imageView)
        card.addView(tvTitulo)
        card.addView(tvVendido)
        card.addView(tvDescripcion)
        card.addView(tvInfo)
        card.addView(btnContainer)
        card.addView(btnVendido)

        contenedorPosts.addView(card)
    }

    private fun mostrarDialogEditar(post: MutableMap<String, String>) {
        val id = post["id"].orEmpty()

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(dp(24), dp(16), dp(24), dp(8))

        val etTitulo = EditText(this)
        etTitulo.hint = "Título"
        etTitulo.setText(post["titulo"])

        val etDescripcion = EditText(this)
        etDescripcion.hint = "Descripción"
        etDescripcion.setText(post["descripcion"])

        val etPrecio = EditText(this)
        etPrecio.hint = "Precio"
        etPrecio.setText(post["precio"])

        val etWhatsapp = EditText(this)
        etWhatsapp.hint = "WhatsApp"
        etWhatsapp.setText(post["whatsapp"])

        layout.addView(etTitulo)
        layout.addView(etDescripcion)
        layout.addView(etPrecio)
        layout.addView(etWhatsapp)

        AlertDialog.Builder(this)
            .setTitle("Editar publicación")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val cambios = mapOf<String, Any>(
                    "titulo" to etTitulo.text.toString().trim(),
                    "descripcion" to etDescripcion.text.toString().trim(),
                    "precio" to etPrecio.text.toString().trim(),
                    "whatsapp" to etWhatsapp.text.toString().trim()
                )

                db.child("publicaciones")
                    .child(id)
                    .updateChildren(cambios)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Publicación actualizada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPost(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar publicación")
            .setMessage("¿Seguro que deseas eliminar esta publicación?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.child("publicaciones")
                    .child(id)
                    .removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Publicación eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun marcarComoVendido(id: String, vendido: Boolean) {
        db.child("publicaciones")
            .child(id)
            .child("vendido")
            .setValue(if (vendido) "true" else "false")
            .addOnSuccessListener {
                val mensaje = if (vendido) "Producto marcado como vendido" else "Producto desmarcado como vendido"
                Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
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