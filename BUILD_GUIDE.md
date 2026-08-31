# Pizza Town — Complete Build Guide

Follow these steps **in order**. Skipping steps is the #1 cause of build errors.

---

## Step 1 — Install base tools (WSL / Linux / macOS)

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk unzip zip git
java -version   # confirm 17.x
```

## Step 2 — Install Android SDK

```bash
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdtools.zip
unzip cmdtools.zip && mv cmdline-tools latest && rm cmdtools.zip

echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

## Step 3 — Get the project into WSL's own filesystem

Don't extract onto `/mnt/c/...` (Windows drive) — it's slow and can cause file-lock build errors. Use your Linux home directory:

```bash
mkdir -p ~/projects && cd ~/projects
unzip /mnt/c/Users/<your-username>/Downloads/PizzaTown.zip
cd PizzaTown
```

## Step 4 — Generate the Gradle wrapper jar (one-time)

The project ships `gradlew`, `gradlew.bat`, and `gradle-wrapper.properties`, but not the binary `gradle-wrapper.jar`. Generate it once:

```bash
wget https://services.gradle.org/distributions/gradle-8.9-bin.zip -P /tmp
sudo unzip -q /tmp/gradle-8.9-bin.zip -d /opt
export PATH="/opt/gradle-8.9/bin:$PATH"

gradle wrapper --gradle-version 8.9
chmod +x gradlew
```

From here on, always use `./gradlew`, never the system `gradle`.

---

## Step 5 — Create the Firebase project

