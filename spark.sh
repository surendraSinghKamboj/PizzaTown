cd ~/Desktop/project_updated/PizzaTown_FULL_v2_Home && python3 - <<'PY'
from pathlib import Path
import shutil
import re

root = Path("delivery-app/src/main/java/com/pizzatown/delivery")

# ============================================================
# 1. CREATE ORDER SELECTION STATE
# ============================================================
selection = root / "presentation" / "home" / "DeliveryOrderSelection.kt"
selection.parent.mkdir(parents=True, exist_ok=True)

selection.write_text(r'''package com.pizzatown.delivery.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DeliveryOrderSelection {
    var selectedOrderId by mutableStateOf<String?>(null)
}
''')

# ============================================================
# 2. CREATE FULL ORDER DETAIL SCREEN
# ============================================================
detail = root / "presentation" / "home" / "DeliveryOrderDetailScreen.kt"

detail.write_text(r'''package com.pizzatown.delivery.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pizzatown.delivery.domain.model.DeliveryOrder

@Composable
fun DeliveryOrderDetailScreen(
    order: DeliveryOrder,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onPickUp: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onDelivered: (String) -> Unit
) {
    val isCod = order.paymentMethod.equals("COD", true)
    val isReady = order.status.equals("READY", true)
    val isOnTheWay = order.status.equals("ON_THE_WAY", true) ||
        order.status.equals("OUT_FOR_DELIVERY", true)

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.orderId.takeLast(6).uppercase()}",
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = when {
                            isReady -> "READY • Waiting for pickup"
                            isOnTheWay -> "ON THE WAY"
                            order.status.equals("DELIVERED", true) -> "DELIVERED"
                            else -> order.status
                        },
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Customer",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(order.customerName)
                            Spacer(Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (order.customerPhone.isNotBlank()) {
                                            onCall(order.customerPhone)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(order.customerPhone.ifBlank { "Phone unavailable" })
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Delivery address",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(
                                order.customerAddress.ifBlank {
                                    "Address unavailable"
                                }
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                "Location: %.6f, %.6f".format(
                                    order.deliveryLat,
                                    order.deliveryLng
                                ),
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Order items",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            order.items.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "${item.name} × ${item.quantity}"
                                        )

                                        if (isCod) {
                                            Text(
                                                "₹${item.lineTotal.toInt()}"
                                            )
                                        }
                                    }

                                    item.variantName
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let {
                                            Text(
                                                it,
                                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                            )
                                        }

                                    if (item.customizationNames.isNotEmpty()) {
                                        Text(
                                            item.customizationNames.joinToString(", "),
                                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {

                            Text(
                                "Payment",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(12.dp))

                            if (isCod) {
                                Text("Cash on Delivery")
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Collect cash: ₹${order.grandTotal.toInt()}",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                                )
                            } else {
                                Text("Paid online")
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Payment completed online. No cash to collect."
                                )
                            }
                        }
                    }
                }

                if (order.specialInstructions.isNotBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    "Special instructions",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(order.specialInstructions)
                            }
                        }
                    }
                }

                if (isReady) {
                    item {
                        Button(
                            onClick = { onPickUp(order.orderId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Pick Up Order")
                        }
                    }
                }

                if (isOnTheWay) {
                    item {
                        Button(
                            onClick = { onNavigate(order.orderId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Open Navigation")
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { onDelivered(order.orderId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Mark Delivered")
                        }
                    }
                }
            }
        }
    }
}
''')

# ============================================================
# 3. PATCH MAIN ACTIVITY
# ============================================================
p = root / "MainActivity.kt"
s = p.read_text()
shutil.copy2(p, str(p) + ".before-big-ui-patch.bak")

# Add imports.
imports = [
    "import androidx.compose.foundation.clickable",
    "import com.pizzatown.delivery.presentation.home.DeliveryOrderDetailScreen",
    "import com.pizzatown.delivery.presentation.home.DeliveryOrderSelection",
]

for imp in imports:
    if imp not in s:
        lines = s.splitlines()
        first_import = next(
            (i for i, line in enumerate(lines) if line.startswith("import ")),
            0
        )
        lines.insert(first_import, imp)
        s = "\n".join(lines) + ("\n" if s.endswith("\n") else "")

# Make dashboard/order cards selectable by changing Card modifier
# near the known order-id rendering locations.
targets = [
    'Text(\n                                "Order #${order.orderId.takeLast(6)}"',
    'Text(\n                            "#${order.orderId.takeLast(6).uppercase()}"',
]

