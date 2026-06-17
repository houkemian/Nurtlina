---
name: nurtlina-product-analysis
description: Use this skill for Nurtlina product analysis, feature planning, UX timing, monetization, retention, rating prompts, ads, paywalls, onboarding, notifications, or product copy review. It applies Nurtlina's local-first baby care context, compliance language, caregiver empathy, Android MVP constraints, and privacy-safe analytics.
---

# Nurtlina Product Analysis

## Purpose

Use this skill when analyzing, planning, or reviewing Nurtlina product features. Optimize for tired caregivers using an Android baby bottle timer and care tracker, while preserving compliance, trust, speed, local-first behavior, and monetization boundaries.

## Product Lens

Always evaluate a product idea through these questions:

1. Does it make the core bottle timer or logging flow faster and calmer?
2. Could it appear during a stressful moment such as expiry, reminder handling, failed sync, purchase failure, or night use?
3. Does any copy imply medical advice, safety determination, or health authority endorsement?
4. Does it work when offline or when the backend fails?
5. Does it collect only the minimum analytics or personal data needed?
6. Does it preserve Free vs Pro boundaries without making active feeding flows feel hostile?
7. Are user-facing strings localizable and accessible?

## Compliance Guardrails

Never propose or approve user-facing copy that says or implies:

- safe to drink
- guaranteed safe
- medically approved
- doctor recommended
- CDC/AAP/NHS approved
- diagnosis
- health assessment
- this milk is safe

Prefer:

- timer expired
- based on your selected guideline
- reminder tool, not medical advice
- follow your formula label, local guidance, and pediatrician's advice
- when in doubt, discard the milk

Guidelines may be referenced as public sources, never as endorsements.

## Feature Planning Workflow

For each feature or product surface, produce:

1. **Goal**: The user or business outcome.
2. **Trigger/context**: Where and when it appears.
3. **Blocklist**: Moments where it must not appear.
4. **User copy**: Calm, localizable wording with no medical claims.
5. **State/data**: Minimal persisted fields needed.
6. **Analytics**: Privacy-safe events and disallowed data.
7. **Edge cases**: Offline, backend unavailable, permission denied, night mode, large font, active timers.
8. **Acceptance criteria**: Concrete checks that define done.

Keep recommendations conservative by default. For MVP, prefer simple local behavior over complex backend, targeting, or real-time sync.

## Timing Rules For Interruptive UI

For modals, rating prompts, paywalls, ads, feedback requests, and permission education:

- Do not show during active feeding, active bottle creation, expiry handling, notification click-through, night mode, or error recovery.
- Do not show immediately after startup or onboarding unless the surface is required to continue.
- Prefer showing after a positive completed action and after the UI returns to a stable Today state.
- Use cooldowns and permanent dismissals.
- Never block the core timer or local log creation flow.

## Monetization Rules

Free tier can include ads and Pro upsells, but:

- No disruptive ads during active feeding.
- No interstitial ads in MVP unless explicitly requested.
- No disruptive ads in night mode.
- No ads immediately after expiry notification click-through.
- Pro users must see no ads.
- Paywall copy must avoid medical or safety guarantees.

## Analytics Rules

Good event examples:

- rating_prompt_shown
- paywall_viewed
- bottle_created
- bottle_started_feeding
- feed_logged
- sync_failed

Never log:

- baby real names
- notes content
- detailed health concerns
- user-entered sensitive medical information
- synced baby care records for ad targeting

Prefer bucketed properties over exact sensitive values.

## Output Style

When answering product-analysis requests:

- Lead with the recommended policy or decision.
- Include clear trigger and anti-trigger rules for any prompt or monetization surface.
- Provide exact copy when copy is involved.
- Call out compliance risks explicitly.
- End with implementation-ready acceptance criteria when the request is planning-oriented.
