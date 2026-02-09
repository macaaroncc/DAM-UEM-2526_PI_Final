package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.EmployeeProfile
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.google.android.material.button.MaterialButton

class UsersActivity : AppCompatActivity() {

    private var usersListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val adapter = UsersAdapter(
        onClick = { user ->
            startActivity(Intent(this, UserFormActivity::class.java)
                .putExtra(UserFormActivity.EXTRA_UID, user.uid))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_users)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnCreateUser).setOnClickListener {
            startActivity(Intent(this, UserFormActivity::class.java))
        }

        findViewById<RecyclerView>(R.id.rvUsers).apply {
            layoutManager = LinearLayoutManager(this@UsersActivity)
            adapter = this@UsersActivity.adapter
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
                    return@addOnSuccessListener
                }

                usersListener?.remove()
                usersListener = FirebaseRefs.db.collection(FirebaseRefs.COL_USERS)
                    .addSnapshotListener { snap, e ->
                        if (e != null || snap == null) return@addSnapshotListener

                        val items = snap.documents.map { d ->
                            EmployeeProfile(
                                uid = d.id,
                                email = d.getString("email") ?: "",
                                name = d.getString("name") ?: "",
                                role = d.getString("role") ?: "",
                                active = d.getBoolean("active") ?: false
                            )
                        }.sortedWith(compareBy<EmployeeProfile>({ it.role }, { it.name }))

                        adapter.submit(items)
                    }
            }
            .addOnFailureListener {
                finish()
            }
    }

    override fun onStop() {
        usersListener?.remove()
        usersListener = null
        super.onStop()
    }
}
