// DELIVERY STATUS CONSTANTS
const DELIVERY_STATUS_READY = "READY";
const DELIVERY_STATUS_OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY";
const DELIVERY_STATUS_DELIVERED = "DELIVERED";

function isDeliveryRole(decodedToken) {
  return decodedToken && decodedToken.role === "delivery";
}

/**
 * PizzaTown Cloud Functions
 * =========================
 * Push notifications can only be *sent* from a trusted server — neither
 * Android app has (or should have) the credentials to message another
 * user's device directly. These Firestore-triggered functions run with
 * the Admin SDK (which bypasses firestore.rules) and are the bridge:
 *
 *   orders/{orderId}      created         -> notify admin (new COD order;
 *                                            ONLINE orders notify once paid)
 *   orders/{orderId}      status changes  -> notify that customer
 *   orders/{orderId}      paymentStatus -> PAID (ONLINE)  -> notify admin
 *   coupons/{couponId}    targeted coupon -> notify that customer
 *   broadcasts/{id}       admin broadcast -> notify one customer, or
 *                                            everyone (topic) if untargeted
 *   settings/restaurantStatus  opened     -> notify every customer
 *
 * Online payments (Cashfree)
 * --------------------------
 * The Cashfree secret key never leaves this file — the Android app only
 * ever sees a `payment_session_id`, never the App ID / Secret Key. Flow:
 *
 *   1. Customer app creates the Firestore order itself (paymentMethod:
 *      ONLINE, paymentStatus: PENDING) — same as it always has for COD.
 *   2. Customer app calls createCashfreeOrder(orderId) — this function
 *      creates the actual order with Cashfree using the secret key and
 *      returns a payment_session_id.
 *   3. Customer app opens the Cashfree Android SDK checkout with that
 *      session id.
 *   4. When the SDK callback fires (success, failure, or cancel), the app
 *      calls verifyCashfreePayment(orderId) — this function asks Cashfree
 *      directly what really happened and updates Firestore. The SDK
 *      callback itself is NEVER treated as proof of payment.
 *   5. cashfreeWebhook is a second, independent path to the same result:
 *      Cashfree calls it directly (configure the URL in the Cashfree
 *      dashboard) so payment status is still corrected even if the app
 *      is killed mid-payment. Both paths are idempotent — whichever
 *      Firestore write actually flips PENDING -> PAID first is the one
 *      that triggers the "order paid" admin notification below; a second,
 *      redundant write to the same status is a no-op for notifications.
 *
 * Switching sandbox -> production: update the CASHFREE_APP_ID /
 * CASHFREE_SECRET_KEY secrets (`firebase functions:secrets:set ...`) to
 * live credentials and set the CASHFREE_ENV param to "PRODUCTION" (see
 * below) — no code changes needed. Remember to also flip
 * CashfreeConfig.environment in the Android app to PRODUCTION at the
 * same time; see that file's doc comment.
 *
 * Deploy with: firebase deploy --only functions
 * (requires `firebase login` and the Blaze plan — Cloud Functions for
 * Firebase does not run on the free Spark plan, and outbound network
 * calls to Cashfree's API require it).
 */

const { onDocumentUpdated, onDocumentWritten, onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret, defineString } = require("firebase-functions/params");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const logger = require("firebase-functions/logger");
const crypto = require("crypto");

initializeApp();
const db = getFirestore();
const messaging = getMessaging();

// Must match firebase.json / firestore.indexes.json's Firestore location.
const REGION = "asia-south1";

// Same topic names the apps subscribe to — customer app in
// NotificationConstants.kt, admin app in AdminNotificationConstants.kt.
const BROADCAST_TOPIC = "customers_all";
const ADMIN_TOPIC = "admin_all";

// ---- Cashfree config ----
// Secrets — set once with:
//   firebase functions:secrets:set CASHFREE_APP_ID
//   firebase functions:secrets:set CASHFREE_SECRET_KEY
// (values come from APIKey.csv for sandbox; replace with live credentials
// from the Cashfree dashboard when switching to production).
const CASHFREE_APP_ID = defineSecret("CASHFREE_APP_ID");
const CASHFREE_SECRET_KEY = defineSecret("CASHFREE_SECRET_KEY");

// "SANDBOX" or "PRODUCTION". Override without a redeploy via a
// functions/.env / .env.<projectId> file (CASHFREE_ENV=PRODUCTION) once
// you're ready to go live — see the Firebase "environment configuration"
// docs. Must stay in sync with CashfreeConfig.environment in the Android app.
const CASHFREE_ENV = defineString("CASHFREE_ENV", { default: "SANDBOX" });

const CASHFREE_API_VERSION = "2023-08-01";

function cashfreeBaseUrl() {
  return CASHFREE_ENV.value() === "PRODUCTION"
    ? "https://api.cashfree.com/pg"
    : "https://sandbox.cashfree.com/pg";
}

