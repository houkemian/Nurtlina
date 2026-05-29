# Nurtlina: Baby Feeding Timer

An Android baby bottle timer and care tracking app for overseas parents and caregivers.

## Product Overview

Nurtlina helps parents and caregivers:
- Track baby bottle freshness with timers based on CDC/AAP/NHS public guidelines
- Distinguish bottle states: Not Started → Feeding Started → Refrigerated → Expired/Fed/Discarded
- Log formula feeds, breast milk feeds, diapers, and sleep
- Receive calm reminders before bottles expire
- View daily summaries and trends (Pro)

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
| Billing | Google Play Billing |
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
│   ├── billing/        Entitlement manager (Play Billing)
│   ├── datastore/      DataStore settings repository
│   ├── local/          Room entities, DAOs, database
│   └── repository/     Room repository implementations
├── domain/
│   ├── guideline/      Bottle timer rules, state machine, expiry calculator
│   ├── model/          Domain models (Baby, Bottle, FeedLog, etc.)
│   ├── repository/     Repository interfaces
│   └── usecase/        Business logic use cases
├── di/                 Hilt modules
└── ui/
    ├── bottle/         Bottle screens and ViewModel
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

## Bottle Timer Rules

MVP defaults (based on CDC/AAP):

| State | Rule |
|---|---|
| Formula, not started, room temp | Expires in 2 hours from preparedAt |
| Formula, feeding started | Expires in 1 hour from feedingStartedAt |
| Formula, refrigerated | Expires in 24 hours from preparedAt |
| Breast milk, room temp | Expires in 4 hours from preparedAt |
| Breast milk, refrigerated | Expires in 4 days from preparedAt |

Rules are versioned in `DefaultGuidelineRules.kt`. Do not change durations without:
1. Incrementing `RULE_VERSION`
2. Updating tests
3. Adding migration notes

---

## Compliance

This app does **not** provide medical advice.

Never add user-facing text that says:
- "safe to drink"
- "guaranteed safe"
- "medically approved"
- "doctor recommended"

Always use:
- "Based on your selected guideline"
- "This app provides reminders and tracking only"
- "Not medical advice"
- "When in doubt, discard the milk"

---

## Setup

1. Replace `app/google-services.json` with your actual Firebase config
2. Replace AdMob App ID in `res/values/strings.xml` (currently set to test ID)
3. Set up Google Play products: `nurtlina_pro_monthly`, `nurtlina_pro_yearly`, `nurtlina_pro_lifetime`

---

## Build

```bash
./gradlew assembleDebug
./gradlew test
./gradlew bundleRelease
```

---

## Monetization

- **Free**: 1 baby, basic bottle timer, basic logs, ads, default widget
- **Pro** ($2.99/mo, $19.99/yr, $29.99 lifetime): No ads, multiple babies, full history, export, backup, custom reminders, widget themes

---

## Compliance Statement

> This app provides reminders and tracking tools based on selected public guidelines. It does not provide medical advice and cannot determine whether milk is safe. Always follow your formula label, local health guidance, and your pediatrician's advice. When in doubt, discard the milk.

Sources: CDC, AAP/HealthyChildren, NHS. Referenced for guideline durations only — no endorsement claimed.
