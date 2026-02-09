package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class PaymentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_payment)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnAppbarHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnAppbarMenu).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnAddCard).setOnClickListener {
            Toast.makeText(this, "Tarjeta añadida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
