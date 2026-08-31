#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="pizzatownitawa"
CSV="$HOME/Downloads/OLD/APIKey.csv"

echo "=========================================="
echo " PizzaTown Cashfree SANDBOX Setup"
echo "=========================================="
echo

if [ ! -f "$CSV" ]; then
  echo "ERROR: APIKey.csv not found:"
  echo "$CSV"
  exit 1
fi

echo "[1/5] Reading Cashfree TEST credentials..."

read -r APP_ID SECRET_KEY < <(
  python3 - "$CSV" <<'PY'
import csv
import sys

path = sys.argv[1]

with open(path, newline="", encoding="utf-8-sig") as f:
    rows = list(csv.reader(f))

if not rows or len(rows[0]) < 2:
    raise SystemExit("APIKey.csv must contain App ID and Secret Key.")

app_id = rows[0][0].strip()
secret = rows[0][1].strip()

if not app_id or not secret:
    raise SystemExit("Cashfree App ID or Secret Key is empty.")

print(app_id, secret)
PY
)

echo "Cashfree App ID loaded: ${APP_ID:0:4}****"
echo "Cashfree Secret Key loaded: ****${SECRET_KEY: -4}"
echo

echo "[2/5] Selecting Firebase project..."
firebase use "$PROJECT_ID"

echo
echo "[3/5] Setting CASHFREE_APP_ID..."
printf '%s\n' "$APP_ID" | firebase functions:secrets:set CASHFREE_APP_ID --project "$PROJECT_ID"

echo
echo "[4/5] Setting CASHFREE_SECRET_KEY..."
printf '%s\n' "$SECRET_KEY" | firebase functions:secrets:set CASHFREE_SECRET_KEY --project "$PROJECT_ID"

echo
echo "[5/5] Deploying Firebase Functions..."
firebase deploy --only functions --project "$PROJECT_ID"

echo
echo "=========================================="
echo " Cashfree SANDBOX setup complete"
echo "=========================================="
echo
echo "Next:"
echo "  ./gradlew :customer-app:assembleDebug"
echo "  ./gradlew :admin-app:assembleDebug"
echo
echo "IMPORTANT:"
echo "The Cashfree secret was NOT written into Android source code."
