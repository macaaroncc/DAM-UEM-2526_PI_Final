package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnAppbarHelp).setOnClickListener {
            Toast.makeText(this, "Ya estás en Ayuda", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.btnAppbarMenu).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        findViewById<TextInputLayout>(R.id.tilSearchHelp).setEndIconOnClickListener {
            Toast.makeText(this, "Buscar ayuda (demo)", Toast.LENGTH_SHORT).show()
        }
    }
}
