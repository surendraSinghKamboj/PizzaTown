# Pizza Town — Android Food Ordering System

Two independent Android apps sharing one Firebase backend:

| App | Application ID | Purpose |
|---|---|---|
| **customer-app** | `com.pizzatown.customer` | Customers browse the menu, order, and pay online (Cashfree) or Cash on Delivery |
| **admin-app** | `com.pizzatown.admin` | Restaurant owner manages categories, menu items, pricing, and images |

The system is **fully generic** — pizza, burgers, drinks, mocktails, combos, anything — driven entirely by Firestore data. No food-specific code exists anywhere.

---

## 1. Architecture

```
Compose UI → ViewModel → UseCase → Repository (interface) → Repository (impl) → Firebase / Room
```

Clean Architecture + MVVM, per app:

```
core/       navigation, firebase, payment (customer only — Cashfree bridge), common (UiState), util
data/       model (Firestore DTOs), local (Room, customer only), repository (impl + Hilt bindings)
domain/     model, repository (interfaces), usecase
presentation/  one package per feature (auth, menu, cart, checkout, profile, categories, dashboard...)
ui/theme/   Color.kt, Theme.kt, Type.kt, Shape.kt
```

Generic menu model (used by both apps): `MenuItem`, `MenuVariant`, `CustomizationGroup`, `CustomizationOption`. There is intentionally no `PizzaItem`, `BurgerItem`, etc.

---

## 2. Technology Stack

Kotlin, Jetpack Compose, Material 3, Hilt, Coroutines/Flow, Navigation Compose, Room (customer cart), DataStore (theme preference), Firebase Auth + Firestore, Cloudinary + OkHttp (image uploads), Coil, Lottie Compose (splash), Gradle Version Catalog.

**Minimum supported Android version: Android 12 (API 31)** for both apps, `compileSdk`/`targetSdk` 35.

---

## 3. Firebase Setup

