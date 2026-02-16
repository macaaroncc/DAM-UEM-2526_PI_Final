package com.example.pi2dam

import com.example.pi2dam.model.EmployeeProfile
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_WORKER
import com.example.pi2dam.model.OrderItem
import com.example.pi2dam.model.OrderStatus
import com.example.pi2dam.model.Product
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot

object PiRepository {

    private const val MAX_ADMINS = 2
    private const val MAX_WORKERS = 5

    private fun getActiveRoleCounts(): Task<Pair<Int, Int>> {
        val users = FirebaseRefs.db.collection(FirebaseRefs.COL_USERS)
        val adminsTask = users.whereEqualTo("active", true).whereEqualTo("role", ROLE_ADMIN).get()
        val workersTask = users.whereEqualTo("active", true).whereEqualTo("role", ROLE_WORKER).get()

        return Tasks.whenAllSuccess<QuerySnapshot>(adminsTask, workersTask).continueWith { t ->
            @Suppress("UNCHECKED_CAST")
            val res = t.result as List<QuerySnapshot>
            val adminCount = res.getOrNull(0)?.size() ?: 0
            val workerCount = res.getOrNull(1)?.size() ?: 0
            adminCount to workerCount
        }
    }

    fun ensureEmployeeAccess(uid: String): Task<EmployeeProfile> {
        val doc = FirebaseRefs.db.collection(FirebaseRefs.COL_USERS).document(uid)
        return doc.get().continueWith { t ->
            val snap = t.result
            val active = snap?.getBoolean("active") ?: false
            val role = snap?.getString("role") ?: ""
            val email = snap?.getString("email") ?: ""
            val name = snap?.getString("name") ?: ""

            if (snap == null || !snap.exists() || !active) {
                throw FirebaseFirestoreException(
                    "Empleado no autorizado",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                )
            }
            if (role != ROLE_ADMIN && role != ROLE_WORKER) {
                throw FirebaseFirestoreException(
                    "Rol inválido",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                )
            }

            EmployeeProfile(uid = uid, email = email, name = name, role = role, active = active)
        }
    }

