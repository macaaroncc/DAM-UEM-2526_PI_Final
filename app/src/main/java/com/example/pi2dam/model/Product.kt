package com.example.pi2dam.model

data class Product(
    val id: String = "",
    val name: String = "",
    val sku: String = "",
    val location: String = "",
    val supplierId: String = "",
    val stock: Long = 0,
    val price: Double = 0.0,
    val lowStockThreshold: Long = 0
)
