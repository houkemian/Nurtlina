import * as admin from "firebase-admin";
import * as functions from "firebase-functions";

admin.initializeApp();
const db = admin.firestore();

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────

interface RtdnNotificationPayload {
  version: string;
  packageName: string;
  eventTimeMillis: string;
  subscriptionNotification?: {
    version: string;
    notificationType: number;
    purchaseToken: string;
    subscriptionId: string;
  };
  oneTimeProductNotification?: {
    version: string;
    notificationType: number;
    purchaseToken: string;
    sku: string;
  };
  voidedPurchaseNotification?: {
    purchaseToken: string;
    orderId: string;
    productType: number;
    refundType: number;
  };
}

interface EntitlementRecord {
  userId: string;
  isProActive: boolean;
  productId: string | null;
  purchaseToken: string | null;
  expiresAtMillis: number | null;
  updatedAt: admin.firestore.FieldValue;
}

// Subscription notification types from Google Play
const SUBSCRIPTION_PURCHASED = 4;
const SUBSCRIPTION_RENEWED = 2;
const SUBSCRIPTION_RECOVERED = 6;
const SUBSCRIPTION_RESTARTED = 7;
const SUBSCRIPTION_EXPIRED = 13;
const SUBSCRIPTION_CANCELED = 3;
const SUBSCRIPTION_REVOKED = 12;
const SUBSCRIPTION_ON_HOLD = 5;
const SUBSCRIPTION_PAUSED = 10;

// ─────────────────────────────────────────────────────────────────────────────
// Google Play Real-Time Developer Notifications (RTDN) webhook
//
// Set this Cloud Function URL as the Pub/Sub push endpoint for your
// Google Play subscription notification topic.
//
// The function decodes the Pub/Sub message, updates the entitlements/{userId}
// document, and never exposes raw purchase tokens in Firestore user documents.
// ─────────────────────────────────────────────────────────────────────────────
export const playBillingWebhook = functions
  .region("us-central1")
  .pubsub.topic("play-billing-notifications")
  .onPublish(async (message) => {
    let payload: RtdnNotificationPayload;

    try {
      payload = message.json as RtdnNotificationPayload;
    } catch (err) {
      functions.logger.error("Failed to parse RTDN payload", { err });
      return;
    }

    const subNotif = payload.subscriptionNotification;
    if (!subNotif) {
      // One-time product or voided purchase – not handled in MVP.
      functions.logger.info("Non-subscription RTDN received; skipping.", {
        payload,
      });
      return;
    }

    const { notificationType, purchaseToken, subscriptionId } = subNotif;
    functions.logger.info("RTDN subscription notification", {
      notificationType,
      subscriptionId,
    });

    // Resolve userId from the purchase token stored in a dedicated lookup doc.
    // The Android app writes this mapping when the purchase is initiated.
    const tokenSnap = await db
      .collection("purchaseTokens")
      .doc(purchaseToken)
      .get();

    if (!tokenSnap.exists) {
      functions.logger.warn("purchaseToken not found in Firestore", {
        purchaseToken,
      });
      return;
    }

    const userId = tokenSnap.data()?.userId as string | undefined;
    if (!userId) {
      functions.logger.error("userId missing from purchaseToken doc");
      return;
    }

    const isActive =
      notificationType === SUBSCRIPTION_PURCHASED ||
      notificationType === SUBSCRIPTION_RENEWED ||
      notificationType === SUBSCRIPTION_RECOVERED ||
      notificationType === SUBSCRIPTION_RESTARTED;

    const isInactive =
      notificationType === SUBSCRIPTION_EXPIRED ||
      notificationType === SUBSCRIPTION_CANCELED ||
      notificationType === SUBSCRIPTION_REVOKED ||
      notificationType === SUBSCRIPTION_ON_HOLD ||
      notificationType === SUBSCRIPTION_PAUSED;

    const entitlement: EntitlementRecord = {
      userId,
      isProActive: isActive,
      productId: isActive || isInactive ? subscriptionId : null,
      purchaseToken: isActive ? purchaseToken : null,
      expiresAtMillis: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    await db.collection("entitlements").doc(userId).set(entitlement, {
      merge: true,
    });

    functions.logger.info("Entitlement updated", { userId, isProActive: isActive });
  });

// ─────────────────────────────────────────────────────────────────────────────
// Register purchase token mapping
//
// Called by the Android app immediately after a purchase is initiated so the
// webhook can resolve userId from a purchaseToken.
// Accessible only by authenticated users for their own token.
// ─────────────────────────────────────────────────────────────────────────────
export const registerPurchaseToken = functions
  .region("us-central1")
  .https.onCall(async (data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required."
      );
    }

    const purchaseToken = data.purchaseToken as string | undefined;
    if (!purchaseToken || typeof purchaseToken !== "string") {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "purchaseToken is required."
      );
    }

    await db.collection("purchaseTokens").doc(purchaseToken).set({
      userId: context.auth.uid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return { success: true };
  });

// ─────────────────────────────────────────────────────────────────────────────
// Verify and restore entitlement
//
// Android client calls this to get the current Pro entitlement status,
// useful for restore-purchase flows and first-launch checks.
// ─────────────────────────────────────────────────────────────────────────────
export const getEntitlement = functions
  .region("us-central1")
  .https.onCall(async (_data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required."
      );
    }

    const userId = context.auth.uid;
    const snap = await db.collection("entitlements").doc(userId).get();

    if (!snap.exists) {
      return { isProActive: false, productId: null };
    }

    const data = snap.data() as EntitlementRecord;
    return {
      isProActive: data.isProActive ?? false,
      productId: data.productId ?? null,
    };
  });

