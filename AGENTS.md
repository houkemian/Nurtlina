# AGENTS.md

## Project

Nurtlina: Baby Feeding Timer

Android-only baby bottle timer and baby care tracking app for overseas parents and caregivers.

Core purpose:

- Track baby bottle freshness timers.
- Distinguish between bottles that have not been started, have been used, are refrigerated, expired, fed, discarded, or canceled.
- Track formula feeding, breast milk feeding, diapers, and sleep.
- Provide calm reminders and logs.
- Avoid medical claims.
- Support monetization through ads, Pro subscription, and lifetime purchase.

Primary platform:

- Android
- Google Play
- Kotlin
- Jetpack Compose

Primary business model:

- Free with ads.
- Pro unlocks no ads, multiple babies, full history, exports, backup, advanced stats, custom reminders, custom rules, and widget themes.

---

## Product Principles

Always optimize for:

1. Simplicity
   - Parents may use this app at night while tired.
   - Common actions should require as few taps as possible.
   - Prefer clear buttons over clever UI.

2. Trust
   - Do not make safety guarantees.
   - Do not imply medical authority.
   - Always treat timers as reminders based on selected public guidance.

3. Speed
   - The main bottle timer flow must be fast.
   - App startup should be quick.
   - Avoid unnecessary network dependency.

4. Local-first with backend from MVP
   - Core timer and logs must work without internet.
   - MVP should directly include a lightweight backend for account identity, backup/sync foundation, entitlement verification, remote config, and future caregiver sharing.
   - Local Room data remains the source of truth for active timers.
   - Backend sync must never block bottle timer creation, state changes, or reminders.
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

- "timer expired"
- "based on your selected guideline"
- "this app provides reminders and tracking only"
- "not medical advice"
- "follow your formula label, local guidance, and pediatrician's advice"
- "when in doubt, discard the milk"

Required disclaimer concept:

> This app provides reminders and tracking tools based on selected public guidelines. It does not provide medical advice and cannot determine whether milk is safe. Always follow your formula label, local health guidance, and your pediatrician's advice. When in doubt, discard the milk.

Do not claim endorsement by CDC, AAP, NHS, WHO, hospitals, pediatricians, or any health authority.

Guidelines may be referenced as public sources, but never as endorsement.

---

## Guideline Source Handling

Public sources used for app copy, FAQ, and timer rule explanations:

- CDC: Infant Formula Preparation and Storage
- CDC: Breast Milk Storage and Preparation
- NHS: Formula milk common questions
- NHS: How to make up baby formula
- AAP / HealthyChildren: safe formula preparation guidance

Rules must be versioned in code.

Do not silently change timer durations without:

- Updating rule version.
- Updating source text if needed.
- Adding migration notes if behavior changes existing active timers.
- Adding tests.

Default formula timer rules for MVP:

- Prepared formula, not started, room temperature: 2 hours from prepared time.
- Formula after feeding starts: 1 hour from feeding start.
- Prepared formula refrigerated before feeding starts: 24 hours from prepared time.

Default breast milk timer rules for MVP:

- Fresh expressed breast milk at room temperature: 4 hours from expressed/prepared time.
- Refrigerated fresh breast milk: 4 days from expressed/prepared time.

If there is uncertainty between sources or regions, show cautious wording and allow region/custom settings rather than hard medical claims.

---

## Naming and Brand Rules

Current working brand:

- Nurtlina

Current Google Play display name:

- Nurtlina: Baby Feeding Timer

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
  bottle
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

- Do not make bottle timer usage dependent on network availability.
- Do not calculate active countdowns only on the server.
- Persist active timers locally.
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
- Safety timer rules must be versioned and should not be silently changed by Remote Config.

Suggested cloud collections/tables:

```text
users/{userId}
families/{familyId}
families/{familyId}/members/{memberId}
families/{familyId}/babies/{babyId}
families/{familyId}/bottles/{bottleId}
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
- Backend failure does not break bottle timer flow.

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
- Do not hardcode magic timer values outside guideline/rule classes.
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
- Bottle
- FeedLog
- DiaperLog
- SleepLog
- UserSettings
- GuidelineRule
- SubscriptionEntitlement
- UserAccount
- Family
- FamilyMember
- SyncState

Bottle statuses:

- NotStarted
- FeedingStarted
- Refrigerated
- Expired
- Fed
- Discarded
- Canceled

Milk types:

- Formula
- BreastMilk
- Custom

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

Guideline regions:

- US
- UK
- Custom

---

## Bottle Timer State Machine

Allowed transitions:

```text
NotStarted -> FeedingStarted
NotStarted -> Refrigerated
NotStarted -> Discarded
NotStarted -> Canceled
NotStarted -> Expired

