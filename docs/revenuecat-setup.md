# RevenueCat Setup Guide

This document describes the RevenueCat dashboard configuration required by Nurtlina's Android app.

RevenueCat dashboard labels can change over time. If the left navigation names differ, use the closest equivalent under the same project/app/product catalog area.

## App-side contract

The Android code expects these identifiers:

| Type | Identifier |
|---|---|
| RevenueCat entitlement | `pro` |
| Monthly Google Play product | `nurtlina_pro_monthly` |
| Yearly Google Play product | `nurtlina_pro_yearly` |
| Lifetime Google Play product | `nurtlina_pro_lifetime` |
| Monthly RevenueCat package | `$rc_monthly` |
| Yearly RevenueCat package | `$rc_annual` |
| Lifetime RevenueCat package | `$rc_lifetime` |

Relevant app files:

- `frontend/app/src/main/java/com/nurtlina/app/data/billing/EntitlementManager.kt`
- `frontend/app/build.gradle.kts`

## 1. Create or select the RevenueCat project

Path:

`RevenueCat Dashboard` -> project switcher -> `New Project` or select the Nurtlina project

Recommended project name:

`Nurtlina`

Keep one RevenueCat project for the production Android app. Use a separate project only if you intentionally want isolated sandbox/test configuration.

## 2. Add the Android app

Path:

`Project Settings` -> `Apps` -> `New app` -> `Google Play / Android`

Use:

| Field | Value |
|---|---|
| App name | `Nurtlina Android` |
| Package name | `com.nurtlina.app` |
| Platform | Google Play / Android |

Notes:

- The debug build uses `com.nurtlina.app.debug` because `applicationIdSuffix = ".debug"`. RevenueCat purchases should be validated against the production package, so test purchase flows should normally use a build/package that matches the Play Console app setup.
- Do not put medical or safety claims in RevenueCat product descriptions.

## 3. Connect Google Play service credentials

Path:

`Project Settings` -> `Apps` -> select `Nurtlina Android` -> `Google Play` / `Service credentials`

Configure the Google Play service account JSON or linked credentials following RevenueCat's Google Play service credentials guide.

Checklist:

- Google Play Console app exists for package `com.nurtlina.app`.
- RevenueCat has access to read purchases/subscriptions for that Play app.
- Play products listed below are created and active enough for testing.
- Play license testers are configured before running sandbox purchases.

## 4. Import or create products

Path:

`Product Catalog` -> `Products` -> `New` or `Import from Google Play`

Create/import these products:

| RevenueCat product identifier | Store type | Google Play product ID | Notes |
|---|---|---|---|
| `nurtlina_pro_monthly` | Subscription | `nurtlina_pro_monthly` | Monthly Pro, suggested price USD 2.99 |
| `nurtlina_pro_yearly` | Subscription | `nurtlina_pro_yearly` | Yearly Pro, suggested price USD 19.99 |
| `nurtlina_pro_lifetime` | One-time / In-app product | `nurtlina_pro_lifetime` | Lifetime Pro, suggested price USD 29.99 |

For subscriptions in Google Play Console, ensure base plans/offers are configured and available to RevenueCat. The app falls back to product ID matching if RevenueCat returns Google product IDs with a base-plan suffix like `product_id:base_plan_id`.

## 5. Create the Pro entitlement

Path:

`Product Catalog` -> `Entitlements` -> `New`

Use:

| Field | Value |
|---|---|
| Identifier | `pro` |
| Display name | `Pro` |
| Description | `Unlocks Nurtlina Pro features.` |

Attach all three products to this entitlement:

- `nurtlina_pro_monthly`
- `nurtlina_pro_yearly`
- `nurtlina_pro_lifetime`

Why this matters:

The app reads `CustomerInfo.entitlements.active["pro"]`. If the products are not attached to the `pro` entitlement, a purchase can complete but the app will stay in Free mode.

## 6. Create the default offering

Path:

`Product Catalog` -> `Offerings` -> `New Offering`

Recommended:

| Field | Value |
|---|---|
| Offering identifier | `default` |
| Description | `Default Nurtlina Pro paywall offering` |

Mark this offering as the current/default offering.

## 7. Add packages to the offering

Path:

`Product Catalog` -> `Offerings` -> select `default` -> `Packages` -> `Add package`

Add exactly these packages:

| Package type | Package identifier expected by SDK | Product |
|---|---|---|
| Monthly | `$rc_monthly` | `nurtlina_pro_monthly` |
| Annual | `$rc_annual` | `nurtlina_pro_yearly` |
| Lifetime | `$rc_lifetime` | `nurtlina_pro_lifetime` |

The app first looks for these RevenueCat package identifiers. If one is missing, it tries to find the package by product ID, but the package identifiers above should still be configured to keep behavior predictable.

## 8. Copy the public Android SDK key

Path:

`Project Settings` -> `API keys` or `Project Settings` -> `Apps` -> select Android app -> `SDK key`

Copy the public Android key, usually starting with `goog_`.

Do not commit the key into the repository. Add it locally:

```properties
# ~/.gradle/gradle.properties
REVENUECAT_API_KEY=goog_your_public_android_sdk_key
```

The Gradle build passes this value to `BuildConfig.REVENUECAT_API_KEY`. If the value is blank, the app keeps running in Free mode and logs that RevenueCat is not configured.

## 9. Test purchase and restore flows

Before testing:

- Install a build whose package name matches the Google Play app setup.
- Use a Google Play license tester account.
- Confirm the app can reach RevenueCat and Google Play.

Manual test checklist:

1. Open the paywall.
2. Purchase monthly Pro.
3. Confirm Pro features unlock and ads are hidden where applicable.
4. Reinstall or clear app data.
5. Sign in with the same Firebase/Google identity if testing account-linked restore.
6. Tap restore purchases.
7. Confirm Pro status returns.
8. Repeat with yearly and lifetime in sandbox before release.

## 10. Release checklist

- RevenueCat `pro` entitlement has all products attached.
- Current offering includes `$rc_monthly`, `$rc_annual`, and `$rc_lifetime`.
- Google Play products are active for the release track.
- RevenueCat Google Play service credentials show no setup errors.
- `REVENUECAT_API_KEY` is configured in the build environment.
- No RevenueCat product/paywall copy makes medical, safety, CDC/AAP/NHS endorsement, or guarantee claims.

## Official references

- RevenueCat Entitlements: https://www.revenuecat.com/docs/getting-started/entitlements
- RevenueCat Offerings: https://www.revenuecat.com/docs/offerings/overview
- RevenueCat Products: https://www.revenuecat.com/docs/getting-started/products
- RevenueCat Google Play service credentials: https://www.revenuecat.com/docs/service-credentials/creating-play-service-credentials