async function cashfreeRequest(method, path, body) {
  const res = await fetch(`${cashfreeBaseUrl()}${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      "x-api-version": CASHFREE_API_VERSION,
      "x-client-id": CASHFREE_APP_ID.value(),
      "x-client-secret": CASHFREE_SECRET_KEY.value(),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const json = await res.json().catch(() => ({}));
  if (!res.ok) {
    const message = (json && (json.message || json.code)) || `Cashfree API error (${res.status})`;
    const err = new Error(message);
    err.cashfreeStatus = res.status;
    err.cashfreeBody = json;
    throw err;
  }
  return json;
}

/** Maps a Cashfree payment_status (from the payments-for-an-order API or
 *  a webhook payload) to our own PaymentStatus values. */
function mapCashfreePaymentStatus(cfStatus) {
  switch (cfStatus) {
    case "SUCCESS":
      return "PAID";
    case "FAILED":
      return "FAILED";
    case "USER_DROPPED":
    case "CANCELLED":
    case "VOID":
      return "CANCELLED";
    case "PENDING":
    case "NOT_ATTEMPTED":
    default:
      return "PENDING";
  }
}

function haversineDistanceKm(lat1, lon1, lat2, lon2) {
  const toRad = (value) => value * Math.PI / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) *
    Math.cos(toRad(lat2)) *
    Math.sin(dLon / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

async function validateOrderDelivery(order) {
  const statusSnap = await db.collection("settings")
    .doc("restaurantStatus")
    .get();

  const status = statusSnap.exists ? (statusSnap.data() || {}) : {};
  const restaurantOpen = status.open === true;

  if (!restaurantOpen) {
    return { ok: false, reason: "Restaurant is currently closed." };
  }

  const areaSnap = await db.collection("settings")
    .doc("deliveryArea")
    .get();

  const area = areaSnap.exists ? (areaSnap.data() || {}) : {};

  const centerLat = Number(area.centerLat);
  const centerLng = Number(area.centerLng);
  const radiusKm = Number(area.radiusKm);
  const deliveryLat = Number(order.deliveryLat);
  const deliveryLng = Number(order.deliveryLng);

  if (
    !Number.isFinite(centerLat) ||
    !Number.isFinite(centerLng) ||
    !Number.isFinite(radiusKm) ||
    radiusKm <= 0
  ) {
    return { ok: false, reason: "Restaurant delivery area is not configured." };
  }

  if (
    !Number.isFinite(deliveryLat) ||
    !Number.isFinite(deliveryLng) ||
    deliveryLat < -90 || deliveryLat > 90 ||
    deliveryLng < -180 || deliveryLng > 180
  ) {
    return { ok: false, reason: "Customer delivery location is invalid." };
  }

  const distanceKm = haversineDistanceKm(
    deliveryLat,
    deliveryLng,
    centerLat,
    centerLng
  );

  if (distanceKm > radiusKm) {
    return {
      ok: false,
      reason: `Customer is ${distanceKm.toFixed(1)} km away; delivery radius is ${radiusKm.toFixed(1)} km.`,
    };
  }

  // ----------------------------------------------------------
  // Delivery pricing rules
  // ----------------------------------------------------------

  const pricingSnap = await db.collection("settings")
    .doc("deliveryPricing")
    .get();

  const pricing = pricingSnap.exists ? (pricingSnap.data() || {}) : {};

  const minimumOrderValue = Number(pricing.minimumOrderValue || 0);
  const deliveryCharge = Number(pricing.deliveryCharge || 0);
  const freeDeliveryAbove = Number(pricing.freeDeliveryAbove || 0);

  const subtotal = Number(order.subtotal || 0);
  const customerDeliveryFee = Number(order.deliveryFee || 0);

  if (
    !Number.isFinite(minimumOrderValue) ||
    minimumOrderValue < 0 ||
    !Number.isFinite(deliveryCharge) ||
    deliveryCharge < 0 ||
    !Number.isFinite(freeDeliveryAbove) ||
    freeDeliveryAbove < 0 ||
    !Number.isFinite(subtotal) ||
    subtotal < 0 ||
    !Number.isFinite(customerDeliveryFee) ||
    customerDeliveryFee < 0
  ) {
    return {
      ok: false,
      reason: "Invalid order or delivery pricing configuration.",
    };
  }

  if (subtotal < minimumOrderValue) {
    return {
      ok: false,
      reason: `Minimum order value is ₹${minimumOrderValue.toFixed(0)}.`,
    };
  }

  const expectedDeliveryFee =
    freeDeliveryAbove > 0 && subtotal >= freeDeliveryAbove
      ? 0
      : deliveryCharge;

  const pricingTolerance = 0.01;

  if (Math.abs(customerDeliveryFee - expectedDeliveryFee) > pricingTolerance) {
    return {
      ok: false,
      reason:
        `Invalid delivery charge. Expected ₹${expectedDeliveryFee.toFixed(2)} ` +
        `for subtotal ₹${subtotal.toFixed(2)}.`,
    };
  }

  return {
    ok: true,
    distanceKm,
    minimumOrderValue,
    deliveryCharge: expectedDeliveryFee,
    freeDeliveryAbove,
  };
}

const STATUS_LABEL = {
  PENDING: "received",
  CONFIRMED: "confirmed",
  PREPARING: "being prepared",
  READY: "ready",
  COMPLETED: "delivered",
  CANCELLED: "cancelled",
};

/**
 * Sends a high-priority FCM notification + data message to one user's
 * saved token. Foreground messages are handled by the Android messaging
 * service; background messages can be displayed directly by Android.
 * Keys here MUST match NotificationConstants.kt on the client.
 */
async function sendToUser(userId, title, body, extraData) {
  if (!userId) return;
  const userSnap = await db.collection("users").doc(userId).get();
  const token = userSnap.get("fcmToken");
  if (!token) {
    logger.info(`No fcmToken on file for user ${userId}, skipping push.`);
    return;
  }
  try {
    await messaging.send({
      token,

      notification: {
        title,
        body,
      },

      data: {
        title,
        body,
        ...extraData,
      },

      android: {
        priority: "high",

        notification: {
          channelId: "pizzatown_delivery_orders",
          priority: "high",
          defaultSound: true,
          defaultVibrateTimings: true,
        },
      },
    });
  } catch (err) {
    logger.error(`Push to user ${userId} failed`, err);
    if (err.code === "messaging/registration-token-not-registered") {
      // Stale token (uninstalled app, cleared data) — stop retrying against it.
      await db.collection("users").doc(userId).update({ fcmToken: FieldValue.delete() });
    }
  }
}

async function sendToTopic(topic, title, body, extraData) {
  try {
    await messaging.send({ topic, data: { title, body, ...extraData } });
  } catch (err) {
    logger.error(`Push to topic ${topic} failed`, err);
  }
}

// ---- orders/{orderId}: notify admin the moment a customer places a new order ----

// ---- DELIVERY PARTNER MANAGEMENT ----

function requireAdmin(request) {
  if (!request.auth || request.auth.token.admin !== true) {
    throw new HttpsError(
      "permission-denied",
      "Only administrators can manage delivery partners."
    );
  }
}

exports.createDeliveryPartner = onCall(
  { region: REGION },
  async (request) => {
    requireAdmin(request);

    const data = request.data || {};

    const name = String(data.name || "").trim();
    const email = String(data.email || "").trim().toLowerCase();
    let phone = String(data.phone || "").trim();
    const password = String(data.password || "");

    // Firebase Auth expects E.164 for phoneNumber.
    // Make common Indian 10-digit numbers usable from the Admin UI.
    if (/^\d{10}$/.test(phone)) {
      phone = `+91${phone}`;
    }

    if (!name || !email || !phone || password.length < 6) {
      throw new HttpsError(
        "invalid-argument",
        "Name, email, phone and a password of at least 6 characters are required."
      );
    }

    const userRecord = await require("firebase-admin/auth")
      .getAuth()
      .createUser({
        email,
        password,
        displayName: name,
        phoneNumber: phone
      });

    await require("firebase-admin/auth")
      .getAuth()
      .setCustomUserClaims(userRecord.uid, {
        role: "delivery"
      });

    await db.collection("users").doc(userRecord.uid).set({
      name,
      email,
      phone,
      role: "delivery",
      active: true,
      createdAt: Date.now()
    });

    return {
      uid: userRecord.uid
    };
  }
);

exports.updateDeliveryPartner = onCall(
  { region: REGION },
  async (request) => {
    requireAdmin(request);

    const data = request.data || {};

    const uid = String(data.uid || "").trim();
    const name = String(data.name || "").trim();
    const email = String(data.email || "").trim().toLowerCase();
    let phone = String(data.phone || "").trim();

    phone = phone.replace(/[\s()-]/g, "");

    if (/^0?\d{10}$/.test(phone)) {
      phone = phone.replace(/^0/, "");
      phone = `+91${phone}`;
    }

    if (!uid || !name || !email || !phone) {
      throw new HttpsError(
        "invalid-argument",
        "UID, name, email and phone are required."
      );
    }

    if (!/^\+[1-9]\d{7,14}$/.test(phone)) {
      throw new HttpsError(
        "invalid-argument",
        "Enter a valid phone number."
      );
    }

    const auth = require("firebase-admin/auth").getAuth();

    await auth.updateUser(uid, {
      displayName: name,
      email,
      phoneNumber: phone
    });

    await db.collection("users").doc(uid).set(
      {
        name,
        email,
        phone,
        role: "delivery",
        updatedAt: Date.now()
      },
      { merge: true }
    );

    return { success: true };
  }
);

exports.resetDeliveryPartnerPassword = onCall(
  { region: REGION },
  async (request) => {
    requireAdmin(request);

    const data = request.data || {};
    const uid = String(data.uid || "").trim();
    const password = String(data.password || "");

    if (!uid || password.length < 6) {
      throw new HttpsError(
        "invalid-argument",
        "UID and a password of at least 6 characters are required."
      );
    }

    await require("firebase-admin/auth")
      .getAuth()
      .updateUser(uid, { password });

    return { success: true };
  }
);

exports.deleteDeliveryPartner = onCall(
  { region: REGION },
  async (request) => {
    requireAdmin(request);

    const data = request.data || {};
    const uid = String(data.uid || "").trim();

    if (!uid) {
      throw new HttpsError(
        "invalid-argument",
        "Delivery partner UID is required."
      );
    }

    const auth = require("firebase-admin/auth").getAuth();

    await auth.deleteUser(uid);
    await db.collection("users").doc(uid).delete();

    return { success: true };
  }
);

exports.setDeliveryPartnerActive = onCall(
  { region: REGION },
  async (request) => {
    requireAdmin(request);

    const data = request.data || {};
    const uid = String(data.uid || "").trim();
    const active = data.active === true;

    if (!uid) {
      throw new HttpsError("invalid-argument", "Delivery partner UID is required.");
    }

    const auth = require("firebase-admin/auth").getAuth();

    await auth.updateUser(uid, {
      disabled: !active
    });

    await db.collection("users").doc(uid).set(
      {
        active,
        updatedAt: Date.now()
      },
      { merge: true }
    );

    return { success: true };
  }
);

exports.onOrderCreated = onDocumentCreated(
  { document: "orders/{orderId}", region: REGION },
  async (event) => {
    const order = event.data ? event.data.data() : null;
    if (!order) return;

    const validation = await validateOrderDelivery(order);

    if (!validation.ok) {
      logger.warn(
        `Rejecting order ${event.params.orderId}: ${validation.reason}`
      );

      await db.collection("orders").doc(event.params.orderId).update({
        status: "CANCELLED",
        updatedAt: Date.now(),
        cancellationReason: validation.reason,
      });
      return;
    }

    const customerName = order.customer && order.customer.name ? order.customer.name : "A customer";
    const itemCount = order.totalItems || (order.items ? order.items.length : 0);
    const amount = typeof order.grandTotal === "number" ? order.grandTotal.toFixed(2) : "";
    const amountSuffix = amount ? `, ₹${amount}` : "";

    // COD orders are final the moment they're placed, so notify admin right
    // away. ONLINE orders aren't confirmed yet at this point — no money has
    // moved — so their admin notification instead fires from
    // onOrderStatusChanged below, the moment paymentStatus actually becomes
    // PAID (via verifyCashfreePayment or the Cashfree webhook).
    // Customer confirmation: every successfully created order gets an
    // initial "received" notification. Status-change notifications below
    // handle every later state transition.
    await sendToUser(
      order.userId,
      "Order received",
      `Your Pizza Town order has been received${amountSuffix ? ` — ${amountSuffix.replace(", ", "")}` : ""}.`,
      {
        type: "ORDER_STATUS",
        orderId: event.params.orderId,
        status: "PENDING"
      }
    );

    if (order.paymentMethod !== "ONLINE") {
      await sendToTopic(
        ADMIN_TOPIC,
        "New COD order",
        `${customerName} placed a COD order — ${itemCount} item(s)${amountSuffix}.`,
        { type: "NEW_ORDER", orderId: event.params.orderId }
      );
    }
  }
);

// ---- orders/{orderId}: notify the customer whenever admin changes status,
//      and notify admin the moment an ONLINE order is actually paid ----

// ---- DELIVERY STATE TRANSITIONS ----
// Delivery partners never update order status directly from the Android
// client. These callable functions validate the authenticated delivery
// partner, verify the order is READY/assigned correctly, and then perform
// the status transition with the Admin SDK.

exports.markOrderPickedUp = onCall(
  { region: REGION },
  async (request) => {
    if (!request.auth || request.auth.token.role !== "delivery") {
      throw new HttpsError(
        "permission-denied",
        "Only delivery partners can pick up orders."
      );
    }

    const orderId = String(request.data?.orderId || "").trim();
    if (!orderId) {
      throw new HttpsError("invalid-argument", "Order ID is required.");
    }

    const uid = request.auth.uid;
    const orderRef = db.collection("orders").doc(orderId);

    return db.runTransaction(async (tx) => {
      const snap = await tx.get(orderRef);

      if (!snap.exists) {
        throw new HttpsError("not-found", "Order not found.");
      }

      const order = snap.data() || {};
      const currentStatus = String(order.status || "");

      if (currentStatus !== "READY") {
        throw new HttpsError(
          "failed-precondition",
          `Order is not ready for pickup. Current status: ${currentStatus || "UNKNOWN"}.`
        );
      }

      const assignedTo = String(order.deliveryBoyId || "");

      if (assignedTo && assignedTo !== uid) {
        throw new HttpsError(
          "already-exists",
          "This order has already been picked up by another rider."
        );
      }

      const now = FieldValue.serverTimestamp();

      tx.update(orderRef, {
        deliveryBoyId: uid,
        status: "ON_THE_WAY",
        pickedUpAt: now,
        updatedAt: Date.now(),
      });

      return {
        ok: true,
        orderId,
        status: "ON_THE_WAY",
      };
    });
  }
);

exports.markOrderDelivered = onCall(
  { region: REGION },
  async (request) => {
    if (!request.auth || request.auth.token.role !== "delivery") {
      throw new HttpsError(
        "permission-denied",
        "Only delivery partners can complete deliveries."
      );
    }

    const orderId = String(request.data?.orderId || "").trim();
    if (!orderId) {
      throw new HttpsError("invalid-argument", "Order ID is required.");
    }

    const uid = request.auth.uid;
    const orderRef = db.collection("orders").doc(orderId);
    const riderRef = db.collection("users").doc(uid);

    return db.runTransaction(async (tx) => {
      const snap = await tx.get(orderRef);
      const riderSnap = await tx.get(riderRef);

      if (!snap.exists) {
        throw new HttpsError("not-found", "Order not found.");
      }

      const order = snap.data() || {};
      const currentStatus = String(order.status || "");
      const assignedTo = String(order.deliveryBoyId || "");

      if (assignedTo !== uid) {
        throw new HttpsError(
          "permission-denied",
          "This order is not assigned to your delivery account."
        );
      }

      if (currentStatus !== "ON_THE_WAY") {
        throw new HttpsError(
          "failed-precondition",
          `Order cannot be delivered from status ${currentStatus || "UNKNOWN"}.`
        );
      }

      const rider = riderSnap.exists ? (riderSnap.data() || {}) : {};

      const deliveredByName =
        String(rider.fullName || "").trim() ||
        String(rider.name || "").trim() ||
        String(request.auth.token.name || "").trim() ||
        String(request.auth.token.email || "").trim() ||
        "Delivery Partner";

      tx.update(orderRef, {
        status: "DELIVERED",
        deliveredAt: FieldValue.serverTimestamp(),
        deliveredById: uid,
        deliveredByName,
        updatedAt: Date.now(),
      });

      return {
        ok: true,
        orderId,
        status: "DELIVERED",
        deliveredById: uid,
        deliveredByName,
      };
    });
  }
);

exports.onOrderStatusChanged = onDocumentUpdated(
  { document: "orders/{orderId}", region: REGION },
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after) return;

    // When an order becomes READY, notify every active delivery partner.
    // If a specific deliveryBoyId is assigned, that rider is also notified
    // through the same broadcast and therefore is not sent a duplicate.
    const becameReady =
      before.status !== "READY" && after.status === "READY";

    if (becameReady) {
      const customerName =
        after.customer && after.customer.name
          ? after.customer.name
          : "Customer";

      const amount =
        typeof after.grandTotal === "number"
          ? after.grandTotal.toFixed(2)
          : "";

      const riderMessage =
        `${customerName}'s order is ready${amount ? ` — ₹${amount}` : ""}.`;

      const ridersSnapshot = await db
        .collection("users")
        .where("role", "==", "delivery")
      .where("active", "==", true)
        .get();

      await Promise.all(
        ridersSnapshot.docs
          .filter((doc) => doc.id !== after.deliveryBoyId)
          .map((doc) =>
            sendToUser(
              doc.id,
              "New ready order",
              riderMessage,
              {
                type: "NEW_DELIVERY",
                orderId: event.params.orderId
              }
            )
          )
      );

      // Notify the specifically assigned rider as well.
      if (after.deliveryBoyId) {
        await sendToUser(
          after.deliveryBoyId,
          "New delivery assigned",
          riderMessage,
          {
            type: "NEW_DELIVERY",
            orderId: event.params.orderId
          }
        );
      }
    }


    if (before.status !== after.status) {
      const label = STATUS_LABEL[after.status] || String(after.status).toLowerCase();
      await sendToUser(
        after.userId,
        "Order update",
        `Your order is now ${label}.`,
        { type: "ORDER_STATUS", orderId: event.params.orderId }
      );
    }

    // Fires exactly once per order: only on the genuine PENDING -> PAID
    // transition, regardless of whether verifyCashfreePayment or
    // cashfreeWebhook was the one that wrote it (or both, in a race —
    // the second write is PAID -> PAID, which isn't a transition).
    const justPaid = after.paymentMethod === "ONLINE" &&
      before.paymentStatus !== "PAID" &&
      after.paymentStatus === "PAID";

    if (justPaid) {
      const customerName = after.customer && after.customer.name ? after.customer.name : "A customer";
      const itemCount = after.totalItems || (after.items ? after.items.length : 0);
      const amount = typeof after.grandTotal === "number" ? after.grandTotal.toFixed(2) : "";
      await sendToTopic(
        ADMIN_TOPIC,
        "New online order — Paid",
        `${customerName} paid online — ${itemCount} item(s)${amount ? `, ₹${amount}` : ""}.`,
        { type: "NEW_ORDER", orderId: event.params.orderId }
      );
    }
  }
);

