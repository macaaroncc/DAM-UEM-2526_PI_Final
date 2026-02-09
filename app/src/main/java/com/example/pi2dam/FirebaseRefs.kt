package com.example.pi2dam

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseRefs {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    const val COL_USERS = "users"
    const val COL_PRODUCTS = "products"
    const val COL_ORDERS = "orders"
}