1. Go to [Firebase Console](https://console.firebase.google.com) → **Add project**.
2. **Add Android app** #1 — package name `com.pizzatown.customer` → download `google-services.json` → place at `customer-app/google-services.json`.
3. **Add Android app** #2 — package name `com.pizzatown.admin` → download `google-services.json` → place at `admin-app/google-services.json`.
4. **Build → Authentication** → Get started → **Sign-in method** tab → enable **Email/Password**.
5. **Build → Firestore Database** → Create database → production mode → pick a region close to you.

## Step 6 — Deploy Firestore rules & indexes

```bash
npm install -g firebase-tools
firebase login
cd ~/projects/PizzaTown
firebase init firestore
# When asked, point it at: firebase/firestore.rules and firebase/firestore.indexes.json
firebase deploy --only firestore:rules,firestore:indexes
```

Or paste `firebase/firestore.rules` manually into Firebase Console → Firestore → Rules tab, and create the composite index manually: Firestore → Indexes → Add index → collection `orders`, fields `userId` (Ascending) + `createdAt` (Descending). (If you skip this, the app still works — Firestore will show an error in Logcat with a direct link to auto-create the missing index the first time the query runs.)

**Firebase Storage is NOT needed** — this project uses Cloudinary instead (next step), so there's nothing to set up in the Storage tab.

## Step 7 — Create the admin account (with custom claim)

1. Firebase Console → Authentication → Users → **Add user** (email + password) — this is your admin login.
2. Firebase Console → ⚙️ Project Settings → **Service accounts** tab → **Generate new private key** → downloads a JSON file.
3. Run the claim script:
   ```bash
   mkdir -p ~/pizzatown-admin-setup && cd ~/pizzatown-admin-setup
   npm init -y
   npm install firebase-admin
   cp ~/projects/PizzaTown/firebase/setAdminClaim.js .
   # move your downloaded key here, renamed to serviceAccountKey.json
   node setAdminClaim.js owner@pizzatown.com
   ```
   You should see "Granted admin access to owner@pizzatown.com."

## Step 8 — Cloudinary setup (image uploads, free)

1. Create a free account at [cloudinary.com](https://cloudinary.com).
2. Dashboard → copy your **Cloud name**.
3. Settings (⚙️) → **Upload** tab → **Upload presets** → **Add upload preset** → set **Signing Mode = Unsigned** → Save → copy the preset name.
4. Edit these two files (only these two lines each):
   ```
   admin-app/src/main/java/com/pizzatown/admin/core/cloudinary/CloudinaryConfig.kt
   customer-app/src/main/java/com/pizzatown/customer/core/cloudinary/CloudinaryConfig.kt
   ```
   ```kotlin
   const val cloudName: String = "your-real-cloud-name"
   const val uploadPreset: String = "your-real-preset-name"
   ```

## Step 9 — Cashfree payment setup

Checkout has two payment methods: **Online Payment** (Cashfree) and **Cash on Delivery** — there is no WhatsApp ordering flow.

1. Set the Cloud Functions secrets (use the sandbox App ID / Secret Key from the supplied `APIKey.csv`):
   ```bash
   firebase functions:secrets:set CASHFREE_APP_ID
   firebase functions:secrets:set CASHFREE_SECRET_KEY
   ```
2. In the Cashfree dashboard, add a webhook pointed at the deployed `cashfreeWebhook` function URL (you'll see the exact URL after `firebase deploy --only functions`).
3. The Android app only needs one thing configured — it never sees the secret key:
   `customer-app/src/main/java/com/pizzatown/customer/core/payment/CashfreeConfig.kt` should have `environment = CFSession.Environment.SANDBOX` for testing (this is the default already).

See README.md §4 for the full flow and how to switch to production later.

---

## Step 10 — Build debug APKs

```bash
cd ~/projects/PizzaTown
./gradlew :admin-app:assembleDebug
./gradlew :customer-app:assembleDebug
```

Output:
```
admin-app/build/outputs/apk/debug/admin-app-debug.apk
customer-app/build/outputs/apk/debug/customer-app-debug.apk
```

Install on a connected device/emulator:
```bash
adb install -r admin-app/build/outputs/apk/debug/admin-app-debug.apk
adb install -r customer-app/build/outputs/apk/debug/customer-app-debug.apk
```

## Step 11 — First-time testing checklist

1. Open **Admin app** → log in with the account from Step 7 → Dashboard should load (all zeros is fine, menu is empty).
2. Dashboard → **Manage Categories** → add a category (e.g. "Pizza").
3. Dashboard → **Manage Menu** → add an item in that category (fixed price or variants).
4. Open **Customer app** → Register a new account → the menu item you just added should appear.
5. Add to cart → Checkout → it'll ask for a delivery address (first order) → choose **Cash on Delivery** → **Place COD Order** → order should be placed immediately.
6. Add to cart again → Checkout → choose **Online Payment** → **Pay ₹XXX Online** → complete a test payment in the Cashfree sandbox checkout screen (see Cashfree's sandbox test card/UPI details in their docs) → app should show the order as paid.
7. Back in Admin app → **Manage Orders** → both orders should appear, one tagged COD and one Online · Paid → tap "Mark as Confirmed" to test the status workflow.
8. Try **Offers & Banners**, **Broadcast**, **Coupons** from the admin dashboard the same way — each writes to Firestore and the customer app reflects it live.

---

## Step 12 — Release build (for real distribution / Play Store)

```bash
keytool -genkey -v -keystore pizzatown-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pizzatown
```
Copy the template and fill in real values:
```bash
cp keystore.properties.example keystore.properties
# edit keystore.properties with your real storeFile path + passwords
```
Build:
```bash
./gradlew :customer-app:bundleRelease
./gradlew :admin-app:bundleRelease
```
AAB output (what you upload to Play Console):
```
customer-app/build/outputs/bundle/release/customer-app-release.aab
admin-app/build/outputs/bundle/release/admin-app-release.aab
```
If you just want a signed APK instead of an AAB, use `assembleRelease` instead of `bundleRelease`.

## Step 13 — Play Store submission (customer app only)

The **admin app is never published to Play Store** — install its APK directly on the owner's phone, or distribute via Play's internal testing track restricted to your team.

For the customer app:
1. Fill in your real details in `PRIVACY_POLICY.md` and `TERMS_OF_SERVICE.md` (every `[bracketed]` placeholder), host them publicly (GitHub Pages works free), paste the Privacy Policy URL into Play Console → App content → Privacy Policy.
2. Use `DATA_SAFETY_CHECKLIST.md` to fill in Play Console's Data Safety form.
3. Upload the signed `.aab` from Step 12.
4. Create a Firebase **Budget Alert** ($5–10) if you're on the Blaze plan, so you get notified before any unexpected charge — see the pricing discussion earlier in this chat; at ~1000 users your realistic bill is $0/month, but the alert is free insurance.

---

## Troubleshooting — issues we already hit and fixed

| Symptom | Fix |
|---|---|
| `CONFIGURATION_NOT_FOUND` on login | Firebase Console → Authentication → Sign-in method → Email/Password not enabled. Enable it (Step 5.4). |
| Online payment button does nothing / "couldn't start the payment" | `CASHFREE_APP_ID` / `CASHFREE_SECRET_KEY` secrets not set on the Functions side, or Functions haven't been redeployed since setting them — run `firebase deploy --only functions` after `firebase functions:secrets:set ...`. |
| Order stuck showing "Online payment pending" | The webhook isn't configured in the Cashfree dashboard yet, or the customer backgrounded the app before `verifyCashfreePayment` ran — tapping "Retry Payment" (or re-opening the order) re-checks status without creating a new order. |
| `Image upload failed: Object does not exist at location` | This was a Firebase Storage bug — no longer applicable, the app uses Cloudinary now. If you see upload errors now, it's almost always Step 8 not done (`cloudName`/`uploadPreset` still placeholders). |
| Admin app says "not authorized" after login | The account exists in Firebase Auth but doesn't have the `admin` custom claim yet — redo Step 7, and make sure to sign out/in again in the admin app afterward. |
| Build fails with `Unresolved reference` or brace/paren errors | Re-download the zip — these were fixed in earlier rounds; if it persists, paste the exact Gradle error and it'll get fixed fast. |
| `assembleRelease` succeeds but Play Store rejects the upload | You skipped Step 12 — the build silently fell back to debug signing (with a warning printed during the build) because `keystore.properties` didn't exist. |
| Firestore query error mentioning a missing index, with a console link | Click the link in the error — it auto-creates the exact index needed. This can happen for the order-history query if you skipped deploying `firestore.indexes.json` in Step 6. |

---

## Quick reference — all config files you'll touch

```
customer-app/google-services.json                                        (Step 5)
admin-app/google-services.json                                           (Step 5)
admin-app/.../core/cloudinary/CloudinaryConfig.kt                        (Step 8)
customer-app/.../core/cloudinary/CloudinaryConfig.kt                     (Step 8)
functions/.env.<project-id>  (CASHFREE_ENV) + secrets (CASHFREE_APP_ID/KEY) (Step 9)
keystore.properties                                                      (Step 12, release only)
PRIVACY_POLICY.md / TERMS_OF_SERVICE.md                                  (Step 13, Play Store only)
```