// ---- coupons/{couponId}: notify the customer a coupon was made for them ----
exports.onCouponTargeted = onDocumentWritten(
  { document: "coupons/{couponId}", region: REGION },
  async (event) => {
    const before = event.data.before.exists ? event.data.before.data() : null;
    const after = event.data.after.exists ? event.data.after.data() : null;
    if (!after || !after.targetUserId || !after.active) return;

    // Only fire once: on first assignment to this customer, or when the
    // coupon flips from inactive -> active while still targeted at them.
    // Otherwise every unrelated edit (e.g. usageCount incrementing on
    // redemption) would re-notify the customer.
    const newlyTargeted = !before || before.targetUserId !== after.targetUserId;
    const justActivated = before && before.active === false && after.active === true;
    if (!newlyTargeted && !justActivated) return;

    await sendToUser(
      after.targetUserId,
      "A coupon just for you \uD83C\uDF89",
      `Use code ${after.code} on your next Pizza Town order!`,
      { type: "COUPON", couponCode: after.code }
    );
  }
);

// ---- broadcasts/{broadcastId}: admin message — one customer or everyone ----
exports.onBroadcastCreated = onDocumentCreated(
  { document: "broadcasts/{broadcastId}", region: REGION },
  async (event) => {
    const data = event.data ? event.data.data() : null;
    if (!data || !data.message) return;

    const title = data.title || "Pizza Town";
    if (data.targetUserId) {
      await sendToUser(data.targetUserId, title, data.message, { type: "BROADCAST" });
      return;
    }

    await sendToTopic(BROADCAST_TOPIC, title, data.message, { type: "BROADCAST" });
  }
);

