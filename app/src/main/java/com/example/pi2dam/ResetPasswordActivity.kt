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

        val etEmail = findViewById<TextInputEditText>(R.id.etResetEmail)
        val etNew = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etRepeat = findViewById<TextInputEditText>(R.id.etRepeatNewPassword)

        val tvLabel1 = findViewById<View>(R.id.tvLabel1)
        val tvLabel2 = findViewById<View>(R.id.tvLabel2)
        val tilNew = findViewById<View>(R.id.tilNewPassword)
        val tilRepeat = findViewById<View>(R.id.tilRepeatNewPassword)
        val btn = findViewById<MaterialButton>(R.id.btnResetPassword)

        // Si el usuario está logueado: permitimos cambiar contraseña directamente.
        // Si NO está logueado: con Firebase Auth lo correcto es enviar un email de restablecimiento.
        val currentUser = FirebaseRefs.auth.currentUser
        if (currentUser == null) {
            tvLabel1.visibility = View.GONE
            tilNew.visibility = View.GONE
            tvLabel2.visibility = View.GONE
            tilRepeat.visibility = View.GONE
            btn.text = getString(R.string.btn_send_reset_email)
        } else {
            etEmail.setText(currentUser.email.orEmpty())
            btn.text = getString(R.string.btn_change_password)
        }

        btn.setOnClickListener {
            val email = etEmail.text?.toString().orEmpty().trim()
            if (email.isBlank()) {
                Toast.makeText(this, getString(R.string.reset_email_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseRefs.auth.currentUser
            if (user == null) {
                FirebaseRefs.auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, e.message ?: getString(R.string.reset_generic_error), Toast.LENGTH_SHORT).show()
                    }
                return@setOnClickListener
            }

            val p1 = etNew.text?.toString().orEmpty()
            val p2 = etRepeat.text?.toString().orEmpty()

            if (p1.isBlank() || p2.isBlank()) {
                Toast.makeText(this, getString(R.string.reset_passwords_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (p1 != p2) {
                Toast.makeText(this, getString(R.string.reset_passwords_not_match), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            user.updatePassword(p1)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.reset_password_updated), Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: getString(R.string.reset_generic_error), Toast.LENGTH_SHORT).show()
                }
        }
    }
}
