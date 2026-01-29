package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProductActivity : AppCompatActivity() {
    private var qty = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val productName = intent.getStringExtra(EXTRA_PRODUCT_NAME)
        if (!productName.isNullOrBlank()) {
            findViewById<TextView>(R.id.tvProductCardTitle).text = productName
            findViewById<TextInputEditText>(R.id.etProductName).setText(productName)
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnAppbarHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnAppbarMenu).setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }

        val tvQty = findViewById<TextView>(R.id.tvQty)
        fun renderQty() { tvQty.text = qty.toString() }
        renderQty()

        findViewById<MaterialButton>(R.id.btnMinus).setOnClickListener {
            if (qty > 0) qty--
            renderQty()
        }

        findViewById<MaterialButton>(R.id.btnPlus).setOnClickListener {
            qty++
            renderQty()
        }

        findViewById<MaterialButton>(R.id.btnSavePart).setOnClickListener {
            Toast.makeText(this, "Pieza guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_PRODUCT_NAME = "extra_product_name"
    }
}
