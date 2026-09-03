cd ~/Desktop/project_updated/PizzaTown_FULL_v2_Home

python3 - <<'PY'
from pathlib import Path
import re

# ============================================================
# 1) MAIN ACTIVITY
#    Fix EMPTY onNavigate callback.
#    Open Navigation -> Live Navigation
# ============================================================

p = Path("delivery-app/src/main/java/com/pizzatown/delivery/MainActivity.kt")
s = p.read_text()

old = '''        onCloseNavigation = {
            navigationOrderId = null
        }
    , onNavigate = { })
'''

new = '''        onCloseNavigation = {
            navigationOrderId = null
        },
        onNavigate = { orderId ->
            navigationOrderId = orderId
            DeliveryOrderSelection.selectedOrderId = null
        }
    )
'''

if old in s:
    s = s.replace(old, new, 1)
elif re.search(r'onCloseNavigation\s*=\s*\{\s*navigationOrderId\s*=\s*null\s*\}\s*,\s*onNavigate\s*=\s*\{\s*\}\s*\)', s):
    s = re.sub(
        r'''onCloseNavigation\s*=\s*\{\s*navigationOrderId\s*=\s*null\s*\}\s*,\s*onNavigate\s*=\s*\{\s*\}\s*\)''',
        '''onCloseNavigation = {
            navigationOrderId = null
        },
        onNavigate = { orderId ->
            navigationOrderId = orderId
            DeliveryOrderSelection.selectedOrderId = null
        }
    )''',
        s,
        count=1,
    )
elif re.search(r'onNavigate\s*=\s*\{\s*\}\s*\)', s):
    # Last-resort exact replacement of the still-empty callback.
    s = s.replace(
        'onNavigate = { })',
        '''onNavigate = { orderId ->
            navigationOrderId = orderId
            DeliveryOrderSelection.selectedOrderId = null
        })''',
        1
    )
else:
    raise SystemExit("ERROR 1: Empty onNavigate callback not found.")

p.write_text(s)
print("1/3 PATCHED: Open Navigation -> Live Navigation")


# ============================================================
# 2) DELIVERY REPOSITORY
#    Reconcile READY + assigned active snapshots.
#    DELIVERED order disappears immediately and cannot return.
# ============================================================

p = Path(
    "delivery-app/src/main/java/com/pizzatown/delivery/data/DeliveryRepository.kt"
)
s = p.read_text()

start = s.find("    fun observeAssignedOrders(")
end = s.find("    fun observeDeliveredOrders(", start)

if start == -1 or end == -1:
    raise SystemExit(
        "ERROR 2: observeAssignedOrders()/observeDeliveredOrders() boundaries not found."
    )

new_function = '''    fun observeAssignedOrders(
        deliveryBoyId: String
    ): Flow<List<DeliveryOrder>> = callbackFlow {

        val ordersById = mutableMapOf<String, DeliveryOrder>()
        val readyIds = mutableSetOf<String>()
        val assignedIds = mutableSetOf<String>()

        fun emitOrders() {
            trySend(
                ordersById.values
                    .filter { order ->
                        order.orderId in readyIds || order.orderId in assignedIds
                    }
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

                val documents = snapshot?.documents.orEmpty()
                val currentReadyIds = documents
                    .map { it.id }
                    .toSet()

                // Remove orders that disappeared from READY.
                readyIds
                    .filter { it !in currentReadyIds }
                    .toList()
                    .forEach { orderId ->
                        readyIds.remove(orderId)

                        // If it is not an assigned active order either,
                        // remove it completely from the merged state.
                        if (orderId !in assignedIds) {
                            ordersById.remove(orderId)
                        }
                    }

                documents.forEach { doc ->
                    doc.toDeliveryOrder()?.let { order ->
                        readyIds.add(order.orderId)
                        ordersById[order.orderId] = order
                    }
                }

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

                val documents = snapshot?.documents.orEmpty()
                val currentAssignedIds = documents
                    .map { it.id }
                    .toSet()

                // THIS is the missing reconciliation:
                // when ON_THE_WAY -> DELIVERED, the order disappears
                // from this query, so remove it from the merged map.
                assignedIds
                    .filter { it !in currentAssignedIds }
                    .toList()
                    .forEach { orderId ->
                        assignedIds.remove(orderId)

                        if (orderId !in readyIds) {
                            ordersById.remove(orderId)
                        }
                    }

                documents.forEach { doc ->
                    doc.toDeliveryOrder()?.let { order ->
                        assignedIds.add(order.orderId)
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

'''

s = s[:start] + new_function + s[end:]

p.write_text(s)
print("2/3 PATCHED: Active repository snapshot reconciliation")


# ============================================================
# 3) VERIFY that the immediate ViewModel removal remains present
# ============================================================

p = Path(
    "delivery-app/src/main/java/com/pizzatown/delivery/presentation/DeliveryViewModel.kt"
)
s = p.read_text()

if '''_orders.value = _orders.value.filterNot {
                    it.orderId == orderId
                }''' in s:
    print("3/3 VERIFIED: Delivered order immediate UI removal already present.")
else:
    start = s.find("    fun markDelivered(")
    if start == -1:
        raise SystemExit("ERROR 3: markDelivered() not found.")

    end = s.find("\n    fun logout()", start)
    if end == -1:
        raise SystemExit("ERROR 3: logout() boundary not found.")

    block = s[start:end]

    old_success = '''            }.onSuccess {
                onSuccess()
            }.onFailure {
'''

    new_success = '''            }.onSuccess {
                _orders.value = _orders.value.filterNot {
                    it.orderId == orderId
                }
                onSuccess()
            }.onFailure {
'''

    if old_success not in block:
        raise SystemExit("ERROR 3: markDelivered success block not found.")

    block = block.replace(old_success, new_success, 1)
    s = s[:start] + block + s[end:]
    p.write_text(s)
    print("3/3 PATCHED: Delivered order immediate UI removal")

print()
print("============================================================")
print("PATCH COMPLETE")
print("============================================================")
PY

echo
echo "========== VERIFY OPEN NAVIGATION =========="
grep -n -A12 -B6 'onNavigate =' \
delivery-app/src/main/java/com/pizzatown/delivery/MainActivity.kt

echo
echo "========== VERIFY DELIVERED REMOVAL =========="
grep -n -A20 -B4 'fun markDelivered' \
delivery-app/src/main/java/com/pizzatown/delivery/presentation/DeliveryViewModel.kt

echo
echo "========== VERIFY REPOSITORY RECONCILIATION =========="
sed -n '20,135p' \
delivery-app/src/main/java/com/pizzatown/delivery/data/DeliveryRepository.kt
