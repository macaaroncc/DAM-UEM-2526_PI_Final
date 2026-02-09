package com.example.pi2dam.model

data class OrderItem(
    val productId: String = "",
    val qty: Long = 0,
    val priceSnapshot: Double = 0.0
)

object OrderStatus {
    const val CREATED = "CREATED"
    const val CANCELLED = "CANCELLED"
}