1. Go to the [Firebase Console](https://console.firebase.google.com) → **Create project** (e.g. "Pizza Town").
2. **Add Android app** #1:
   - Package name: `com.pizzatown.customer`
   - Download `google-services.json` → place it at `customer-app/google-services.json`
3. **Add Android app** #2:
   - Package name: `com.pizzatown.admin`
   - Download `google-services.json` → place it at `admin-app/google-services.json`
4. **Authentication** → Sign-in method → enable **Email/Password**.
5. **Firestore Database** → Create database (production mode, pick a region close to your users).
6. Deploy Firestore security rules (Firebase CLI) — **Storage is not needed** (this project uses Cloudinary for images instead, see §5):
   ```bash
   npm install -g firebase-tools
   firebase login
   firebase init firestore   # point at firebase/firestore.rules
   firebase deploy --only firestore:rules
   ```
   Or paste `firebase/firestore.rules` directly into the Firebase Console → Firestore → Rules tab.

### Creating the first admin account

There is **no admin self-registration** anywhere in the app (by design — see spec §35). Admin authorization is decided by a Firebase **custom claim** (`admin: true`), which can only be set from a trusted server environment (Admin SDK / Cloud Function) — never from either Android app.

Steps:
1. Create the admin's login normally: in Firebase Console → Authentication → Add user (email + password).
2. Grant the claim using the Admin SDK, e.g. a one-off Node.js script run locally with a service account key (**never** commit this key or ship it inside either app):
   ```js
   const admin = require("firebase-admin");
   admin.initializeApp({ credential: admin.credential.cert(require("./serviceAccountKey.json")) });

   admin.auth().getUserByEmail("owner@pizzatown.example").then(user =>
     admin.auth().setCustomUserClaims(user.uid, { admin: true })
   ).then(() => console.log("Admin claim set."));
   ```
3. Sign out and back in on the Admin app (or wait for the ID token to refresh) so the new claim takes effect.

Firestore rules re-check `request.auth.token.admin == true` on every write, so this cannot be bypassed by modifying the APK.

---

## 4. Payments (Cashfree) — Online + Cash on Delivery

Checkout offers two payment methods: **Online Payment** (Cashfree) and **Cash on Delivery**. There is no WhatsApp ordering flow — it has been fully removed.

### How it works

The Cashfree App ID and Secret Key are **never** shipped inside the Android APK. All Cashfree API calls (creating the order, verifying payment) happen server-side in Cloud Functions using the secret key; the app only ever receives a `payment_session_id` to open the Cashfree Android SDK checkout screen with.

```
Customer app  --(1) creates Firestore order (PENDING)-->  Firestore
Customer app  --(2) createCashfreeOrder(orderId)-->        Cloud Function --> Cashfree (creates order, using secret key)
Customer app  <--(3) payment_session_id-------------------- Cloud Function
Customer app  --(4) opens Cashfree Android SDK checkout with that session id
Customer app  --(5) verifyCashfreePayment(orderId)-->       Cloud Function --> Cashfree (fetches REAL status)
                                                              Cloud Function --> Firestore (writes paymentStatus)
Cashfree      --(6) webhook (independent, reliable fallback)--> cashfreeWebhook Cloud Function --> Firestore
```

Steps 5 and 6 are both idempotent and both go straight to Cashfree for the truth — the Android SDK's own success/failure callback is never trusted as final confirmation.

### One-time setup

1. **Cloud Functions secrets** (values come from the supplied `APIKey.csv` for sandbox — first column is the App ID, second is the Secret Key):
   ```
   firebase functions:secrets:set CASHFREE_APP_ID
   firebase functions:secrets:set CASHFREE_SECRET_KEY
   ```
2. **Webhook** — in the Cashfree Merchant Dashboard (Developers → Webhooks), add the deployed `cashfreeWebhook` function's HTTPS trigger URL (shown after `firebase deploy --only functions`, looks like `https://asia-south1-<project-id>.cloudfunctions.net/cashfreeWebhook`) and subscribe it to payment events.
3. Deploy: `firebase deploy --only functions`.

### Switching sandbox → production

Two places, kept intentionally simple and independent of any other code changes:

- **Backend**: set the `CASHFREE_ENV` Cloud Functions param to `PRODUCTION` (via a `functions/.env.<project-id>` file, e.g. `CASHFREE_ENV=PRODUCTION`), and re-set `CASHFREE_APP_ID` / `CASHFREE_SECRET_KEY` secrets to your live credentials.
- **Android app**: flip `CashfreeConfig.environment` in
  ```
  customer-app/src/main/java/com/pizzatown/customer/core/payment/CashfreeConfig.kt
  ```
  from `SANDBOX` to `PRODUCTION`.

Do both at the same time — a mismatch (e.g. app in PRODUCTION mode pointed at a sandbox session, or vice versa) will fail at the Cashfree checkout screen.

### Payment + order data model

Every order document now also stores:

| Field | Values | Notes |
|---|---|---|
| `paymentMethod` | `ONLINE`, `COD` | Set once at order creation, never changes. |
| `paymentStatus` | `PENDING`, `PAID`, `FAILED`, `NOT_REQUIRED`, `CANCELLED` | `NOT_REQUIRED` for COD. Only Cloud Functions (Admin SDK) can ever move this to `PAID` — see `firestore.rules`. |
| `cashfreeOrderId` | string | The Cashfree order id (usually same as the Firestore order id; may have a `_r<timestamp>` suffix if payment was retried after the first attempt expired). |
| `cashfreePaymentId` | string | Cashfree's payment id, once known. |

---

## 5. Cloudinary Setup (Image Uploads)

This project uses **Cloudinary** instead of Firebase Storage for all image uploads (menu item photos, profile photos) — Firebase Storage requires the paid Blaze plan, while Cloudinary's free tier (25GB storage/bandwidth per month) is more than enough for a single restaurant.

1. Create a free account at [cloudinary.com](https://cloudinary.com).
2. Dashboard → copy your **Cloud name**.
3. Settings (gear icon) → **Upload** tab → **Upload presets** → **Add upload preset**:
   - Set **Signing Mode** to **Unsigned** (this is what lets the app upload directly without embedding any secret key in the APK — Cloudinary's officially supported approach for mobile/client-only apps)
   - Optionally restrict folder, max file size, allowed formats from the same screen
   - Save, and copy the preset's name
4. Paste both values into **two files**:
   ```
   admin-app/src/main/java/com/pizzatown/admin/core/cloudinary/CloudinaryConfig.kt
   customer-app/src/main/java/com/pizzatown/customer/core/cloudinary/CloudinaryConfig.kt
   ```
   ```kotlin
   const val cloudName: String = "your-cloud-name"
   const val uploadPreset: String = "pizza_town_unsigned"
   ```

That's it — no other Firebase Storage setup, rules, or billing plan upgrade needed. `firebase/storage.rules` is kept only to lock down an unused Storage bucket if one exists on your project; you don't need to deploy it.

## 6. Logo & Branding

- The real Pizza Town logos are bundled at `res/drawable-nodpi/pizza_town_logo.png` in **both** apps (each app has its own logo variant — the admin app's includes an "ADMIN" badge instead of "BEST IN TOWN") and are used on the splash fallback / login screen.
- **Launcher icons are already generated from the real logos** (adaptive icon PNGs at every density, `mipmap-mdpi` through `mipmap-xxxhdpi`, plus legacy fallback icons) — nothing further to do here.
- The theme (`ui/theme/Color.kt` in each app) is set to the logo's palette: golden-yellow `#F5B301`, crust brown `#3D2314`, badge red `#C1272D`, on a clean white (light mode) / near-black (dark mode) background. Change these two files to re-theme either app.
- The **splash animation** (`customer-app/src/main/res/raw/splash_animation.json`) is a real Lottie file rendered by `SplashScreen.kt`. It plays once, respects the system "remove animations" accessibility setting, and only navigates onward once both the animation and the auth check have finished.

## 7. Theme (Light / Dark)

- Both apps follow the device's system light/dark setting by default.
- The **customer app** additionally lets each user override this from **Profile → Appearance** (System / Light / Dark), persisted on-device via DataStore so the choice survives app restarts.
- Backgrounds are plain white (light) / `#121212` near-black (dark) in both apps — not a tinted color — so photos and content stay legible.

## 8. Delivery Addresses

- Registration does **not** ask for a delivery address — only name, mobile, email, password.
- A customer is asked for their address **on their first checkout** instead; it's saved to their profile automatically once entered.
- Customers can save multiple addresses (Home / Work / Other, or a custom label) from **Profile → Delivery Addresses**, mark one as default, and pick a different one at checkout each time.

## 9. Build Commands (WSL / Linux / macOS)

**One-time step first:** this project ships `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`, but not the binary `gradle-wrapper.jar` (binary files can't travel through this delivery channel). Generate it once — it needs *some* Gradle available locally just for this one step, after which `./gradlew` is fully self-contained and you never need system Gradle again:

```bash
# Install JDK 17 + a bootstrap Gradle (Ubuntu/WSL)
sudo apt update
sudo apt install -y openjdk-17-jdk unzip zip

# Get a temporary Gradle 8.9 just to generate the wrapper jar
wget https://services.gradle.org/distributions/gradle-8.9-bin.zip -P /tmp
sudo unzip -q /tmp/gradle-8.9-bin.zip -d /opt
export PATH="/opt/gradle-8.9/bin:$PATH"

cd PizzaTown          # project root, where this README lives
gradle wrapper --gradle-version 8.9
chmod +x gradlew
```

That's it — `gradlew`/`gradlew.bat` now work standalone. From here on, always use `./gradlew`, never a system-wide `gradle`, so every teammate/CI machine builds with the exact same Gradle version.

Also make sure the Android SDK is available (`ANDROID_HOME` set, with `platform-tools`, `platforms;android-35`, `build-tools;35.0.0` installed via `sdkmanager`) and that `customer-app/google-services.json` and `admin-app/google-services.json` are in place (see §3) before building.

```bash
# Debug builds
./gradlew :customer-app:assembleDebug
./gradlew :admin-app:assembleDebug

# Run all unit tests
./gradlew test

# Instrumentation tests (needs a connected device/emulator)
./gradlew :customer-app:connectedAndroidTest
./gradlew :admin-app:connectedAndroidTest
```

APK output paths:
```
customer-app/build/outputs/apk/debug/customer-app-debug.apk
admin-app/build/outputs/apk/debug/admin-app-debug.apk
```

## 10. Release Build

1. Create a keystore (once):
   ```bash
   keytool -genkey -v -keystore pizzatown-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pizzatown
   ```
2. Copy `keystore.properties.example` (repo root) to `keystore.properties` and fill in your real values — it's already `.gitignore`d:
   ```properties
   storeFile=/absolute/path/to/pizzatown-release.jks
   storePassword=********
   keyAlias=pizzatown
   keyPassword=********
   ```
   Both apps' `build.gradle.kts` automatically pick this up — nothing else to wire. **If `keystore.properties` is missing, `assembleRelease` still succeeds but signs with the debug key and prints a warning** — that build will be rejected by Google Play, so don't skip this step for a real release.
3. Build:
   ```bash
   ./gradlew :customer-app:assembleRelease
   ./gradlew :admin-app:assembleRelease
   # or for Play Store:
   ./gradlew :customer-app:bundleRelease
   ./gradlew :admin-app:bundleRelease
   ```
4. Release builds are minified + resource-shrunk (`isMinifyEnabled`/`isShrinkResources = true`). `proguard-rules.pro` in each app already keeps everything Firestore/Room need via reflection and preserves enum names used as stored string values — you shouldn't need to touch these unless you add new reflection-based libraries.

---

## 11. What the Admin Can Do Without a Developer

Add/edit/delete categories and menu items, upload images, switch a product between Fixed Price and Variants, add/remove Customization Groups and Options, and enable/disable anything — all from the Admin app, live in Firestore, no new APK required.

## 12. Crash Reporting (Firebase Crashlytics)

Both apps report crashes and key non-fatal errors (failed order creation, failed menu save) to **Firebase Crashlytics**, which is free on the Spark plan — no billing upgrade needed, same as Firestore/Auth.

Nothing to configure beyond the `google-services.json` files already set up in §3 — Crashlytics activates automatically. To see reports: Firebase Console → **Crashlytics** (left sidebar). It can take a few minutes after the first crash for the console to show data, and on the very first run Crashlytics needs one prior app launch before it starts reporting.

Crash reports are tagged with the signed-in user's Firebase UID (not their name/email) so you can distinguish "one user hit this 10 times" from "10 different users hit this once."

## 13. Order History

Customers can view their own past orders from **Profile → My Orders** — status, itemized breakdown, delivery address, and totals for each order, most recent first. This reads live from Firestore (`orders` collection, filtered to the signed-in user), so it reflects order status changes the moment an admin updates them (once admin-side status updates are built — currently all orders stay `PENDING` since that's the only status the admin app writes today).

---

## 14. Play Store Submission

Before submitting the customer app to Google Play:

- Fill in your real details in `PRIVACY_POLICY.md` and `TERMS_OF_SERVICE.md` (replace every `[bracketed]` placeholder), host them somewhere public, and paste the Privacy Policy URL into Play Console → App content → Privacy Policy.
- Use `DATA_SAFETY_CHECKLIST.md` to fill in Play Console's Data Safety form quickly and correctly.
- Complete a real release build with your own keystore (§10) — a debug-signed APK/AAB will be rejected.
- The **admin app is not meant for the Play Store** — distribute it privately (direct APK install, or Play's internal testing track restricted to your team) since it's a management tool, not a public app.

## 15. Continuous Integration

`.github/workflows/build.yml` runs on every push/PR: unit tests for both apps, then assembles debug APKs and uploads them as build artifacts. It uses a placeholder `google-services.json` (CI-only, never touches real data) purely so the Firebase Gradle plugin can run — real credentials never enter git, per `.gitignore`.

---

## 16. Project Status

See the progress notes shared during development for what's implemented vs. pending (customer/admin app tests, etc. — check `firebase/` and each app's `src/test` folder for current coverage).
