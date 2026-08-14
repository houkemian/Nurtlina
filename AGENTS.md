# AGENTS.md

## Project

Nurtlina: Baby Feeding Tracker

Android-only baby feeding tracker and care logging app for overseas parents and caregivers.

Core purpose:

- Track baby feeding records (formula, breast milk, mixed, nursing).
- Quick logging of feeds, diapers, and sleep with minimal taps.
- Provide calm reminders for next feeding time.
- Avoid medical claims.
- Support monetization through ads, Pro subscription, and lifetime purchase.

Primary platform:

- Android
- Google Play
- Kotlin
- Jetpack Compose

Primary business model:

- Free with ads.
- Pro unlocks no ads, multiple babies, full history, exports, backup, advanced stats, custom reminders, and widget themes.

## Current Implementation State (2026-07-15)

**Feeding record management is the primary path and is implemented:**

- `NewFeedSheet` (`ui/feed/NewFeedSheet.kt`) creates FeedLogs directly via `TodayViewModel.logFeed()` — see `NurtlinaNavHost.kt`
- `FeedingStatusCard` in `TodayScreen.kt` shows last feed info + next feed countdown
- `NextFeedNotificationScheduler` + `FeedingPatternAnalyzer` / `GenerateFeedingPredictionUseCase` handle personalized feed reminders
- `TodayViewModel.logFeed()` / `quickLogFeed()` are the main feeding record methods

**Bottle system is REMOVED (Phase 0 complete):**

The Bottle timer/state-machine system has been fully removed from the codebase (see `Nurtlina_PRD_Evaluation_and_Task_Plan.md` Phase 0, commit `0e42f39` + `ba00f03`). The only remaining traces are a legacy nullable `bottle_id` column on the backend `feed_logs` table and the Room `DROP TABLE IF EXISTS bottles` migration in `DatabaseModule.kt`.

**When modifying code:**
- DO NOT reintroduce Bottle features, the Bottle state machine, or `/sync/bottles`
- DO add/improve FeedLog-based feeding record features
- Prefer removing any remaining Bottle references over adding new ones

---

## Product Principles

Always optimize for:

1. Simplicity
   - Parents may use this app at night while tired.
   - Common actions should require as few taps as possible.
   - Prefer clear buttons over clever UI.
   - No complex state machines or timers — just log what happened.

2. Trust
   - Do not make safety guarantees.
   - Do not imply medical authority.
   - Always treat guidelines as informational, not prescriptive.

3. Speed
   - Logging a feeding must be fast (1-2 taps).
   - App startup should be quick.
   - Avoid unnecessary network dependency.

4. Local-first with backend from MVP
   - Core logging must work without internet.
   - MVP should directly include a lightweight backend for account identity, backup/sync foundation, entitlement verification, remote config, and future caregiver sharing.
   - Local Room data remains the source of truth for baby care records.
   - Backend sync must never block feeding log creation or reminders.
   - Network failures must degrade gracefully.
   - Sync should be eventual, not blocking.

5. Android-native quality
   - Use Material 3.
   - Support dark mode.
   - Support notification permission flows.
   - Support widgets.
   - Support large font sizes and accessibility.

---

## Compliance Rules

This app is a tracking and reminder tool, not a medical device and not medical advice.

Never write or generate user-facing text that says or implies:

- "safe to drink"
- "guaranteed safe"
- "medically approved"
- "doctor recommended"
- "CDC approved"
- "AAP approved"
- "NHS approved"
- "diagnosis"
- "health assessment"
- "your baby is healthy/unhealthy"
- "this milk is safe"
- "this milk is unsafe" unless referring to a general guideline and not a direct determination

Preferred wording:

- "based on public guidelines"
- "this app provides tracking and reminders only"
- "not medical advice"
- "follow your formula label, local guidance, and pediatrician's advice"
- "when in doubt, discard the milk"

Required disclaimer concept:

> This app provides tracking tools based on public guidelines. It does not provide medical advice and cannot determine whether milk is safe. Always follow your formula label, local health guidance, and your pediatrician's advice. When in doubt, discard the milk.