    /**
     * Crea un perfil en Firestore aplicando límites:
     * - máximo 2 admins activos
     * - máximo 5 workers activos
     * Si [requestedRole] es null, asigna admin si aún hay hueco; si no, worker.
     */
    fun createEmployeeProfileWithLimits(
        uid: String,
        email: String,
        name: String,
        requestedRole: String? = null
    ): Task<EmployeeProfile> {
        val doc = FirebaseRefs.db.collection(FirebaseRefs.COL_USERS).document(uid)

        return getActiveRoleCounts().continueWithTask { t ->
            val (adminCount, workerCount) = t.result

            val role = requestedRole ?: if (adminCount < MAX_ADMINS) ROLE_ADMIN else ROLE_WORKER

            if (role == ROLE_ADMIN && adminCount >= MAX_ADMINS) {
                return@continueWithTask Tasks.forException(
                    FirebaseFirestoreException(
                        "Máximo de admins alcanzado",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    )
                )
            }
            if (role == ROLE_WORKER && workerCount >= MAX_WORKERS) {
                return@continueWithTask Tasks.forException(
                    FirebaseFirestoreException(
                        "Máximo de trabajadores alcanzado",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    )
                )
            }

            val data = hashMapOf(
                "uid" to uid,
                "email" to email.trim(),
                "name" to name.trim(),
                "role" to role,
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            doc.set(data).continueWith {
                EmployeeProfile(uid = uid, email = email.trim(), name = name.trim(), role = role, active = true)
            }
        }
    }

    /** Mantengo compatibilidad con el registro antiguo (siempre worker). */
    fun createWorkerProfile(uid: String, email: String, name: String): Task<Void> {
        return createEmployeeProfileWithLimits(uid, email, name, ROLE_WORKER).continueWith { null }
    }

    fun updateEmployeeProfileWithLimits(
        uid: String,
        name: String,
        role: String,
        active: Boolean
    ): Task<Void> {
        val userRef = FirebaseRefs.db.collection(FirebaseRefs.COL_USERS).document(uid)

        return userRef.get().continueWithTask { getTask ->
            val snap = getTask.result
            if (snap == null || !snap.exists()) {
                return@continueWithTask Tasks.forException(
                    FirebaseFirestoreException("Usuario no existe", FirebaseFirestoreException.Code.NOT_FOUND)
                )
            }

            val oldRole = snap.getString("role") ?: ROLE_WORKER
            val oldActive = snap.getBoolean("active") ?: false

            getActiveRoleCounts().continueWithTask { countsTask ->
                var (adminCount, workerCount) = countsTask.result

                // Quitamos al propio usuario de los contadores actuales para poder revalidar.
                if (oldActive) {
                    if (oldRole == ROLE_ADMIN) adminCount--
                    if (oldRole == ROLE_WORKER) workerCount--
                }

                if (active) {
                    if (role == ROLE_ADMIN && adminCount >= MAX_ADMINS) {
                        return@continueWithTask Tasks.forException(
                            FirebaseFirestoreException(
                                "Máximo de admins alcanzado",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION
                            )
                        )
                    }
                    if (role == ROLE_WORKER && workerCount >= MAX_WORKERS) {
                        return@continueWithTask Tasks.forException(
                            FirebaseFirestoreException(
                                "Máximo de trabajadores alcanzado",
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION
                            )
                        )
                    }
                }

                userRef.update(
                    mapOf(
                        "name" to name.trim(),
                        "role" to role,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
    }

    fun deleteEmployeeProfile(uid: String): Task<Void> {
        return FirebaseRefs.db.collection(FirebaseRefs.COL_USERS).document(uid).delete()
    }

    fun upsertProduct(product: Product): Task<Void> {
        val col = FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS)
        val ref = if (product.id.isBlank()) col.document() else col.document(product.id)

        val data = hashMapOf(
            "name" to product.name.trim(),
            "sku" to product.sku.trim(),
            "location" to product.location.trim(),
            "stock" to product.stock,
            "price" to product.price,
            "lowStockThreshold" to product.lowStockThreshold,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return ref.set(data)
    }

    fun deleteProduct(productId: String): Task<Void> {
        return FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS).document(productId).delete()
    }

    fun createOrder(createdByUid: String, items: List<OrderItem>): Task<String> {
        if (items.isEmpty()) return Tasks.forException(IllegalArgumentException("items vacío"))

        val db = FirebaseRefs.db
        val orderRef = db.collection(FirebaseRefs.COL_ORDERS).document()

        return db.runTransaction { tx ->
            // 1) Validar items
            items.forEach { item ->
                if (item.productId.isBlank() || item.qty <= 0) {
                    throw FirebaseFirestoreException("Item inválido", FirebaseFirestoreException.Code.INVALID_ARGUMENT)
                }
            }

            // 2) TODAS las lecturas primero
            val productRefs = items.map { db.collection(FirebaseRefs.COL_PRODUCTS).document(it.productId) }
            val snapshots = productRefs.map { tx.get(it) }

            // 3) Verificar stock y calcular nuevos valores
            val stockUpdates = items.mapIndexed { index, item ->
                val currentStock = snapshots[index].getLong("stock") ?: 0L
                if (currentStock < item.qty) {
                    throw FirebaseFirestoreException(
                        "Stock insuficiente",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    )
                }
                productRefs[index] to (currentStock - item.qty)
            }

            // 4) TODAS las escrituras después
            stockUpdates.forEach { (prodRef, newStock) ->
                tx.update(prodRef, mapOf(
                    "stock" to newStock,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
            }

            // 5) Crear order
            val orderData = hashMapOf(
                "status" to OrderStatus.CREATED,
                "createdByUid" to createdByUid,
                "items" to items.map { mapOf(
                    "productId" to it.productId,
                    "qty" to it.qty,
                    "priceSnapshot" to it.priceSnapshot
                ) },
                "createdAt" to FieldValue.serverTimestamp(),
                "cancelledAt" to null
            )

            tx.set(orderRef, orderData)
            orderRef.id
        }
    }

    fun cancelOrder(orderId: String): Task<Void> {
        val db = FirebaseRefs.db
        val orderRef = db.collection(FirebaseRefs.COL_ORDERS).document(orderId)

        return db.runTransaction { tx ->
            val snap = tx.get(orderRef)
            if (!snap.exists()) {
                throw FirebaseFirestoreException("Pedido no existe", FirebaseFirestoreException.Code.NOT_FOUND)
            }
            val status = snap.getString("status") ?: ""
            if (status == OrderStatus.CANCELLED) return@runTransaction null

            @Suppress("UNCHECKED_CAST")
            val items = (snap.get("items") as? List<Map<String, Any?>>).orEmpty()

            items.forEach { m ->
                val productId = m["productId"] as? String ?: return@forEach
                val qty = (m["qty"] as? Number)?.toLong() ?: 0L
                if (qty <= 0) return@forEach

                val prodRef = db.collection(FirebaseRefs.COL_PRODUCTS).document(productId)
                tx.update(
                    prodRef,
                    mapOf(
                        "stock" to FieldValue.increment(qty),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            tx.update(
                orderRef,
                mapOf(
                    "status" to OrderStatus.CANCELLED,
                    "cancelledAt" to FieldValue.serverTimestamp()
                )
            )

            null
        }
    }
}