// ---- settings/restaurantStatus: notify every customer the moment the shop reopens ----
exports.onRestaurantStatusChanged = onDocumentUpdated(
  { document: "settings/restaurantStatus", region: REGION },
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (!before || !after) return;

    const justOpened = before.open === false && after.open === true;
    if (!justOpened) return;

    await sendToTopic(
      BROADCAST_TOPIC,
      "We're open! \uD83C\uDF55",
      "Pizza Town just opened — order your favorite pizza now!",
      { type: "RESTAURANT_OPEN" }
    );
  }
);

// =====================================================================
// Cashfree online payments
// =====================================================================

const CASHFREE_FUNCTION_CONFIG = {
  region: REGION,
  secrets: [CASHFREE_APP_ID, CASHFREE_SECRET_KEY],
};

/**
 * Callable from the customer app right after it creates a Firestore order
 * with paymentMethod=ONLINE / paymentStatus=PENDING. Creates the matching
 * order with Cashfree (server-side, using the secret key) and returns the
 * payment_session_id the Android SDK needs to open checkout.
 *
 * Safe to call more than once for the same orderId (e.g. the customer's
 * network dropped right after the first call) — it reuses the existing
 * Cashfree order if it's still payable instead of creating a duplicate.
 */
exports.createCashfreeOrder = onCall(CASHFREE_FUNCTION_CONFIG, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be signed in to pay online.");
  }

  const orderId = request.data && request.data.orderId;
  if (!orderId || typeof orderId !== "string") {
    throw new HttpsError("invalid-argument", "orderId is required.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists) {
    throw new HttpsError("not-found", "Order not found.");
  }
  const order = orderSnap.data();

  if (order.userId !== request.auth.uid) {
    throw new HttpsError("permission-denied", "This is not your order.");
  }
  if (order.paymentMethod !== "ONLINE") {
    throw new HttpsError("failed-precondition", "This order is not an online-payment order.");
  }
  if (order.paymentStatus === "PAID") {
    throw new HttpsError("failed-precondition", "This order has already been paid.");
  }
  if (order.status === "CANCELLED") {
    throw new HttpsError("failed-precondition", "This order was cancelled and can no longer be paid.");
  }

  // Defense in depth: onOrderCreated already validated restaurant-open +
  // delivery-radius at creation time and would have cancelled the order if
  // invalid, but re-check here too since time may have passed (e.g. the
  // shop closed between order creation and the customer tapping pay).
  const validation = await validateOrderDelivery(order);
  if (!validation.ok) {
    await orderRef.update({
      status: "CANCELLED",
      paymentStatus: "CANCELLED",
      updatedAt: Date.now(),
      cancellationReason: validation.reason,
    });
    throw new HttpsError("failed-precondition", validation.reason);
  }

  const amount = Number(order.grandTotal);
  if (!Number.isFinite(amount) || amount <= 0) {
    throw new HttpsError("failed-precondition", "This order has an invalid amount.");
  }

  let cfOrderId = order.cashfreeOrderId || "";
  let paymentSessionId = null;

  if (cfOrderId) {
    // Retry path — see if the previous Cashfree order is still usable.
    try {
      const existing = await cashfreeRequest("GET", `/orders/${encodeURIComponent(cfOrderId)}`);
      if (existing.order_status === "PAID") {
        await orderRef.update({
          status: "PENDING",
          paymentStatus: "PAID",
          updatedAt: Date.now()
        });
        throw new HttpsError("failed-precondition", "This order has already been paid.");
      }
      if (existing.order_status === "ACTIVE" && existing.payment_session_id) {
        paymentSessionId = existing.payment_session_id;
      }
      // Anything else (EXPIRED / TERMINATED / TERMINATION_REQUESTED) falls
      // through to minting a fresh Cashfree order below.
    } catch (err) {
      if (err instanceof HttpsError) throw err;
      logger.warn(`Could not fetch existing Cashfree order ${cfOrderId}, will create a new one`, err);
    }
  }

  if (!paymentSessionId) {
    // Cashfree order ids must be unique and, once closed out (expired/
    // terminated), can't be reactivated — so a genuine retry after that
    // point mints a new Cashfree order id, tagged with a suffix we can
    // trace back to the same Firestore order (see cashfreeWebhook).
    cfOrderId = cfOrderId ? `${orderId}_r${Date.now()}` : orderId;

    const phoneDigits = String((order.customer && order.customer.phone) || "").replace(/\D/g, "");

    const created = await cashfreeRequest("POST", "/orders", {
      order_id: cfOrderId,
      order_amount: Number(amount.toFixed(2)),
      order_currency: "INR",
      customer_details: {
        customer_id: order.userId,
        customer_name: (order.customer && order.customer.name) || "PizzaTown Customer",
        customer_phone: phoneDigits || "9999999999",
        // No email is collected from customers in this app; Cashfree
        // requires some value here, so a placeholder is used. Payment
        // identity for verification purposes comes from customer_id/order_id.
        customer_email: `${order.userId}@pizzatown.customer`,
      },
      order_note: `PizzaTown order ${orderId}`,
    });

    paymentSessionId = created.payment_session_id;
    if (!paymentSessionId) {
      logger.error("Cashfree create order returned no payment_session_id", created);
      throw new HttpsError("internal", "Could not start the payment. Please try again.");
    }
  }

  await orderRef.update({
    cashfreeOrderId: cfOrderId,
    updatedAt: Date.now(),
  });

  return { orderId, cashfreeOrderId: cfOrderId, paymentSessionId };
});

