#!/usr/bin/env bash

cd "$(dirname "$0")" || exit 1

LOG="/tmp/pizzatown_dashboard_fix.log"
FILE="delivery-app/src/main/java/com/pizzatown/delivery/MainActivity.kt"

echo "======================================================"
echo "PIZZATOWN DELIVERY — DASHBOARD FIX"
echo "======================================================"

if [ ! -f "$FILE" ]; then
    echo "ERROR: $FILE not found"
    read -r -p "Press ENTER to close..."
    exit 1
fi

BACKUP="$FILE.backup_dashboard_clean_$(date +%Y%m%d_%H%M%S)"
cp "$FILE" "$BACKUP"

echo
echo "BACKUP:"
echo "$BACKUP"

python3 - "$FILE" <<'PY'
from pathlib import Path
import re
import sys

p = Path(sys.argv[1])
s = p.read_text()

def add_import(text, imp):
    if imp in text:
        return text

    lines = text.splitlines()
    package_index = next(
        i for i, line in enumerate(lines)
        if line.startswith("package ")
    )

    insert = package_index + 1

    while insert < len(lines):
        line = lines[insert]
        if line.startswith("import ") or not line.strip():
            insert += 1
        else:
            break

    lines.insert(insert, imp)
    return "\n".join(lines) + "\n"

imports = [
    "import androidx.compose.foundation.background",
    "import androidx.compose.foundation.layout.Arrangement",
    "import androidx.compose.foundation.layout.Box",
    "import androidx.compose.foundation.layout.Column",
    "import androidx.compose.foundation.layout.Row",
    "import androidx.compose.foundation.layout.Spacer",
    "import androidx.compose.foundation.layout.fillMaxSize",
    "import androidx.compose.foundation.layout.fillMaxWidth",
    "import androidx.compose.foundation.layout.height",
    "import androidx.compose.foundation.layout.padding",
    "import androidx.compose.foundation.layout.size",
    "import androidx.compose.foundation.layout.width",
    "import androidx.compose.foundation.shape.RoundedCornerShape",
    "import androidx.compose.material.icons.filled.CheckCircle",
    "import androidx.compose.material.icons.filled.LocalShipping",
    "import androidx.compose.material.icons.filled.PendingActions",
    "import androidx.compose.material.icons.filled.TrendingUp",
    "import androidx.compose.material3.CardDefaults",
    "import androidx.compose.material3.Surface",
    "import com.pizzatown.delivery.presentation.dashboard.DeliveryDashboardScreen",
]

for imp in imports:
    s = add_import(s, imp)

# Add history/dashboard parameter to DeliveryAppShell if not already there.
s = s.replace(
    """private fun DeliveryAppShell(
    orders: List<com.pizzatown.delivery.domain.model.DeliveryOrder>,""",
    """private fun DeliveryAppShell(
    orders: List<com.pizzatown.delivery.domain.model.DeliveryOrder>,
    history: List<com.pizzatown.delivery.domain.model.DeliveryOrder>,""",
    1
)

# Locate the Home branch inside DeliveryAppShell.
home_pattern = re.compile(
    r'(?ms)^\s*0\s*->\s*\{\s*'
    r'OrdersScreen\('
    r'.*?'
    r'^\s*\}\s*\n\s*1\s*->'
)

m = home_pattern.search(s)

if m:
    block = m.group(0)

    replacement = """                0 -> {
                    DeliveryDashboardScreen(
                        orders = orders
                    )
                }

                1 ->"""

    block = re.sub(
        r'^\s*0\s*->.*?^\s*1\s*->',
        replacement,
        block,
        count=1
    )

    s = s[:m.start()] + block + s[m.end():]
else:
    print("INFO: Existing Home branch pattern not found; dashboard function will still be added.")

# Ensure actual dashboard is available before Profile screen.
if "private fun DeliveryDashboardScreenFallback" not in s:
    dashboard = r'''

@Composable
private fun DeliveryDashboardScreenFallback(
    orders: List<com.pizzatown.delivery.domain.model.DeliveryOrder>
) {
    val ready = orders.count {
        it.status.equals("READY", ignoreCase = true)
    }

    val onTheWay = orders.count {
        it.status.equals("ON_THE_WAY", ignoreCase = true)
    }

    val cod = orders
        .filter { it.paymentMethod.equals("COD", ignoreCase = true) }
        .sumOf { it.grandTotal }

    val totalValue = orders.sumOf { it.grandTotal }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Delivery Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Your delivery activity at a glance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Active deliveries",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = orders.size.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetric(
                    modifier = Modifier.weight(1f),
                    title = "Ready",
                    value = ready.toString(),
                    icon = Icons.Filled.PendingActions
                )

                DashboardMetric(
                    modifier = Modifier.weight(1f),
                    title = "On the way",
                    value = onTheWay.toString(),
                    icon = Icons.Filled.LocalShipping
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetric(
                    modifier = Modifier.weight(1f),
                    title = "COD",
                    value = "₹${cod.toInt()}",
                    icon = Icons.Filled.CheckCircle
                )

                DashboardMetric(
                    modifier = Modifier.weight(1f),
                    title = "Order value",
                    value = "₹${totalValue.toInt()}",
                    icon = Icons.Filled.TrendingUp
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Delivery status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(14.dp))

                    DashboardBar("Ready", ready, maxOf(1, orders.size))
                    Spacer(Modifier.height(10.dp))
                    DashboardBar("On the way", onTheWay, maxOf(1, orders.size))
                }
            }
        }
    }
}

@Composable
private fun DashboardMetric(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DashboardBar(
    label: String,
    value: Int,
    max: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(
                value.toString(),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(5.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (value > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(
                        value.toFloat() / max.toFloat()
                    ),
                    shape = RoundedCornerShape(50.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {}
            }
        }
    }
}
'''

    marker = "\n@Composable\nprivate fun LoginScreen("
    if marker in s:
        s = s.replace(
            marker,
            dashboard + marker,
            1
        )

# Make sure DeliveryAppShell receives history from DeliveryRoot.
if "history = viewModel.history.collectAsState().value" not in s:
    s = s.replace(
        """DeliveryAppShell(
            orders = orders,""",
        """DeliveryAppShell(
            orders = orders,
            history = viewModel.history.collectAsState().value,""",
        1
    )

p.write_text(s)

print("SUCCESS: Dashboard patch generated.")
PY

echo
echo "========== VERIFY SOURCE =========="

grep -n -A25 -B5 \
    "private fun DeliveryAppShell" \
    "$FILE" | head -80

echo
echo "========== VERIFY DASHBOARD =========="

grep -n \
    "DeliveryDashboardScreenFallback\|DeliveryDashboardScreen(" \
    "$FILE" || true

echo
echo "========== KOTLIN COMPILE =========="

./gradlew :delivery-app:compileDebugKotlin

RESULT=$?

echo
echo "======================================================"

if [ "$RESULT" -eq 0 ]; then
    echo "SUCCESS: DASHBOARD COMPILE PASSED"
else
    echo "ERROR: COMPILE FAILED"
fi

echo "EXIT CODE: $RESULT"
echo "BACKUP: $BACKUP"
echo "LOG: $LOG"
echo "======================================================"

read -r -p "Press ENTER to close..."

exit "$RESULT"