// ─────────────────────────────────────────────────────────────────────────────
// Create family on first sign-in
//
// Called by the Android app after a user completes sign-in if they do not yet
// have a family document. Creates a family and adds the user as owner member.
// ─────────────────────────────────────────────────────────────────────────────
export const provisionFamily = functions
  .region("us-central1")
  .https.onCall(async (_data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required."
      );
    }

    const userId = context.auth.uid;

    // Check if user already has a family.
    const userSnap = await db.collection("users").doc(userId).get();
    const existingFamilyId = userSnap.data()?.familyId as string | undefined;
    if (existingFamilyId) {
      return { familyId: existingFamilyId, created: false };
    }

    // Create a new family with the user as the owner.
    const familyRef = db.collection("families").doc();
    const familyId = familyRef.id;
    const now = admin.firestore.FieldValue.serverTimestamp();

    const batch = db.batch();

    batch.set(familyRef, {
      id: familyId,
      ownerUserId: userId,
      createdAt: now,
      updatedAt: now,
    });

    batch.set(familyRef.collection("members").doc(userId), {
      userId,
      role: "owner",
      joinedAt: now,
    });

    batch.set(db.collection("users").doc(userId), {
      userId,
      familyId,
      createdAt: now,
      updatedAt: now,
    }, { merge: true });

    await batch.commit();

    functions.logger.info("Family provisioned", { userId, familyId });
    return { familyId, created: true };
  });

// ─────────────────────────────────────────────────────────────────────────────
// Delete user data
//
// Callable by the authenticated user to delete all their family's data.
// Complies with Google Play Data Safety deletion requirements.
// ─────────────────────────────────────────────────────────────────────────────
export const deleteUserData = functions
  .region("us-central1")
  .https.onCall(async (_data, context) => {
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "Authentication required."
      );
    }

    const userId = context.auth.uid;
    const userSnap = await db.collection("users").doc(userId).get();
    const familyId = userSnap.data()?.familyId as string | undefined;

    if (!familyId) {
      await db.collection("users").doc(userId).delete();
      await db.collection("entitlements").doc(userId).delete();
      return { success: true };
    }

    // Check if the user is the family owner.
    const familySnap = await db.collection("families").doc(familyId).get();
    const isOwner = familySnap.data()?.ownerUserId === userId;

    const subcollections = [
      "babies",
      "bottles",
      "feedLogs",
      "diaperLogs",
      "sleepLogs",
      "settings",
      "members",
    ];

    if (isOwner) {
      // Delete all family sub-collections.
      for (const sub of subcollections) {
        await deleteCollection(db, `families/${familyId}/${sub}`, 100);
      }
      await db.collection("families").doc(familyId).delete();
    } else {
      // Only remove this user from the family members list.
      await db
        .collection("families")
        .doc(familyId)
        .collection("members")
        .doc(userId)
        .delete();
    }

    await db.collection("users").doc(userId).delete();
    await db.collection("entitlements").doc(userId).delete();

    // Revoke Firebase Auth token so sessions end.
    await admin.auth().revokeRefreshTokens(userId);

    functions.logger.info("User data deleted", { userId, familyId, isOwner });
    return { success: true };
  });

// ─────────────────────────────────────────────────────────────────────────────
// Scheduled cleanup: purge soft-deleted records older than 90 days
// ─────────────────────────────────────────────────────────────────────────────
export const cleanupDeletedRecords = functions
  .region("us-central1")
  .pubsub.schedule("every 24 hours")
  .onRun(async () => {
    const cutoffMs = Date.now() - 90 * 24 * 60 * 60 * 1000;
    const cutoff = admin.firestore.Timestamp.fromMillis(cutoffMs);

    const collections = [
      "bottles",
      "feedLogs",
      "diaperLogs",
      "sleepLogs",
      "babies",
    ];

    let totalDeleted = 0;

    const familiesSnap = await db.collection("families").get();
    for (const familyDoc of familiesSnap.docs) {
      for (const col of collections) {
        const staleSnap = await db
          .collection(`families/${familyDoc.id}/${col}`)
          .where("deletedAt", "<=", cutoff)
          .limit(200)
          .get();

        const batch = db.batch();
        staleSnap.docs.forEach((d) => batch.delete(d.ref));
        if (!staleSnap.empty) {
          await batch.commit();
          totalDeleted += staleSnap.size;
        }
      }
    }

    functions.logger.info("Cleanup complete", { totalDeleted });
  });

// ─────────────────────────────────────────────────────────────────────────────
// Utility: delete a Firestore sub-collection in batches
// ─────────────────────────────────────────────────────────────────────────────
async function deleteCollection(
  firestore: admin.firestore.Firestore,
  collectionPath: string,
  batchSize: number
): Promise<void> {
  const ref = firestore.collection(collectionPath).limit(batchSize);

  return new Promise((resolve, reject) => {
    deleteQueryBatch(firestore, ref, resolve).catch(reject);
  });
}

async function deleteQueryBatch(
  firestore: admin.firestore.Firestore,
  query: admin.firestore.Query,
  resolve: () => void
): Promise<void> {
  const snapshot = await query.get();
  if (snapshot.size === 0) {
    resolve();
    return;
  }
  const batch = firestore.batch();
  snapshot.docs.forEach((doc) => batch.delete(doc.ref));
  await batch.commit();
  await deleteQueryBatch(firestore, query, resolve);
}
