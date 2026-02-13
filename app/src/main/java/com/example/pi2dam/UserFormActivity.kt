package com.example.pi2dam

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_WORKER
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class UserFormActivity : AppCompatActivity() {

    private var editingUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_form)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        editingUid = intent.getStringExtra(EXTRA_UID)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val spRole = findViewById<Spinner>(R.id.spRole)
        val swActive = findViewById<SwitchMaterial>(R.id.swActive)

        AppMenu.bind(this)

        spRole.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(ROLE_ADMIN, ROLE_WORKER)
        )

        val isEdit = !editingUid.isNullOrBlank()
        tilPassword.visibility = if (isEdit) View.GONE else View.VISIBLE
        tilEmail.isEnabled = !isEdit
        swActive.visibility = if (isEdit) View.VISIBLE else View.GONE

        findViewById<MaterialButton>(R.id.btnDelete).visibility = if (isEdit) View.VISIBLE else View.GONE

        if (isEdit) {
            val uid = editingUid!!
            FirebaseRefs.db.collection(FirebaseRefs.COL_USERS).document(uid).get()
                .addOnSuccessListener { d ->
                    etName.setText(d.getString("name") ?: "")
                    etEmail.setText(d.getString("email") ?: "")
                    val role = d.getString("role") ?: ROLE_WORKER
                    spRole.setSelection(if (role == ROLE_ADMIN) 0 else 1)
                    swActive.isChecked = d.getBoolean("active") ?: false
                }
        } else {
            swActive.isChecked = true
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = etName.text?.toString().orEmpty().trim()
            val email = etEmail.text?.toString().orEmpty().trim()
            val role = spRole.selectedItem?.toString() ?: ROLE_WORKER

            if (name.isBlank() || email.isBlank()) {
                Toast.makeText(this, "Completa nombre y email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isEdit) {
                val pass = etPassword.text?.toString().orEmpty()
                if (pass.isBlank()) {
                    Toast.makeText(this, "Completa contraseña", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val secondaryAuth = SecondaryAuthProvider.auth(this)
                secondaryAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { res ->
                        val uid = res.user?.uid
                        if (uid.isNullOrBlank()) {
                            Toast.makeText(this, "Error creando auth", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        PiRepository.createEmployeeProfileWithLimits(uid, email, name, requestedRole = role)
                            .addOnSuccessListener {
                                secondaryAuth.signOut()
                                Toast.makeText(this, "Usuario creado", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Revertimos auth si no se pudo crear el perfil (por límites, etc.)
                                secondaryAuth.currentUser?.delete()?.addOnCompleteListener {
                                    secondaryAuth.signOut()
                                    Toast.makeText(this, e.message ?: "No se pudo crear", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, e.message ?: "No se pudo crear", Toast.LENGTH_SHORT).show()
                    }

                return@setOnClickListener
            }

            val active = swActive.isChecked
            val uid = editingUid!!
            PiRepository.updateEmployeeProfileWithLimits(uid, name, role, active)
                .addOnSuccessListener {
                    Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Error guardando", Toast.LENGTH_SHORT).show()
                }
        }

        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            val uid = editingUid ?: return@setOnClickListener
            PiRepository.deleteEmployeeProfile(uid)
                .addOnSuccessListener {
                    Toast.makeText(this, "Usuario borrado (perfil)", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "No se pudo borrar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onStart() {
        super.onStart()
        val current = FirebaseRefs.auth.currentUser
        if (current == null) {
            finish()
            return
        }

        PiRepository.ensureEmployeeAccess(current.uid)
            .addOnSuccessListener { me ->
                Session.setEmployee(me)
                if (me.role != ROLE_ADMIN) {
                    Toast.makeText(this, "Solo admin", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                finish()
            }
    }

    companion object {
        const val EXTRA_UID = "extra_uid"
    }
}
