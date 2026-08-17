# Release Configuration Guide: AdMob & Google Play

Operational runbook for the two remaining release-blocking config steps. Follow these
before submitting Nurtlina to Play Console for closed testing.

> Related: [`docs/revenuecat-setup.md`](revenuecat-setup.md) covers the RevenueCat
> dashboard side of the same products. This guide covers the **AdMob** and
> **Google Play Console** sides only.

---

## 1. Replace the AdMob test IDs with real IDs

The app currently ships Google's official test identifiers. Both must be replaced
before release, otherwise no real ads will serve and AdMob policy will reject the app.

### Where the IDs live

| Location | Current value | What it is |
|---|---|---|
| `frontend/app/src/main/res/values/strings.xml` → `admob_app_id` | `ca-app-pub-3940256099942544~3347511713` | AdMob **App ID** |
| `frontend/app/src/main/java/com/nurtlina/app/ui/today/TodayScreen.kt` → `TodayScreen(adUnitId = ...)` default | `ca-app-pub-3940256099942544/6300978111` | **Banner ad unit ID** |

### Steps

1. Go to [apps.admob.com](https://apps.admob.com) and sign in with the Google account that
   owns the Play Console listing.
2. Create or select the Nurtlina app.
3. **Create the app entry**
   - `Apps` → `Add app` → pick "Android" → choose "No" (not listed on a store yet, or link the
     existing Play app if it already exists).
   - Package name: `com.nurtlina.app`.
4. **Copy the App ID**
   - `App settings` → `App ID` (format `ca-app-pub-<publisher>~<app>`).
   - Replace `admob_app_id` in `values/strings.xml` with this value.
5. **Create a banner ad unit**
   - `Ad units` → `Add ad unit` → `Banner`.
   - Give it a name such as `Nurtlina Today Banner`.
   - Copy the **Ad unit ID** (format `ca-app-pub-<publisher>/<unit>`).
   - Replace the default `adUnitId` value in `TodayScreen.kt`.
6. **Optional but recommended:** enable the sensitive-category filter for baby/family content:
   - `Blocking controls` → `Ad content rating` → keep "General audiences", and enable the
     sensitive categories block for infant/health-adjacent categories as appropriate.

Do **not** show interstitial/rewarded ads (product decision for MVP); the app only uses the
Today banner, and it is already gated behind `showAds = !isPro && !nightModeEnabled`.

---

## 2. Create the three Google Play products

The app and RevenueCat both expect these product IDs:

| Product ID | Type | Suggested price |
|---|---|---|
| `nurtlina_pro_monthly` | Subscription | USD 2.99 / month |
| `nurtlina_pro_yearly` | Subscription | USD 19.99 / year |
| `nurtlina_pro_lifetime` | One-time (in-app product) | USD 29.99 |

### Steps

1. Open [Google Play Console](https://play.google.com/console) and select the Nurtlina app
   (package `com.nurtlina.app`).
2. **Lifetime (one-time product)**
   - `Monetize` → `Products` → `In-app products` → `Create product`.
   - Product ID: `nurtlina_pro_lifetime`; name e.g. "Nurtlina Pro (Lifetime)"; set the price.
   - Activate it.
3. **Monthly subscription**
   - `Monetize` → `Products` → `Subscriptions` → `Create subscription`.
   - Product ID: `nurtlina_pro_monthly`.
   - Add a **base plan** (auto-renewing) priced at USD 2.99/month; set a billing period of 1 month.
4. **Yearly subscription**
   - Product ID: `nurtlina_pro_yearly`.
   - Base plan priced at USD 19.99/year; billing period 1 year.
5. **License testers (before any sandbox purchase)**
   - `Setup` → `License testing` → `Add testers`, and add the Google account(s) used to test.
6. **Verify RevenueCat can read them**
   - In RevenueCat: `Product Catalog` → `Products` → `Import from Google Play`, or create the
     products with the exact IDs above, then attach them to the `pro` entitlement and the
     default offering packages (`$rc_monthly`, `$rc_annual`, `$rc_lifetime`).
   - Full dashboard steps: see [`docs/revenuecat-setup.md`](revenuecat-setup.md).

> Copy rules: product/paywall text must stay free of medical/safety claims and never imply
> CDC/AAP/NHS endorsement. See `AGENTS.md` compliance rules.

---

## 3. Pre-submit checklist

- [ ] Real AdMob App ID + banner unit ID in place (no `3940256099942544` anywhere).
- [ ] Three Play products created and **active**.
- [ ] RevenueCat `pro` entitlement attaches all three products; offering has the three packages.
- [ ] `REVENUECAT_API_KEY` configured via `~/.gradle/gradle.properties` (not committed).
- [ ] `frontend/app/google-services.json` points at the production Firebase project.
- [ ] Privacy Policy (`https://nurtlina.app/privacy`) and Terms (`https://nurtlina.app/terms`)
      pages exist and are reachable.
- [ ] App icon, feature graphic, and screenshots are uploaded.
- [ ] Data Safety form filled (the app collects analytics, crash logs, account email on sign-in,
      and optional cloud-synced baby records; offers in-app account deletion — see Settings → Delete account & data).
