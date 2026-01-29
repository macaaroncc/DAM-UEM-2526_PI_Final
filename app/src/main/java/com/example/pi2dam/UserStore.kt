package com.example.pi2dam

import android.content.Context

object UserStore {
    private const val PREFS = "bahj_user"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"

    fun hasUser(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return !prefs.getString(KEY_USERNAME, null).isNullOrBlank() ||
            !prefs.getString(KEY_EMAIL, null).isNullOrBlank()
    }

    fun register(context: Context, username: String, email: String, password: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, username.trim())
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun canLogin(context: Context, userOrEmail: String, password: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val u = prefs.getString(KEY_USERNAME, "") ?: ""
        val e = prefs.getString(KEY_EMAIL, "") ?: ""
        val p = prefs.getString(KEY_PASSWORD, "") ?: ""

        val inId = userOrEmail.trim()
        return (inId.equals(u, ignoreCase = true) || inId.equals(e, ignoreCase = true)) && password == p
    }

    fun resetPassword(context: Context, newPassword: String): Boolean {
        if (!hasUser(context)) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PASSWORD, newPassword)
            .apply()
        return true
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
