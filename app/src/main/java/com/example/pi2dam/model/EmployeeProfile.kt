package com.example.pi2dam.model

data class EmployeeProfile(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = ROLE_WORKER,
    val active: Boolean = true
) {
    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_WORKER = "worker"
    }
}
