package com.pizzatown.delivery.data

import android.util.Log

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.pizzatown.delivery.domain.model.DeliveryOrder
import com.pizzatown.delivery.domain.model.DeliveryOrderItem
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DeliveryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    fun observeAssignedOrders(
        deliveryBoyId: String
    ): Flow<List<DeliveryOrder>> = callbackFlow {

        val ordersById = mutableMapOf<String, DeliveryOrder>()

        fun emitOrders() {
            trySend(
                ordersById.values
                    .sortedByDescending { it.orderId }
            )
        }

        val readyRegistration = firestore
            .collection("orders")
            .whereEqualTo("status", "READY")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                Log.d(
                    "DeliveryOrders",
                    "READY listener: error=$error docs=${snapshot?.documents?.size ?: 0}"
                )

                snapshot?.documents.orEmpty().forEach { doc ->
                    Log.d(
                        "DeliveryOrders",
                        "READY doc=${doc.id} status=${doc.getString("status")} deliveryBoyId=${doc.getString("deliveryBoyId")}"
                    )

                    doc.toDeliveryOrder()?.let { order ->
                        ordersById[order.orderId] = order
                    }
                }

                Log.d(
                    "DeliveryOrders",
                    "READY merged orders=${ordersById.size}"
                )

                emitOrders()
            }

        val assignedRegistration = firestore
            .collection("orders")
            .whereEqualTo("deliveryBoyId", deliveryBoyId)
            .whereIn(
                "status",
                listOf("OUT_FOR_DELIVERY", "ON_THE_WAY")
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                snapshot?.documents.orEmpty().forEach { doc ->
                    doc.toDeliveryOrder()?.let { order ->
                        ordersById[order.orderId] = order
                    }
                }

                emitOrders()
            }

        awaitClose {
            readyRegistration.remove()
            assignedRegistration.remove()
        }
    }

    fun observeDeliveredOrders(
        deliveryBoyId: String
    ): Flow<List<DeliveryOrder>> = callbackFlow {

        val registration = firestore
            .collection("orders")
            .whereEqualTo("deliveryBoyId", deliveryBoyId)
            .whereEqualTo("status", "DELIVERED")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val result = snapshot?.documents.orEmpty()
                    .mapNotNull { it.toDeliveryOrder() }

                trySend(result)
            }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun markPickedUp(orderId: String) {
        functions
            .getHttpsCallable("markOrderPickedUp")
            .call(mapOf("orderId" to orderId))
            .await()
    }

    suspend fun markDelivered(orderId: String) {
        functions
            .getHttpsCallable("markOrderDelivered")
            .call(mapOf("orderId" to orderId))
            .await()
    }

    private fun DocumentSnapshot.toDeliveryOrder(): DeliveryOrder? {
        val data = data ?: return null

        val customer = data["customer"] as? Map<*, *>

        val items = (data["items"] as? List<*>)
            .orEmpty()
            .mapNotNull { raw ->
                val item = raw as? Map<*, *>
                    ?: return@mapNotNull null

                DeliveryOrderItem(
                    name = item["name"] as? String ?: "",
                    variantName = item["variantName"] as? String,
                    customizationNames =
                        (item["customizationNames"] as? List<*>)
                            ?.filterIsInstance<String>()
                            .orEmpty(),
                    quantity =
                        (item["quantity"] as? Number)?.toInt() ?: 0,
                    unitPrice =
                        (item["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                    lineTotal =
                        (item["lineTotal"] as? Number)?.toDouble() ?: 0.0
                )
            }

        return DeliveryOrder(
            orderId = id,
            customerName = customer?.get("name") as? String ?: "",
            customerPhone = customer?.get("phone") as? String ?: "",
            customerAddress = customer?.get("address") as? String ?: "",
            deliveryLat =
                (data["deliveryLat"] as? Number)?.toDouble() ?: 0.0,
            deliveryLng =
                (data["deliveryLng"] as? Number)?.toDouble() ?: 0.0,
            items = items,
            subtotal =
                (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            deliveryFee =
                (data["deliveryFee"] as? Number)?.toDouble() ?: 0.0,
            grandTotal =
                (data["grandTotal"] as? Number)?.toDouble() ?: 0.0,
            paymentMethod =
                data["paymentMethod"] as? String ?: "COD",
            paymentStatus =
                data["paymentStatus"] as? String ?: "NOT_REQUIRED",
            status =
                data["status"] as? String ?: "READY",
            specialInstructions =
                data["specialInstructions"] as? String ?: ""
        )
    }
}