/**
 * Callable from the customer app after the Cashfree Android SDK's checkout
 * callback fires (success, failure, OR cancellation — all three call this,
 * since the SDK's own verdict is never trusted). Fetches the real payment
 * status directly from Cashfree and writes it to Firestore. Idempotent:
 * calling this repeatedly for the same order is always safe.
 */
exports.verifyCashfreePayment = onCall(CASHFREE_FUNCTION_CONFIG, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }

  const orderId = request.data && request.data.orderId;
  if (!orderId || typeof orderId !== "string") {
    throw new HttpsError("invalid-argument", "orderId is required.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const orderSnap = await orderRef.get();
  if (!orderSnap.exists) {
    throw new HttpsError("not-found", "Order not found.");
  }
  const order = orderSnap.data();

  if (order.userId !== request.auth.uid) {
    throw new HttpsError("permission-denied", "This is not your order.");
  }
  if (order.paymentMethod !== "ONLINE") {
    throw new HttpsError("failed-precondition", "This order is not an online-payment order.");
  }

  // Already settled from an earlier verify call or the webhook — nothing
  // left to check, and PAID must never be re-derived/overwritten.
  if (order.paymentStatus === "PAID") {
    return { paymentStatus: "PAID", cashfreePaymentId: order.cashfreePaymentId || null };
  }

  if (!order.cashfreeOrderId) {
    // Payment was never even started server-side for this order.
    return { paymentStatus: "PENDING", cashfreePaymentId: null };
  }

  const payments = await cashfreeRequest(
    "GET",
    `/orders/${encodeURIComponent(order.cashfreeOrderId)}/payments`
  ).catch((err) => {
    logger.error(`Could not fetch Cashfree payments for order ${orderId}`, err);
    throw new HttpsError("unavailable", "Could not reach the payment gateway. Please try again.");
  });

  const attempts = Array.isArray(payments) ? payments : [];
  // Prefer a successful attempt if one exists (there can only ever be one);
  // otherwise report the most recent attempt's outcome.
  const successAttempt = attempts.find((p) => p.payment_status === "SUCCESS");
  const latestAttempt = successAttempt ||
    attempts.slice().sort((a, b) => new Date(b.payment_time || 0) - new Date(a.payment_time || 0))[0];

  const paymentStatus = latestAttempt ? mapCashfreePaymentStatus(latestAttempt.payment_status) : "PENDING";
  const cashfreePaymentId = latestAttempt && latestAttempt.cf_payment_id != null
    ? String(latestAttempt.cf_payment_id)
    : (order.cashfreePaymentId || null);

  const paymentUpdate = {
    paymentStatus,
    cashfreePaymentId: cashfreePaymentId || "",
    updatedAt: Date.now(),
  };

  if (paymentStatus === "PAID") {
    paymentUpdate.status = "PENDING";
  }

  await orderRef.update(paymentUpdate);

  return { paymentStatus, cashfreePaymentId };
});

