package com.example.pi2dam

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

object SecondaryAuthProvider {
    private const val APP_NAME = "pi2dam_user_creator"

    fun auth(context: Context): FirebaseAuth {
        val defaultApp = FirebaseApp.getInstance()
        val app = try {
            FirebaseApp.getInstance(APP_NAME)
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(context.applicationContext, defaultApp.options, APP_NAME)
        }
        return FirebaseAuth.getInstance(app)
    }
}
