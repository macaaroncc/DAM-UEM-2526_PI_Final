package com.example.pi2dam

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val etNew = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etRepeat = findViewById<TextInputEditText>(R.id.etRepeatNewPassword)

        findViewById<MaterialButton>(R.id.btnResetPassword).setOnClickListener {
            val p1 = etNew.text?.toString().orEmpty()
            val p2 = etRepeat.text?.toString().orEmpty()

            if (p1.isBlank() || p2.isBlank()) {
                Toast.makeText(this, "Completa ambas contraseñas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (p1 != p2) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ok = UserStore.resetPassword(this, p1)
            if (!ok) {
                Toast.makeText(this, "No hay usuario registrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Contraseña restablecida", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
