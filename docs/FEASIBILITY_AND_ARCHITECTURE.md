# Feasibility and architecture

Status: October 2026, MVP (Phase 1 complete, Phase 2 partially implemented).

This document records what is technically possible for observing WhatsApp and email content on Android without modifying those apps, what we chose, and why. It separates verified facts from assumptions.

## Evidence quality

Research was done on 2026-10-21 from a sandbox with restricted network access. Pages were classified as:

- **Fetched**: the page was read directly (mostly developer.android.com, cloud.google.com pricing, AOSP source, Microsoft's official docs repository on GitHub).
- **Snippet**: only search-result excerpts were available. support.google.com (all Google Play policy pages), developers.google.com, whatsapp.com, faq.whatsapp.com and developers.facebook.com could not be opened.

**Before a Google Play submission, every policy statement marked "snippet" must be re-checked on the live page.** This is listed as a release blocker in [SECURITY_REVIEW.md](SECURITY_REVIEW.md).

## Summary of the decision

| Source | Approach | Classification | MVP status |
|---|---|---|---|
| WhatsApp | Notification listener (A) | Best-effort automatic detection | Implemented, not yet verified on a real device |
| Email apps (Gmail, Outlook, others) | Notification listener (A) | Best-effort, preview only (subject and first lines) | Implemented, not yet verified on a real device |
| Any app | Share sheet and text-selection menu (C) | User-initiated analysis | Implemented |
| Any app | Copy and paste into the app (C) | User-initiated analysis | Implemented |
| Gmail full body | Gmail API (B) | Possible, but restricted scope with yearly security assessment | Not implemented, not recommended for MVP |
| Outlook full body | Microsoft Graph (B) | Possible with user consent | Not implemented, optional later |
| Screen content | AccessibilityService (D) | Technically possible, policy-ineligible | Rejected |
| WhatsApp official API (E) | Business Platform | Not applicable to personal accounts | Not possible |

The app works fully without notification access: manual and shared-text checks are always available.

## A. NotificationListenerService

**What it provides** (fetched, developer.android.com `Notification`, `NotificationListenerService`):

- `EXTRA_TITLE`, `EXTRA_TEXT`, `EXTRA_BIG_TEXT`, `EXTRA_TEXT_LINES`, and for chat apps `EXTRA_MESSAGES` (MessagingStyle messages with text, timestamp and sender `Person`), `EXTRA_CONVERSATION_TITLE`, `EXTRA_IS_GROUP_CONVERSATION`.
- Group summaries carry `FLAG_GROUP_SUMMARY` and should be skipped.
- Only what the sending app put in the notification. For WhatsApp this is usually the full text of short messages and the saved contact name, or the phone number if the sender is not a saved contact. For email apps it is the sender display name, subject and a short preview, never the full body or the sender address.

**What it does not provide**

- No history. A notification shows recent messages of a chat, but nothing before the app was installed, nothing from chats that are open on screen, muted, or archived, and nothing when the user has turned off previews.
- No media content: photos, audio and documents appear as placeholders.
- No reliable sender address or phone number for saved contacts. The display name is whatever the user saved.
- **Android 15 and later redact notifications that contain a one-time code** for listeners that are not trusted (fetched, Android 15 behavior changes, "OTP Redaction"; AOSP `NotificationManagerService`). The title becomes the app name and the text becomes "Sensitive notification content hidden". The permission that lifts this, `RECEIVE_SENSITIVE_NOTIFICATIONS`, is reserved for system and privileged apps. Consequence: the app cannot see the code itself. It can still see that a hidden notification arrived from WhatsApp; this is recorded as `REDACTED` and never treated as "safe".

**Consent and permissions**: the user must enable the app in Android's "Notification access" settings page. It is not a runtime dialog. The app opens the page directly (`ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` on Android 11+, falling back to `ACTION_NOTIFICATION_LISTENER_SETTINGS`), after an in-app explanation.

**Sideloading**: since Android 13, apps installed outside an app store cannot enable notification access without an extra hidden "Allow restricted settings" step. In some countries, Play Protect blocks installing sideloaded apps that declare a notification listener (snippet; confirmed for Singapore and Malaysia).

**Observed in Portugal on 2026-10-23**: installing the debug APK from the phone's file manager on a Samsung phone was blocked by Play Protect ("App bloqueada para proteger o dispositivo. O Play Protect ainda não viu uma app deste programador."). The block may be due to the unknown developer, the notification listener, or both; this was not isolated.

**Conclusion: distribution must be through Google Play.** For device testing before a release, use a Google Play internal testing track, or (developer phones only) temporarily switch off Play Protect scanning to install the debug build. Testers then also need the "Allow restricted settings" step.

**Battery and reliability**: the system binds the listener and rebinds it after reboot. Aggressive battery managers on some manufacturers can disconnect it. Analysis is local and takes milliseconds, so the battery cost per notification is small. Not yet measured on a device.

**Google Play**: no specific declaration form for notification listeners was found (snippet). The User Data policy applies: a prominent in-app disclosure, explicit consent, a privacy policy, and an accurate Data safety section. The "Permissions and APIs that Access Sensitive Information" page (answer 16558241, snippet only) requires requesting access in context, only for features shown in the store listing, and never selling data.

**MVP use**: implemented in `app/.../notify/MessageNotificationListener.kt`. Only packages in `SupportedApps` are read; the package name is checked before any content is touched. Only the newest incoming message is analysed. Messages written by the phone's owner (MessagingStyle messages without a `Person`) are skipped.

## B. Email provider APIs

**Gmail API** (snippet): `gmail.readonly` and `gmail.metadata` are both restricted scopes. Production use requires Google OAuth verification and a CASA security assessment repeated every year. Third-party estimates of the cost range from a few hundred to tens of thousands of US dollars a year, plus about six weeks. Without verification, an app is limited to 100 test users with an "unverified app" warning.

**Microsoft Graph** (fetched, Microsoft docs repository): delegated `Mail.Read` is available to personal Outlook and Hotmail accounts without admin consent. Without publisher verification, many users cannot consent to new multi-tenant apps.

**Both would require the app to have internet access and to receive full email content on the device through an OAuth token.** That changes the privacy model substantially.

**MVP use**: none. The email notification preview plus user-initiated checks cover the main email patterns (fake bank, fake tax authority, fake courier) because the scam is usually visible in the subject and first lines. Revisit Microsoft Graph first if users ask for full-email checks.

## C. User-initiated sharing

- `ACTION_SEND` with `text/plain` (fetched): the app appears in the Android share sheet.
- `ACTION_PROCESS_TEXT` (fetched): the app appears as "Verificar burla" in the text-selection menu of any app where text can be selected.
- WhatsApp: a text message can be copied (long press, Copy) but not shared to another app; forward works only inside WhatsApp. Copy and paste, or selecting text where possible, is the practical route.
- Gmail: no "share this email" action. Users can select text and use the selection menu, or copy and paste.

**MVP use**: implemented in `MainActivity.handleIntent`. Shared text is analysed immediately, and the text is removed from the stored intent afterwards.

## D. AccessibilityService

Google Play's policy (snippet, answer 10964491) reserves `isAccessibilityTool` for apps that help people with disabilities and names antivirus and monitoring apps as examples that do not qualify. Other uses need a declaration, prominent disclosure, and face the strictest review. It is also the main technique used by Android banking malware, and the system warns users that the app gets "full control" of the device. That warning would undermine trust with exactly the users we serve.

**MVP use**: rejected. Not declared in the manifest. A test (`ManifestPolicyTest`) fails if it is added.

## E. WhatsApp official integrations

There is no official API that lets a third party read a consumer's personal WhatsApp messages (snippet; WhatsApp Business Platform documentation). The Cloud API serves a business's own number only. WhatsApp's terms forbid collecting user data by automated or unauthorised means, so WhatsApp Web scraping and unofficial libraries are excluded.

A legitimate future option is a "tip line": users forward a suspicious message to our own WhatsApp Business number. This sends content to a server and has per-conversation costs, so it is outside the MVP.

WhatsApp has its own protections the app could recommend turning on: two-step verification, "Silence unknown callers", the safety overview shown when a stranger adds you to a group (August 2025, snippet).

## Architecture

```
app (Android, Kotlin, Jetpack Compose, Material 3)
  MainActivity        UI state machine, share and text-selection intents
  ui/                 screens and design system
  notify/             MessageNotificationListener, AlertNotifier
  data/               Settings (non-sensitive), ProtectionStatus, RecentAlerts (memory only)
        |
        v
core (plain Kotlin, no Android dependency, unit-tested on any JVM)
  ingest/     MessageSourceAdapter: NotificationAdapter, SharedTextAdapter, SupportedApps
  text/       NormalizedText (folding accents and lookalike letters), EntityExtractor
  context/    ContextProvider, ConversationContext, DisplayNameContextProvider
  detect/     FraudSignalDetector: PhraseRuleDetector + Lexicon, StructuredDataDetector, SenderMetadataDetector
  links/      UrlAnalyzer, LinkParser, OfficialDomains (never opens links)
  intel/      ThreatIntelligenceProvider (NoThreatIntelligence, LocalBlocklist)
  ai/         InferenceProvider, BoundedInference (optional, validated, cannot lower risk)
  policy/     RiskPolicy (combination rules, uncertainty, recommended actions)
  explain/    ExplanationGenerator, PtPtCatalog, EnglishCatalog
  alert/      AlertDispatcher, AlertPayload (codes only), AlertDeduplicator, AlertThreshold
  privacy/    Redactor, SafeLogger
  FraudAnalyzer   the bounded pipeline
```

Mapping to the requested interfaces: `MessageSourceAdapter`, `ContextProvider`, `FraudSignalDetector`, `ThreatIntelligenceProvider`, `InferenceProvider` and `AlertDispatcher` exist with those names. `MessageNormalizer` is `NormalizedText` plus `EntityExtractor`. `RiskAssessmentEngine` is `FraudAnalyzer` with `RiskPolicy`. `ExplanationGenerator` exists with that name.

### Pipeline

1. **Normalise**: bound to 8,000 characters; fold case, accents and common Cyrillic and Greek lookalike letters, keeping a one-to-one character map so evidence can be quoted from the original.
2. **Extract**: links (with and without `https://`), phone numbers, IBANs, amounts, email addresses.
3. **Context**: for WhatsApp, a title that looks like a phone number means the sender is not a saved contact. This is recorded as an inference. Nothing else about conversation history is assumed.
4. **Deterministic detectors**: phrase rules in European Portuguese (with common Brazilian variants) and English; local link analysis; IBAN detection; sender name versus domain checks; risky attachment types. A detector that throws is ignored, not fatal.
5. **Link reputation** (optional, off in the MVP): receives host names only, never full URLs. Time-limited. "Not listed" never counts as safe.
6. **AI** (optional, off in the MVP): see below.
7. **Policy**: combination rules decide the level. See [TESTING_AND_EVALUATION.md](TESTING_AND_EVALUATION.md).
8. **Explanation**: built from codes only, at most three reasons and four actions.
9. **Alert**: only HIGH and CRITICAL produce a notification. The notification carries codes, not content.

### Decisions

**Decision: analyse on the phone, with no internet permission.**
Business reason: the core promise ("as suas conversas ficam privadas") becomes verifiable: anyone can check the manifest. No server costs. No processor agreements needed for message content.
Trade-off: no live link reputation and no cloud AI. Detection relies on rules, which generalise poorly (see the holdout results). Adding internet later is possible, but must be a visible, consented change.

**Decision: the risk level comes from combinations of tactics, not from counting keywords.**
Business reason: older adults stop trusting a tool that cries wolf. Urgency or money vocabulary alone never produces an automatic alert.
Trade-off: some scams with a single strong sign are shown as "Merece atenção" instead of an alert.

**Decision: alerts never contain message content.**
Business reason: the lock screen, Android's notification history, and the intent that opens the app never hold the message.
Trade-off: when the user opens an alert, the app shows the reasons, but cannot quote the message (it was discarded).

**Decision: plain Kotlin engine separate from the Android app.**
Business reason: the engine can be tested and improved without a phone, and reused later (for example in an iOS app or a server-side tip line).
Trade-off: one extra module.

**Decision: no dependency-injection framework, no navigation library, no database.**
Business reason: fewer third-party components to audit; the app is small.
Trade-off: if the app grows significantly, these may become worth adding.

### AI layer

The AI layer is designed but no model ships in the MVP. `BoundedInference` enforces:

- input is redacted (phone numbers, emails, IBANs, card numbers, full links reduced to host names) and limited to 2,000 characters;
- a 3-second timeout; failures degrade to deterministic results and are noted as a limitation;
- the model may only report signal types from a fixed list; unknown types and confidence values are discarded;
- each signal must quote text that appears in the message; invented evidence is discarded;
- model findings are capped at medium confidence and marked as inferred;
- the policy counts a model finding only if a deterministic rule found a different tactic, so a model alone cannot raise a message above "Merece atenção", and nothing a model says can lower a deterministic result or declare a sender legitimate.

Recommended next step: evaluate an on-device model (for example Gemini Nano through Android's AICore, where available, or a small quantised classifier) against the holdout corpus. On-device availability varies by phone model; this must be verified before committing. A cloud model would require internet permission, explicit opt-in consent, a processor agreement, and an update to the privacy documentation.

## High-risk assumptions

| Assumption | Status | How to verify |
|---|---|---|
| WhatsApp notifications contain the full text of short messages | Not verified in this environment | Test on 2 or 3 real phones with WhatsApp previews on |
| WhatsApp shows the phone number as title for unsaved senders | Widely observed, not verified here | Same device test |
| Email app previews contain enough of the scam | Partly; depends on the app | Device test with Gmail and Outlook |
| Google Play accepts the notification listener for this purpose | Not verified; policy pages could not be opened | Read live policies, prepare disclosure and Data safety form, consider a pre-review |
| Phrase rules generalise to real messages | **Contradicted by holdout: recall 0.33** | Larger real-world-like corpus; on-device model |
| Older adults understand and act on the warnings | Not tested | Moderated usability sessions |