# Add selection to the closest Card modifier before each target.
for target in targets:
    pos = s.find(target)
    if pos == -1:
        continue

    card_pos = s.rfind("Card(", 0, pos)
    if card_pos == -1:
        continue

    modifier_pos = s.find("modifier =", card_pos, pos)
    if modifier_pos != -1:
        line_end = s.find("\n", modifier_pos)
        line = s[modifier_pos:line_end]
        if "clickable" not in s[modifier_pos:line_end]:
            # Handle simple modifier = Modifier.fillMaxWidth()
            if "Modifier.fillMaxWidth()" in line:
                s = (
                    s[:modifier_pos]
                    + line.replace(
                        "Modifier.fillMaxWidth()",
                        "Modifier\n                                    .fillMaxWidth()\n"
                        "                                    .clickable {\n"
                        "                                        DeliveryOrderSelection.selectedOrderId = order.orderId\n"
                        "                                    }"
                    )
                    + s[line_end:]
                )

# ============================================================
# 4. COD-ONLY DASHBOARD VALUE
# ============================================================
# Replace common total aggregations with COD-only equivalents.
s = s.replace(
    'orders.sumOf { it.grandTotal }',
    'orders.filter { it.paymentMethod.equals("COD", true) }.sumOf { it.grandTotal }'
)

s = s.replace(
    'orders.filter { it.paymentMethod.equals("COD", true) }\n        .sumOf { it.grandTotal }',
    'orders.filter { it.paymentMethod.equals("COD", true) }\n        .sumOf { it.grandTotal }'
)

# Replace visible latest activity amount with COD-aware rendering.
s = s.replace(
    '''Text(
                            "₹${order.grandTotal.toInt()}",''',
    '''Text(
                            if (order.paymentMethod.equals("COD", true))
                                "₹${order.grandTotal.toInt()}"
                            else
                                "Paid online",'''
)

# Replace payment text in order cards.
s = s.replace(
    '''"Collect on delivery: ₹${order.grandTotal.toInt()}"''',
    '''"Collect on delivery: ₹${order.grandTotal.toInt()}"'''
)

# Existing online amount text patterns.
s = s.replace(
    '''"Delivered • ₹${order.grandTotal.toInt()}"''',
    '''if (order.paymentMethod.equals("COD", true))
                                "Delivered • ₹${order.grandTotal.toInt()}"
                            else
                                "Delivered • Paid online"'''
)

# ============================================================
# 5. ADD FULL-SCREEN DETAIL DIALOG TO DELIVERY APP SHELL
# ============================================================
shell_pos = s.find("private fun DeliveryAppShell(")
if shell_pos == -1:
    raise SystemExit("DeliveryAppShell not found")

brace = s.find("{", shell_pos)
if brace == -1:
    raise SystemExit("DeliveryAppShell body not found")

detail_overlay = r'''
    val selectedDeliveryOrder =
        DeliveryOrderSelection.selectedOrderId?.let { selectedId ->
            (orders + history).firstOrNull { it.orderId == selectedId }
        }

    if (selectedDeliveryOrder != null && navigationOrderId == null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                DeliveryOrderSelection.selectedOrderId = null
            }
        ) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxSize()
            ) {
                DeliveryOrderDetailScreen(
                    order = selectedDeliveryOrder,
                    onBack = {
                        DeliveryOrderSelection.selectedOrderId = null
                    },
                    onCall = onCall,
                    onPickUp = { orderId ->
                        onPickedUp(orderId)
                        DeliveryOrderSelection.selectedOrderId = null
                    },
                    onNavigate = { orderId ->
                        navigationOrderId = orderId
                        DeliveryOrderSelection.selectedOrderId = null
                    },
                    onDelivered = { orderId ->
                        onDelivered(orderId)
                        DeliveryOrderSelection.selectedOrderId = null
                    }
                )
            }
        }
    }

'''

# Only insert once.
if "val selectedDeliveryOrder =" not in s:
    s = s[:brace + 1] + detail_overlay + s[brace + 1:]

# ============================================================
# 6. CLEAR SELECTION WHEN NAVIGATION OPENS
# ============================================================
s = s.replace(
    'navigationOrderId = orderId',
    'navigationOrderId = orderId\n                    DeliveryOrderSelection.selectedOrderId = null'
)

p.write_text(s)

print("================================================")
print("BIG DELIVERY UI PATCH APPLIED")
print("COD -> cash amount visible")
print("ONLINE -> amount hidden")
print("Order detail screen -> added")
print("Order selection -> added")
print("Navigation wiring -> preserved")
print("Backups -> created")
print("================================================")
PY

./gradlew :delivery-app:clean :delivery-app:assembleDebug --rerun-tasks
