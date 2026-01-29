package com.example.pi2dam

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val etUser = findViewById<TextInputEditText>(R.id.etUser)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etRepeatPassword = findViewById<TextInputEditText>(R.id.etRepeatPassword)

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            val user = etUser.text?.toString().orEmpty().trim()
            val email = etEmail.text?.toString().orEmpty().trim()
            val pass = etPassword.text?.toString().orEmpty()
            val pass2 = etRepeatPassword.text?.toString().orEmpty()

            if (user.isBlank() || email.isBlank() || pass.isBlank() || pass2.isBlank()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != pass2) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            UserStore.register(this, user, email, pass)
            Toast.makeText(this, "Usuario registrado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