/**
 * Cashfree calls this directly (configure the URL in the Cashfree
 * dashboard's webhook settings) whenever a payment event happens — this is
 * the reliable fallback path in case the app is killed, loses network, or
 * the customer force-quits mid-payment before verifyCashfreePayment runs.
 * Every write here is guarded the same way as verifyCashfreePayment:
 * signature-verified, and never allowed to downgrade an already-PAID order.
 */

/**
 * Called by the customer app when the Cashfree checkout UI is abandoned
 * (Back/cancel). The client never deletes the Firestore order itself.
 *
 * IMPORTANT:
 * Before deleting anything, we ask Cashfree for the REAL payment state.
 * A successfully paid order is NEVER deleted, even if the Android callback
 * reported a cancellation/back action.
 */
exports.abandonCashfreeOrder = onCall(CASHFREE_FUNCTION_CONFIG, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "You must be signed in.");
  }

  const orderId = request.data && request.data.orderId;
  if (!orderId || typeof orderId !== "string") {
    throw new HttpsError("invalid-argument", "orderId is required.");
  }

  const orderRef = db.collection("orders").doc(orderId);
  const orderSnap = await orderRef.get();

  if (!orderSnap.exists) {
    return { abandoned: true, alreadyRemoved: true };
  }

  const order = orderSnap.data();

  if (order.userId !== request.auth.uid) {
    throw new HttpsError("permission-denied", "This is not your order.");
  }

  if (order.paymentMethod !== "ONLINE") {
    throw new HttpsError(
      "failed-precondition",
      "Only online-payment orders can be abandoned."
    );
  }

  // Never delete a successfully paid order.
  if (order.paymentStatus === "PAID") {
    return {
      abandoned: false,
      paymentStatus: "PAID",
      preserved: true
    };
  }

  /*
   * If Cashfree order creation never completed, there is nothing to verify
   * at Cashfree. This is safe to remove because the Firestore order is still
   * an unpaid temporary ONLINE order.
   */
  if (!order.cashfreeOrderId) {
    await orderRef.delete();

    return {
      abandoned: true,
      paymentStatus: "PENDING",
      cashfreeChecked: false
    };
  }

  let cashfreeOrder;

  try {
    cashfreeOrder = await cashfreeRequest(
      "GET",
      `/orders/${encodeURIComponent(order.cashfreeOrderId)}`
    );
  } catch (err) {
    logger.error(
      `Could not verify Cashfree order before abandoning ${orderId}`,
      err
    );

    // Do NOT delete if Cashfree cannot be reached. This prevents a real
    // payment from being lost because of a temporary gateway/network error.
    throw new HttpsError(
      "unavailable",
      "Could not confirm the payment status. Please try again."
    );
  }

  // Cashfree itself says the order is paid — preserve it.
  if (cashfreeOrder && cashfreeOrder.order_status === "PAID") {
    await orderRef.update({
      status: "PENDING",
      paymentStatus: "PAID",
      updatedAt: Date.now()
    });

    return {
      abandoned: false,
      paymentStatus: "PAID",
      preserved: true
    };
  }

  /*
   * Also inspect individual payment attempts. This protects against the
   * case where the Cashfree order status has not yet caught up but a
   * successful payment attempt already exists.
   */
  try {
    const payments = await cashfreeRequest(
      "GET",
      `/orders/${encodeURIComponent(order.cashfreeOrderId)}/payments`
    );

    const attempts = Array.isArray(payments) ? payments : [];

    const successfulAttempt = attempts.find(
      (payment) => payment.payment_status === "SUCCESS"
    );

    if (successfulAttempt) {
      const paymentId =
        successfulAttempt.cf_payment_id != null
          ? String(successfulAttempt.cf_payment_id)
          : (order.cashfreePaymentId || "");

      await orderRef.update({
        status: "PENDING",
        paymentStatus: "PAID",
        cashfreePaymentId: paymentId,
        updatedAt: Date.now()
      });

      return {
        abandoned: false,
        paymentStatus: "PAID",
        preserved: true,
        cashfreePaymentId: paymentId
      };
    }
  } catch (err) {
    logger.error(
      `Could not inspect Cashfree payment attempts before abandoning ${orderId}`,
      err
    );

    // Again: fail safe. Never delete when payment status cannot be confirmed.
    throw new HttpsError(
      "unavailable",
      "Could not confirm the payment status. Please try again."
    );
  }

  // No successful payment exists. This temporary order can now be removed.
  await orderRef.delete();

  return {
    abandoned: true,
    paymentStatus: "CANCELLED"
  };
});

