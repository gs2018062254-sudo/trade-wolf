package com.crow.tradewolf.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.crow.tradewolf.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class CreatePostActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var spCategoria: Spinner
    private lateinit var spTipo: Spinner
    private lateinit var etPrecio: EditText
    private lateinit var etWhatsapp: EditText
    private lateinit var etImagenUrl: EditText
    private lateinit var ivPreviewImagen: ImageView
    private lateinit var tvImagenHint: TextView
    private lateinit var btnTomarFoto: MaterialButton
    private lateinit var btnCargarImagen: MaterialButton
    private lateinit var btnPublicar: MaterialButton

    private var imagenUri: android.net.Uri? = null
    private var imagenBitmap: Bitmap? = null

    private val galeriaLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imagenUri = uri
            imagenBitmap = null
            ivPreviewImagen.setImageURI(uri)
            tvImagenHint.text = "Imagen seleccionada"
        }
    }

    private val camaraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            imagenBitmap = bitmap
            imagenUri = null
            ivPreviewImagen.setImageBitmap(bitmap)
            tvImagenHint.text = "Foto capturada"
        } else {
            Toast.makeText(this, "No se tomó ninguna foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val categorias = listOf(
        "Selecciona categoría",
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
        "Selecciona tipo",
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
        setContentView(R.layout.activity_create_post)

        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        spCategoria = findViewById(R.id.spCategoria)
        spTipo = findViewById(R.id.spTipo)
        etPrecio = findViewById(R.id.etPrecio)
        etWhatsapp = findViewById(R.id.etWhatsapp)
        etImagenUrl = findViewById(R.id.etImagenUrl)
        ivPreviewImagen = findViewById(R.id.ivPreviewImagen)
        tvImagenHint = findViewById(R.id.tvImagenHint)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnCargarImagen = findViewById(R.id.btnCargarImagen)
        btnPublicar = findViewById(R.id.btnPublicar)

        configurarSpinners()
        configurarBotonesImagen()

        btnPublicar.setOnClickListener {
            validarYPublicar()
        }
    }

    private fun configurarSpinners() {
        val categoriaAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            categorias
        )
        categoriaAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spCategoria.adapter = categoriaAdapter

        val tipoAdapter = ArrayAdapter(
            this,
            R.layout.item_spinner_selected,
            tipos
        )
        tipoAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        spTipo.adapter = tipoAdapter
    }

    private fun configurarBotonesImagen() {
        btnTomarFoto.setOnClickListener {
            camaraLauncher.launch(null)
        }

        btnCargarImagen.setOnClickListener {
            galeriaLauncher.launch("image/*")
        }
    }

    private fun validarYPublicar() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "No hay usuario activo. Inicia sesión otra vez.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val titulo = etTitulo.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val categoria = spCategoria.selectedItem.toString()
        val tipo = spTipo.selectedItem.toString()
        val precio = etPrecio.text.toString().trim()
        val whatsapp = etWhatsapp.text.toString().trim()

        if (titulo.isEmpty() || descripcion.isEmpty() || precio.isEmpty() || whatsapp.isEmpty()) {
            Toast.makeText(
                this,
                "Completa título, descripción, precio y WhatsApp",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (categoria == "Selecciona categoría") {
            Toast.makeText(this, "Selecciona categoría", Toast.LENGTH_SHORT).show()
            return
        }

        if (tipo == "Selecciona tipo") {
            Toast.makeText(this, "Selecciona tipo", Toast.LENGTH_SHORT).show()
            return
        }

        val id = db.child("publicaciones").push().key ?: System.currentTimeMillis().toString()

        btnPublicar.isEnabled = false
        btnPublicar.text = "Publicando..."

        subirImagenSiExiste(
            id = id,
            onSuccess = { imagenUrl ->
                etImagenUrl.setText(imagenUrl)

                guardarPublicacion(
                    id = id,
                    userId = user.uid,
                    userEmail = user.email ?: "",
                    titulo = titulo,
                    descripcion = descripcion,
                    categoria = categoria,
                    tipo = tipo,
                    precio = precio,
                    whatsapp = whatsapp,
                    imagenUrl = imagenUrl
                )
            },
            onError = { mensaje ->
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                btnPublicar.isEnabled = true
                btnPublicar.text = "Publicar"
            }
        )
    }

    private fun subirImagenSiExiste(
        id: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uri = imagenUri
        val bitmap = imagenBitmap

        if (uri == null && bitmap == null) {
            onSuccess("")
            return
        }

        btnPublicar.text = "Subiendo imagen..."

        val currentUserId = auth.currentUser?.uid ?: ""

        if (currentUserId.isBlank()) {
            onError("No hay usuario activo para subir la imagen")
            return
        }

        val imagenRef = storage.child("publicaciones/$currentUserId/$id.jpg")

        if (uri != null) {
            imagenRef.putFile(uri)
                .addOnSuccessListener {
                    imagenRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            onSuccess(downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            onError("Error al obtener URL de imagen: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    onError("Error al subir imagen: ${e.message}")
                }

            return
        }

        if (bitmap != null) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imagenBytes = outputStream.toByteArray()

            imagenRef.putBytes(imagenBytes)
                .addOnSuccessListener {
                    imagenRef.downloadUrl
                        .addOnSuccessListener { downloadUri ->
                            onSuccess(downloadUri.toString())
                        }
                        .addOnFailureListener { e ->
                            onError("Error al obtener URL de imagen: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    onError("Error al subir foto: ${e.message}")
                }
        }
    }

    private fun guardarPublicacion(
        id: String,
        userId: String,
        userEmail: String,
        titulo: String,
        descripcion: String,
        categoria: String,
        tipo: String,
        precio: String,
        whatsapp: String,
        imagenUrl: String
    ) {
        btnPublicar.text = "Guardando..."

        val data = hashMapOf(
            "id" to id,
            "userId" to userId,
            "userEmail" to userEmail,
            "titulo" to titulo,
            "descripcion" to descripcion,
            "categoria" to categoria,
            "tipo" to tipo,
            "precio" to precio,
            "whatsapp" to whatsapp,
            "imagenUrl" to imagenUrl,
            "createdAt" to System.currentTimeMillis()
        )

        db.child("publicaciones")
            .child(id)
            .setValue(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Publicación guardada", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "ERROR: ${e.message}", Toast.LENGTH_LONG).show()
                btnPublicar.isEnabled = true
                btnPublicar.text = "Publicar"
            }
    }
}