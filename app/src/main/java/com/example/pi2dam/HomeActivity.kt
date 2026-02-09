package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        findViewById<View>(R.id.btnBack).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        findViewById<View>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
        findViewById<View>(R.id.btnMenu).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        findViewById<TextInputLayout>(R.id.tilSearchHome).setEndIconOnClickListener {
            Toast.makeText(this, "Buscar (demo)", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.btnFilterRecent).setOnClickListener {
            Toast.makeText(this, "Filtro: Reciente", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnFilterLowStock).setOnClickListener {
            Toast.makeText(this, "Filtro: Stock Bajo", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnFilterHighPrice).setOnClickListener {
            Toast.makeText(this, "Filtro: Mayor Precio", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnFilterLowPrice).setOnClickListener {
            Toast.makeText(this, "Filtro: Menor Precio", Toast.LENGTH_SHORT).show()
        }

        fun openProduct(name: String) {
            startActivity(Intent(this, ProductActivity::class.java)
                .putExtra(ProductActivity.EXTRA_PRODUCT_NAME, name))
        }

        findViewById<MaterialCardView>(R.id.item1).setOnClickListener {
            openProduct("Filtro de Aceite Premium")
        }
        findViewById<MaterialCardView>(R.id.item2).setOnClickListener {
            openProduct("Pastillas de Freno Delanteras x4")
        }
        findViewById<MaterialCardView>(R.id.item3).setOnClickListener {
            openProduct("Filtro de Aceite Premium")
        }
    }
}