Do not claim endorsement by CDC, AAP, NHS, WHO, hospitals, pediatricians, or any health authority.

Guidelines may be referenced as public sources, but never as endorsement.

---

## Naming and Brand Rules

Current working brand:

- Nurtlina

Current Google Play display name:

- Nurtlina: Baby Feeding Tracker

Future expansion name:

- Nurtlina: Baby Tracker

Avoid brand names that imply medical safety or official endorsement.

Do not use:

- SafeBottle
- Formula Safety Checker
- CDC Formula Timer
- AAP Baby Tracker
- Doctor Baby Tracker
- Medical Baby Feeding Tracker

---

## Architecture

Use a simple clean architecture.

Suggested modules/packages:

```text
app
core
  design
  localization
  analytics
  notification
  time
data
  local
  repository
  billing
  ads
  backup
domain
  model
  usecase
  guideline
  reminder
ui
  onboarding
  today
  feed
  logs
  insights
  settings
  paywall
  widget
```

Preferred stack:

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- WorkManager
- AlarmManager only where justified
- Hilt
- Coroutines
- Flow
- Navigation Compose
- Google Play Billing or RevenueCat
- AdMob
- Firebase Analytics
- Firebase Crashlytics
- Firebase Remote Config for non-safety UI/paywall settings only
- Firebase Auth
- Firestore or Supabase/Postgres
- Cloud Functions or serverless functions where justified

MVP should include a lightweight backend, but avoid building a complex custom backend or operationally heavy infrastructure.

---

## Backend Rules

MVP includes a lightweight backend from the start.

Recommended MVP backend responsibilities:

- Anonymous or Google sign-in account identity.
- Optional email/Google login for backup and restore.
- Cloud backup and sync foundation.
- Purchase entitlement verification and restore support.
- Remote config for non-safety UI, paywall, and rollout settings.
- Support/contact metadata if needed.
- Future multi-caregiver sharing foundation.

Recommended backend options:

1. Firebase-first
   - Firebase Auth
   - Firestore
   - Firebase Storage if needed
   - Cloud Functions for Google Play Billing webhooks, entitlement checks, data cleanup, and safe backend jobs
   - Firebase Remote Config
   - Firebase Analytics / Crashlytics

2. Supabase-first
   - Supabase Auth
   - Postgres
   - Row Level Security
   - Edge Functions for entitlement checks
   - Storage if needed

Default recommendation for one-person Android MVP:

- Firebase-first, because it integrates naturally with Android, Google Play, Crashlytics, Analytics, Remote Config, and serverless functions.

Backend constraints:

- Do not make feeding log creation dependent on network availability.
- Persist all records locally.
- Sync should be eventual, not blocking.
- Use deterministic client-generated IDs for offline-created records.
- Implement conflict handling with `updatedAt`, `deletedAt`, `clientId`, and `schemaVersion`.
- Avoid real-time multi-device sync complexity in MVP unless explicitly requested.
- Do not store more personal data than necessary.
- Do not upload baby notes to analytics.
- Do not store medical diagnosis fields.
- Do not expose public unauthenticated read/write access.
- Use Firestore Security Rules or Supabase RLS from day one.
- All backend reads/writes must be scoped to the authenticated user/family.
- Add deletion/export paths before broad launch.
- Do not use synced baby care records for advertising targeting.
- Keep analytics separate from personal baby records.

Suggested cloud collections/tables:

```text
users/{userId}
families/{familyId}
families/{familyId}/members/{memberId}
families/{familyId}/babies/{babyId}
families/{familyId}/feedLogs/{feedLogId}
families/{familyId}/diaperLogs/{diaperLogId}
families/{familyId}/sleepLogs/{sleepLogId}
families/{familyId}/settings/{settingsId}
entitlements/{userId}
supportMessages/{messageId}
```

Minimum sync metadata on synced records:

```text
id
ownerUserId
familyId
createdAt
updatedAt
deletedAt
clientId
schemaVersion
```