Refrigerated -> FeedingStarted
Refrigerated -> Discarded
Refrigerated -> Expired

FeedingStarted -> Fed
FeedingStarted -> Discarded
FeedingStarted -> Expired

Expired -> Discarded

Fed -> terminal
Discarded -> terminal
Canceled -> terminal
```

Rules:

- `FeedingStarted` should not silently return to `NotStarted`.
- `Fed`, `Discarded`, and `Canceled` cancel all scheduled bottle notifications.
- `Expired` should not be marked as `Fed` without explicit user confirmation such as "record anyway".
- Editing times must recalculate expiry.
- Active timers must survive app restart.
- Pending notifications must be rescheduled after device reboot.
- UI countdown must be derived from persisted `expiresAt - now`, not from in-memory timers.
- Remote sync must not override a newer local timer state with stale cloud data.

---

## Notification Rules

Core reminders:

- Before expiry, default 15 minutes before.
- At expiry.
- Optional feeding-started reminder around 45 minutes.
- At feeding-started expiry around 60 minutes.

Notification text rules:

- Say "timer expired", not "milk is unsafe".
- Include calm action guidance.
- Do not use fear-based language.
- Do not show disruptive ads immediately after reminder click.

Required behavior:

- When bottle status changes, cancel previous notifications and schedule new ones.
- When bottle is terminal, cancel notifications.
- When phone restarts, restore active timer notifications from local data.
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
- Active bottle card.
- New Bottle button.
- Quick Feed.
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
- Do not rely on color alone to communicate bottle status.
- Provide text labels for warning/expired states.

---

## Monetization Rules

Free tier:

- 1 baby.
- Basic bottle timer.
- Basic feed, diaper, and sleep logs.
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
- Custom timer rules.
- Widget themes.

Suggested prices:

- Monthly: USD 2.99
- Yearly: USD 19.99
- Lifetime: USD 29.99

Ads:

- Do not show interstitial ads in MVP unless explicitly requested.
- Do not show disruptive ads during active feeding flow.
- Do not show disruptive ads in night mode.
- Do not show disruptive ads immediately after a bottle expiry notification.
- Pro users must see no ads.

---

## Analytics

Track product events without collecting unnecessary personal data.

Suggested events:

- onboarding_started
- baby_created
- guideline_selected
- notification_permission_shown
- notification_permission_granted
- bottle_created
- bottle_started_feeding
- bottle_refrigerated
- bottle_expired
- bottle_discarded
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

- Formula room-temperature expiry.
- Formula feeding-started expiry.
- Formula refrigerated expiry.
- Breast milk room-temperature expiry.
- Breast milk refrigerated expiry.
- State transitions.
- Notification scheduling and cancellation.
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
3. Preserve timer rule tests.
4. Do not add medical claims.
5. Use the MVP backend only for clear product needs: account, backup/sync foundation, entitlement verification, remote config, and future caregiver sharing. Avoid backend complexity beyond that.
6. Prefer small, reviewable changes.
7. Explain non-obvious tradeoffs in comments or commit notes.
8. When adding user-facing text, add localization keys.
9. When changing bottle timer logic, update tests.
10. When changing paywall or ad behavior, ensure night mode and reminder flows stay non-disruptive.
11. When changing backend/sync logic, verify offline behavior still works.
12. When changing backend data shape, update security rules/RLS and migration notes.

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
- Existing bottle timer behavior is not broken.
- UI works in light and dark mode.
- Backend failure does not break core local timer flows.
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
- Real-time family sharing in MVP unless explicitly requested
- Complex custom account system beyond lightweight Auth
- Hardware integration
- Nutrition recommendations
- Brand-specific formula recommendations

---

## Preferred User-Facing Copy

Use these patterns:

- "Start bottle timer"
- "Bottle timer expired"
- "Feeding started"
- "Mark as discarded"
- "Based on your selected guideline"
- "This app is a reminder tool, not medical advice."
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