exports.cashfreeWebhook = onRequest(CASHFREE_FUNCTION_CONFIG, async (req, res) => {
  try {
    if (req.method !== "POST") {
      res.status(405).send("Method not allowed");
      return;
    }

    const signature = req.header("x-webhook-signature");
    const timestamp = req.header("x-webhook-timestamp");
    if (!signature || !timestamp || !req.rawBody) {
      res.status(400).send("Missing signature");
      return;
    }

    const expectedSignature = crypto
      .createHmac("sha256", CASHFREE_SECRET_KEY.value())
      .update(timestamp + req.rawBody)
      .digest("base64");

    // Constant-time compare to avoid a timing side-channel on the signature check.
    const sigMatches = (() => {
      const a = Buffer.from(signature);
      const b = Buffer.from(expectedSignature);
      return a.length === b.length && crypto.timingSafeEqual(a, b);
    })();

    if (!sigMatches) {
      logger.warn("Cashfree webhook: signature mismatch");
      res.status(401).send("Invalid signature");
      return;
    }

    const payload = req.body || {};
    const data = payload.data || {};
    const cfOrder = data.order || {};
    const cfPayment = data.payment || {};
    const cfOrderId = cfOrder.order_id;

    if (!cfOrderId) {
      res.status(200).send("ignored: no order_id");
      return;
    }

    // We name Cashfree orders after our Firestore order id, with an
    // optional "_r<timestamp>" retry suffix — see createCashfreeOrder.
    const firestoreOrderId = String(cfOrderId).split("_r")[0];
    const orderRef = db.collection("orders").doc(firestoreOrderId);
    const snap = await orderRef.get();

    if (!snap.exists) {
      logger.warn(`Cashfree webhook: no order ${firestoreOrderId} for Cashfree order ${cfOrderId}`);
      res.status(200).send("ignored: unknown order");
      return;
    }

    const order = snap.data();
    if (order.paymentMethod !== "ONLINE") {
      res.status(200).send("ignored: not an online order");
      return;
    }
    if (order.paymentStatus === "PAID") {
      // Never let a later/duplicate event downgrade a settled order.
      res.status(200).send("ok: already settled");
      return;
    }

    const paymentStatus = mapCashfreePaymentStatus(cfPayment.payment_status || "");
    const cashfreePaymentId = cfPayment.cf_payment_id != null
      ? String(cfPayment.cf_payment_id)
      : (order.cashfreePaymentId || "");

    const paymentUpdate = {
      paymentStatus,
      cashfreePaymentId,
      cashfreeOrderId: order.cashfreeOrderId || String(cfOrderId),
      updatedAt: Date.now(),
    };

    if (paymentStatus === "PAID") {
      paymentUpdate.status = "PENDING";
    }

    await orderRef.update(paymentUpdate);

    res.status(200).send("ok");
  } catch (err) {
    logger.error("cashfreeWebhook error", err);
    res.status(500).send("error");
  }
});