Backend Definition of Done:

- Auth flow works.
- Offline creation still works.
- Local records sync after network returns.
- Security rules/RLS prevent cross-user access.
- User can disable backup/sync if implemented as optional.
- User can request or perform data deletion.
- Purchase entitlement restore does not rely only on local client state.
- Backend failure does not break feeding log flow.

---

## Coding Standards

Use Kotlin idioms:

- Immutable data classes where practical.
- Sealed interfaces/classes for states and actions.
- Explicit domain models.
- Coroutines and Flow for async streams.
- Repository interfaces for data access.
- Use cases for business logic.
- Avoid putting business logic inside Composables.

General rules:

- No hardcoded user-facing strings in Kotlin files.
- Put user-facing strings in localized resources.
- Do not hardcode magic values outside guideline/reference classes.
- Do not use floating-point values for timestamps.
- Store timestamps consistently.
- Prefer `Instant` for absolute time and convert only at UI boundaries.
- Keep unit conversion logic centralized.
- Keep notification IDs deterministic.
- Keep local IDs stable for sync.
- Keep sync logic out of UI components.

Compose rules:

- Composables should be stateless where possible.
- Use state hoisting.
- Keep previews for important screens.
- Support dark mode.
- Test large font sizes.
- Make tappable targets large enough for tired one-handed use.

---

## Domain Models

Important entities:

- Baby
- FeedLog (primary feeding record)
- DiaperLog
- SleepLog
- UserSettings
- UserAccount
- Family
- FamilyMember
- SyncState

Feed types:

- Formula
- BreastMilk
- Mixed
- Nursing
- Other

Diaper types:

- Wet
- Dirty
- Mixed
- Dry

---

## Notification Rules

Core reminders:

- Optional next feeding reminder based on user-configured interval.
- Calm, non-intrusive notification text.

Notification text rules:

- Do not use fear-based language.
- Include calm action guidance.
- Do not show disruptive ads immediately after reminder click.

Required behavior:

- When phone restarts, restore active reminders from local data.
- When notification permission is denied, app must still work and show a non-blocking explanation.
- Notification scheduling must not rely on the backend.

---

## UI Requirements

Primary tabs:

1. Today
2. Logs
3. Insights
4. Settings

Today screen must prioritize:

- Current baby selector.
- Last feeding status / next feeding reminder.
- Quick Feed (1-tap logging).
- Quick Diaper.
- Quick Sleep.
- Today summary.

Night mode:

- Large buttons.
- Low-stimulation colors.
- No full-screen ads.
- No complex forms.
- Common operations should be possible with one hand.

Accessibility:

- Support TalkBack labels.
- Support large font.
- Do not rely on color alone to communicate status.
- Provide text labels for all states.

---

## Monetization Rules

Free tier:

- 1 baby.
- Basic feeding, diaper, and sleep logs.
- Today summary.
- Ads.
- Default widget.
- Limited historical range if implemented.

Pro tier:

- No ads.
- Multiple babies.
- Full history.
- Advanced stats.
- Export.
- Backup.
- Custom reminders.
- Widget themes.

Suggested prices:

- Monthly: USD 2.99
- Yearly: USD 19.99
- Lifetime: USD 29.99

Ads:

- Do not show interstitial ads in MVP unless explicitly requested.
- Do not show disruptive ads during active feeding flow.
- Do not show disruptive ads in night mode.
- Pro users must see no ads.

---

## Analytics

Track product events without collecting unnecessary personal data.

Suggested events:

- onboarding_started
- baby_created
- feed_logged
- diaper_logged
- sleep_started
- sleep_ended
- widget_added
- paywall_viewed
- purchase_started
- purchase_completed
- purchase_failed
- export_clicked
- backup_enabled
- sync_enabled
- sync_failed
- language_changed

Do not log:

- Baby real names.
- Notes content.
- Detailed health concerns.
- User-entered sensitive medical information.

Encourage nicknames for babies.

---

## Privacy Rules

MVP should minimize data collection.

Do not request permissions for:

- Location
- Contacts
- Camera
- Microphone

