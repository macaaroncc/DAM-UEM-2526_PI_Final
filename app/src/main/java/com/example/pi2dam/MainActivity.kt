package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        // Siempre empezar en Login (sin auto-sesión)
        FirebaseRefs.auth.signOut()
        Session.clear()

        AppMenu.bind(this)

        val etUser = findViewById<TextInputEditText>(R.id.etUser)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)

        fun goHome() {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }


        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            val email = etUser.text?.toString().orEmpty().trim()
            val password = etPassword.text?.toString().orEmpty()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Completa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseRefs.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { res ->
                    val uid = res.user?.uid
                    if (uid.isNullOrBlank()) {
                        Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    PiRepository.ensureEmployeeAccess(uid)
                        .addOnSuccessListener {
                            Session.setEmployee(it)
                            goHome()
                        }
                        .addOnFailureListener {
                            FirebaseRefs.auth.signOut()
                            Session.clear()
                            Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show()
                }
        }

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            startActivity(Intent(this, ResetPasswordActivity::class.java))
        }

        findViewById<TextView>(R.id.tvHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }
    }
}
