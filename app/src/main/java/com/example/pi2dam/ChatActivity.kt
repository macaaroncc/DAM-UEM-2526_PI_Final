package com.example.pi2dam

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private var listener: ListenerRegistration? = null

    private lateinit var et: TextInputEditText
    private lateinit var btn: MaterialButton

    private val adapter = ChatAdapter { FirebaseRefs.auth.currentUser?.uid }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat)

        findViewById<View>(R.id.main).applySystemBarsPadding()
        AppMenu.bind(this)

        et = findViewById(R.id.etMsg)
        btn = findViewById(R.id.btnSend)

        val rv = findViewById<RecyclerView>(R.id.rvMessages)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rv.adapter = adapter

        btn.setOnClickListener {
            send()
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
                attachListener()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    override fun onStop() {
        listener?.remove()
        listener = null
        super.onStop()
    }

    private fun attachListener() {
        listener?.remove()

        val ref = FirebaseRefs.db
            .collection(FirebaseRefs.COL_CHATS)
            .document(FirebaseRefs.CHAT_STOCK)
            .collection(FirebaseRefs.SUB_MESSAGES)

        listener = ref
            .orderBy("createdAt")
            .limitToLast(150)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener

                val rows = snap.documents.map { d ->
                    ChatAdapter.Msg(
                        id = d.id,
                        senderUid = d.getString("senderUid") ?: "",
                        senderName = d.getString("senderName") ?: "",
                        senderRole = d.getString("senderRole") ?: "",
                        text = d.getString("text") ?: "",
                        createdAt = d.getTimestamp("createdAt")?.toDate() ?: Date(0)
                    )
                }

                adapter.submit(rows)
                findViewById<RecyclerView>(R.id.rvMessages).scrollToPosition(maxOf(0, rows.size - 1))
            }
    }

    private fun send() {
        val txt = et.text?.toString()?.trim().orEmpty()
        if (txt.isBlank()) return

        val current = FirebaseRefs.auth.currentUser ?: return
        val me = Session.employee

        btn.isEnabled = false

        val data = hashMapOf(
            "senderUid" to current.uid,
            "senderName" to (me?.name ?: current.email.orEmpty()),
            "senderRole" to (me?.role ?: ""),
            "text" to txt,
            "createdAt" to FieldValue.serverTimestamp()
        )

        FirebaseRefs.db
            .collection(FirebaseRefs.COL_CHATS)
            .document(FirebaseRefs.CHAT_STOCK)
            .collection(FirebaseRefs.SUB_MESSAGES)
            .add(data)
            .addOnSuccessListener {
                et.setText("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "No se pudo enviar", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                btn.isEnabled = true
            }
    }
}
