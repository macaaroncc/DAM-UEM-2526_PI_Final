package com.example.pi2dam

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

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

            val user = FirebaseRefs.auth.currentUser
            if (user == null) {
                Toast.makeText(this, "Inicia sesión para cambiar la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            user.updatePassword(p1)
                .addOnSuccessListener {
                    Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "No se pudo actualizar", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
