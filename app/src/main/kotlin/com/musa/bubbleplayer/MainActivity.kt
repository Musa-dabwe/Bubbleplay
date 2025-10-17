package com.musa.bubbleplayer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var permissionText: TextView
    private lateinit var permissionButton: Button

    private val overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (checkOverlayPermission()) {
            startBubbleService()
        } else {
            Toast.makeText(this, "Permission denied. The floating player cannot be displayed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        permissionText = findViewById(R.id.permission_text)
        permissionButton = findViewById(R.id.permission_button)

        permissionButton.setOnClickListener {
            requestOverlayPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        if (checkOverlayPermission()) {
            permissionText.text = "Permission granted! Starting the player."
            permissionButton.visibility = View.GONE
            startBubbleService()
        } else {
            permissionText.text = "This app requires permission to draw over other apps to display the floating player."
            permissionButton.visibility = View.VISIBLE
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true // Overlay permission is not required before Marshmallow
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startBubbleService()
        }
    }

    private fun startBubbleService() {
        if (checkOverlayPermission()) {
            startService(Intent(this, BubbleService::class.java))
            finish() // Close main activity after starting the service
        }
    }
}