package com.crow.tradewolf.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.crow.tradewolf.R
import com.crow.tradewolf.data.repository.AuthRepository
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    // Lanzador para pedir permiso de notificaciones
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    // El usuario negó el permiso permanentemente
                    showEnableNotificationsDialog()
                } else {
                    Toast.makeText(
                        this,
                        "Necesitas habilitar notificaciones para recibir mensajes",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnEditProfile = findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnCreatePost = findViewById<MaterialButton>(R.id.btnCreatePost)
        val btnListPosts = findViewById<MaterialButton>(R.id.btnListPosts)
        val btnMarketplace = findViewById<MaterialButton>(R.id.btnMarketplace)
        val btnChats = findViewById<MaterialButton>(R.id.btnChats)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        tvWelcome.text = "Bienvenido a TradeWolf"

        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnCreatePost.setOnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        btnListPosts.setOnClickListener {
            val intent = Intent(this, MyPostsActivity::class.java)
            startActivity(intent)
        }

        btnMarketplace.setOnClickListener {
            val intent = Intent(this, MarketplaceActivity::class.java)
            startActivity(intent)
        }

        btnChats.setOnClickListener {
            val intent = Intent(this, ChatsListActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            authRepository.logout()

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Verificar y pedir permiso de notificaciones
        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    // Permiso otorgado
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Mostrar explicación al usuario
                    showPermissionRationaleDialog()
                }
                else -> {
                    // Pedir permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Notificaciones necesarias")
            .setMessage("Necesitamos habilitar las notificaciones para que puedas recibir mensajes en tiempo real.")
            .setPositiveButton("Continuar") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEnableNotificationsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Habilitar notificaciones")
            .setMessage("Las notificaciones están desactivadas. Por favor, habilítalas en la configuración de la app para recibir mensajes.")
            .setPositiveButton("Ir a configuración") { _, _ ->
                // Abrir configuración de la app
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}