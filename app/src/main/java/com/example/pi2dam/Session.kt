package com.example.pi2dam

import com.example.pi2dam.model.EmployeeProfile

object Session {
    @Volatile
    var employee: EmployeeProfile? = null
        private set

    fun setEmployee(profile: EmployeeProfile) {
        employee = profile
    }

    fun clear() {
        employee = null
    }
}