Unless a future feature explicitly requires them and the user approves the scope.

Data should be local-first and synced to backend when the user enables backup/sync or signs in, depending on product decision.

For backend sync:

- Clearly explain what is uploaded.
- Provide deletion/export options.
- Encrypt data in transit.
- Be truthful in Google Play Data Safety disclosures.
- Do not use synced baby care records for advertising targeting.
- Keep analytics separate from personal baby records.
- Prefer nicknames over real names.

The app is for adult parents/caregivers, not for children directly.

---

## Localization

Initial languages:

- English
- Spanish
- Simplified Chinese
- German
- French

Rules:

- All user-facing strings must be localizable.
- Safety/disclaimer copy must preserve meaning across languages.
- Do not auto-change guideline region based only on language.
- Do not translate in a way that increases medical certainty.
- Prefer calm, parent-friendly wording.

---

## Testing Requirements

Must include unit tests for:

- Feed log CRUD operations.
- Feed type and amount validation.
- Unit conversion.
- Today summary.
- Cross-day logs.
- Sync metadata generation.
- Conflict handling for stale remote updates.
- Entitlement restore logic.

Backend and sync testing must include:

- Sign in / sign out.
- Anonymous user upgrade if supported.
- Offline record creation.
- Sync after network returns.
- Conflict handling between two devices if sync is enabled.
- Security rules/RLS access denial for other users' data.
- Account deletion or data deletion path.
- Entitlement restore from backend/client purchase state.

Manual testing must include:

- App restart.
- Device reboot.
- Notification permission denied.
- Notification permission later granted.
- Time zone change.
- Manual device time change.
- Dark mode.
- Large font.
- Low-end Android device.
- Subscription restore.
- Ad load failure.
- Backend unavailable.
- Slow network.
- Sign-in failure.
- Sync retry.

---

## AI Agent Behavior

When editing this project:

1. Read this file first.
2. Preserve compliance language.
3. Do not add medical claims.
4. Use the MVP backend only for clear product needs: account, backup/sync foundation, entitlement verification, remote config, and future caregiver sharing. Avoid backend complexity beyond that.
5. Prefer small, reviewable changes.
6. Explain non-obvious tradeoffs in comments or commit notes.
7. When adding user-facing text, add localization keys.
8. When changing feeding log logic, update tests.
9. When changing paywall or ad behavior, ensure night mode and reminder flows stay non-disruptive.
10. When changing backend/sync logic, verify offline behavior still works.
11. When changing backend data shape, update security rules/RLS and migration notes.

---

## Definition of Done

A task is done only when:

- Code compiles.
- Relevant tests pass.
- User-facing strings are localized.
- Accessibility is not worsened.
- Compliance language is preserved.
- No medical/safety guarantee is introduced.
- Analytics do not collect unnecessary sensitive data.
- Existing feeding log behavior is not broken.
- UI works in light and dark mode.
- Backend failure does not break core local logging flows.
- Security rules/RLS are updated for new backend data.
- Sync changes include offline/retry behavior.

---

## Non-Goals for MVP

Do not build these unless explicitly requested:

- AI doctor
- Medical diagnosis
- Community/social feed
- Pediatric consultation
- Baby photo analysis
- Growth abnormality detection
- Vaccine schedule advice
- Bottle freshness timer / state machine (REMOVED — replaced by simple feeding records)
- Real-time family sharing in MVP unless explicitly requested
- Complex custom account system beyond lightweight Auth
- Hardware integration
- Nutrition recommendations
- Brand-specific formula recommendations

---

## Preferred User-Facing Copy

Use these patterns:

- "Log feeding"
- "Record feeding"
- "Quick feed — 120 ml"
- "Last feeding: 25 min ago"
- "Based on public guidelines"
- "This app is a tracking tool, not medical advice."
- "When in doubt, discard the milk."

Avoid:

- "Safe"
- "Unsafe"
- "Approved"
- "Certified"
- "Medical"
- "Doctor"
- "Diagnosis"
- "Health assessment"
