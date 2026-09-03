package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.BackupAppUi
import com.example.ui.BackupViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BackupViewModel by viewModels()

    // Solicitud dinámica de permisos estándar (Notificaciones y Almacenamiento en versiones anteriores)
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        val storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: true

        if (!notificationsGranted || !storageGranted) {
            Toast.makeText(
                this,
                "Se requieren permisos de Almacenamiento y Notificaciones para el correcto funcionamiento en segundo plano.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Verificar y solicitar permisos al iniciar
        checkAndRequestPermissions()

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BackupAppUi(viewModel = viewModel)
                }
            }
        }
    }

    /**
     * Valida y solicita de forma dinámica los permisos necesarios de almacenamiento
     * y notificaciones según la versión de Android instalada.
     */
    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
        }

        // Permiso especial MANAGE_EXTERNAL_STORAGE requerido para escuchar cambios fuera de los sandboxes en Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Por favor, concede acceso total a archivos para que el FileObserver monitoree las carpetas de WhatsApp y DCIM.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}
