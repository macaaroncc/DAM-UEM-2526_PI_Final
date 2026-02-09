package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        val etUser = findViewById<TextInputEditText>(R.id.etUser)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etRepeatPassword = findViewById<TextInputEditText>(R.id.etRepeatPassword)

        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            val name = etUser.text?.toString().orEmpty().trim()
            val email = etEmail.text?.toString().orEmpty().trim()
            val pass = etPassword.text?.toString().orEmpty()
            val pass2 = etRepeatPassword.text?.toString().orEmpty()

            if (name.isBlank() || email.isBlank() || pass.isBlank() || pass2.isBlank()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != pass2) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FirebaseRefs.auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { res ->
                    val user = res.user
                    val uid = user?.uid
                    if (uid.isNullOrBlank()) {
                        Toast.makeText(this, "Error creando usuario", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    // Auto-rol: primero 2 admins, luego hasta 5 workers.
                    PiRepository.createEmployeeProfileWithLimits(uid, email, name, requestedRole = null)
                        .addOnSuccessListener {
                            startActivity(Intent(this, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // Si no hay plazas, intentamos borrar el auth recién creado.
                            user.delete().addOnCompleteListener {
                                FirebaseRefs.auth.signOut()
                                Toast.makeText(this, e.message ?: "No hay plazas", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "No se pudo registrar", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
