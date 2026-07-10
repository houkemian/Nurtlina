# Nurtlina: Baby Feeding Tracker

An Android baby feeding tracker and care logging app for overseas parents and caregivers.

## Product Overview

Nurtlina helps parents and caregivers:
- Quickly log formula feeds, breast milk feeds, diapers, and sleep with minimal taps
- View feeding history and daily summaries
- Receive calm reminders for next feeding time
- Track multiple babies (Pro)
- Export and backup data (Pro)

**This app is a tracking and reminder tool, not medical advice.**

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture (domain/data/ui) |
| Database | Room |
| Preferences | DataStore |
| Background | WorkManager + AlarmManager |
| DI | Hilt |
| Analytics | Firebase Analytics |
| Crash Reporting | Firebase Crashlytics |
| Billing | RevenueCat + Google Play products |
| Ads | AdMob |

---

## Project Structure

```
app/src/main/java/com/nurtlina/app/
├── core/
│   ├── analytics/      Firebase Analytics wrapper
│   ├── notification/   Notification scheduling, boot receiver
│   └── time/           Time formatting utilities
├── data/
│   ├── billing/        RevenueCat-backed entitlement manager
│   ├── datastore/      DataStore settings repository
│   ├── local/          Room entities, DAOs, database
│   └── repository/     Room repository implementations
├── domain/
│   ├── model/          Domain models (Baby, FeedLog, DiaperLog, SleepLog, etc.)
│   ├── repository/     Repository interfaces
│   └── usecase/        Business logic use cases
├── di/                 Hilt modules
└── ui/
    ├── feed/           Feeding log screens and ViewModel
    ├── insights/       Insights screen
    ├── logs/           Logs screen
    ├── navigation/     Nav host and routes
    ├── onboarding/     Onboarding flow
    ├── paywall/        Pro upgrade screen
    ├── settings/       Settings screen
    ├── theme/          Compose theme (colors, typography, shapes)
    └── today/          Today screen and ViewModel
```

---

## Feeding Guidelines Reference

The app references public guidelines for informational purposes only. These are NOT medical advice:

| Milk Type | General Reference (informational) |
|---|---|
| Formula, room temp | Generally use within 2 hours of preparation |
| Formula, after feeding starts | Generally use within 1 hour |
| Formula, refrigerated | Generally use within 24 hours |
| Breast milk, room temp | Generally use within 4 hours |
| Breast milk, refrigerated | Generally use within 4 days |

**Always follow your formula label, local health guidance, and your pediatrician's advice. When in doubt, discard the milk.**

---

## Compliance

This app does **not** provide medical advice.

Never add user-facing text that says:
- "safe to drink"
- "guaranteed safe"
- "medically approved"
- "doctor recommended"

Always use:
- "Based on public guidelines"
- "This app provides tracking and reminders only"
- "Not medical advice"
- "When in doubt, discard the milk"

---

## Setup

1. Replace `app/google-services.json` with your actual Firebase config
2. Replace AdMob App ID in `res/values/strings.xml` (currently set to test ID)
3. Set up Google Play products: `nurtlina_pro_monthly`, `nurtlina_pro_yearly`, `nurtlina_pro_lifetime`
4. Set up RevenueCat with entitlement `pro` and packages `$rc_monthly`, `$rc_annual`, `$rc_lifetime`
5. Provide the public Android RevenueCat SDK key via `~/.gradle/gradle.properties`: `REVENUECAT_API_KEY=goog_...`

Detailed RevenueCat dashboard setup: [`docs/revenuecat-setup.md`](docs/revenuecat-setup.md)

---

## Build

```bash
./gradlew assembleDebug
./gradlew test
./gradlew bundleRelease
```

---

## Monetization

- **Free**: 1 baby, basic feeding/diaper/sleep logs, ads, default widget
- **Pro** ($2.99/mo, $19.99/yr, $29.99 lifetime): No ads, multiple babies, full history, export, backup, custom reminders, widget themes

---

## Compliance Statement

> This app provides tracking tools based on public guidelines. It does not provide medical advice and cannot determine whether milk is safe. Always follow your formula label, local health guidance, and your pediatrician's advice. When in doubt, discard the milk.

Sources: CDC, AAP/HealthyChildren, NHS. Referenced for guideline durations only — no endorsement claimed.
