# Privacy and data flows

Principle: "As suas conversas ficam privadas."

This document describes every data source, what is read, where it is processed, how long it lives, and what is never done. It reflects the code as of the MVP. Any change to permissions, storage or network access must update this file first.

## Short version

- The app has **no internet permission**. Nothing it reads can leave the phone through the app. This is enforced by a test (`app/src/test/.../ManifestPolicyTest.kt`).
- Message content is analysed **in memory** and discarded when the analysis ends.
- The app **stores no messages, names, numbers or links**. It stores four settings (below).
- Warning notifications contain **no message content**.
- App data is **excluded from cloud backup and device transfer**.

## Data sources

| Source | Fields read | When | Processing | Lifetime |
|---|---|---|---|---|
| WhatsApp notification (`com.whatsapp`, `com.whatsapp.w4b`) | Title (contact name or number), newest incoming message text, group flag, post time | Each new notification, if the user enabled notification access and protection is not paused | In memory on the phone | Until analysis ends (milliseconds); a salted fingerprint is kept for de-duplication (see below) |
| Email app notification (Gmail, Outlook, Samsung Email, SAPO Mail, Yahoo Mail) | Title (sender display name), subject line, preview text | Same | Same | Same |
| Any other app's notification | **Nothing.** The package name is compared with the supported list before any field is read | Each notification | Package name comparison only | Not retained |
| Text shared into the app or selected with "Verificar burla" | The text, up to 20,000 characters | When the user chooses to share | In memory | While the result screen is open; removed from the stored intent |
| Text typed or pasted in "Verificar uma mensagem" | The text | When the user taps Verificar | In memory | While the screens are open |
| Clipboard | The current clipboard text | Only when the user taps "Colar mensagem" | In memory | Same as typed text |

The app does not request access to contacts, SMS, call logs, files, location, the microphone, the camera or accessibility services.

## Data derived from analysis

| Data | Content | Where | Lifetime |
|---|---|---|---|
| Risk assessment | Level, signal types, short evidence excerpts | Memory | Discarded with the screen or after the alert is sent |
| Alert payload | Enum codes only: level, confidence, source type, signal types, actions, limitations | The warning notification's intent | Until the notification is dismissed |
| Last alert | The same codes and a timestamp | Memory (`RecentAlerts`) | Until the app process ends |
| De-duplication fingerprints | SHA-256 of a random per-process salt plus source, sender and text | Memory | At most 6 hours and 200 entries; lost when the process ends. The random salt means fingerprints cannot be compared with a list of known texts or across restarts |

Evidence excerpts (up to three short quotes from the message) are shown only on the result screen of a check the user started. They are never put in notifications or intents. IBANs are masked in evidence.

## Stored settings

Stored in the app's private `SharedPreferences`:

1. Onboarding completed (yes/no).
2. Automatic protection paused (yes/no).
3. Trusted person's name, optional, entered by the user.
4. Trusted person's phone number, optional, entered by the user.

Items 3 and 4 are personal data of a third person, entered voluntarily, used only to open the phone app (`ACTION_DIAL`, which does not place a call). They can be deleted with "Apagar" in the Help screen, or by clearing the app's data or uninstalling.

There is **no risk history** in the MVP. If a history is added later, it must be opt-in, store only codes and dates, and provide deletion.

## External services

None. The app has no internet permission, no analytics SDK, no crash-reporting SDK, and no advertising SDK.

The optional interfaces for link reputation and AI (`ThreatIntelligenceProvider`, `InferenceProvider`) are not connected to any service. If one is connected later:

- Link reputation receives host names only (tested in `ThreatIntelTest`), because full links can contain personal tokens.
- AI receives redacted text only (tested in `InferenceTest`).
- Both require adding internet permission, which fails `ManifestPolicyTest` until this document, the in-app privacy text, the Google Play Data safety form and the consent flow are updated.

## The user's own sharing

"Pedir ajuda a alguém de confiança" opens Android's share sheet with a short text: the warning headline and reasons, and a request to call. **It never includes the message itself.** The user chooses the recipient and app. The app cannot see what happens afterwards.

## Logging and failures

- Analysis code may only log through `SafeLogger`, which accepts an event code and numbers. No logger is connected in the MVP.
- A test (`PrivacyTest`) fails if engine code contains `println`, `System.out`, file writers or network classes.
- A test (`ManifestPolicyTest`) fails if app code uses `Log.`, `println` or `printStackTrace`.
- `toString()` of every class that holds message content returns a redacted placeholder, so an accidental log or crash message cannot include content (tested).
- Errors in a detector, in link reputation or in AI degrade to the remaining results; they do not crash and do not record content.

**Not fully under our control**: if the app crashes, Android may generate a crash report. With no crash SDK, this report goes only to Google Play's developer console (Android vitals) if the user allowed diagnostics. It contains a stack trace, and because `toString()` is redacted, it should not contain message content. This has not been tested with a real crash.

## Lock screen

Warning notifications use `VISIBILITY_PRIVATE` with a neutral public version: "O Guardião tem um aviso para si. Desbloqueie o telemóvel para ver." The private version contains only the risk headline and the source type (for example "Mensagem do WhatsApp").

## Backup

`android:allowBackup="false"`, plus `dataExtractionRules` that exclude every domain from cloud backup and device-to-device transfer (Android 12+), plus `fullBackupContent` exclusions for Android 11 and older. Consequence: settings (including the trusted person) are not restored on a new phone.

## What the operating system and other apps do

We do not control:

- WhatsApp and email apps, which store their own messages.
- Android's own notification history, if the user enabled it, which stores the original notifications (not our warnings' content, because they have none).
- The keyboard app, if the user types or pastes a message.
- The clipboard, which other apps may read in some Android versions.

The privacy text in the app says "Não guardamos mensagens", not "nenhuma mensagem é guardada em lado nenhum".

## Legal assessment (preliminary, not legal advice)

**GDPR applies.** Message content is personal data of the user and of third parties (the senders). The app's controller is whoever publishes it.

- **Role**: because all processing happens on the user's device and nothing reaches the publisher, the publisher does not receive personal data from messages. The publisher is still responsible for the design (Article 25, data protection by design and by default) and for transparency (Articles 12 to 14) in the app and store listing.
- **Household exemption** (Article 2(2)(c)): may cover the user's own processing on their phone, but does not remove the publisher's obligations for how the tool is designed. Get legal confirmation.
- **Lawful basis**: consent for notification access (explicit, granular, revocable in Android settings), or legitimate interest of the user in fraud protection for the senders' data. Senders are not informed; the minimisation measures (in memory, no storage, no transfer) are what make this proportionate.
- **Article 22 (automated decisions)**: the app does not take decisions with legal or similarly significant effects. It does not block, delete or send anything. It gives advice that the user can ignore. Profiling of senders does not occur: no data about a sender is kept.
- **Special categories**: message content may reveal health, religion and so on. It is processed transiently and not stored. A DPIA (Article 35) is advisable because the app reads communications of third parties systematically, even if transiently. Recommended before public release.
- **ePrivacy (Directive 2002/58/EC, Lei n.º 41/2004 in Portugal)**: the confidentiality of communications obligations bind providers of communication services; reading a notification on the recipient's own device with the recipient's consent is different from interception. Get legal confirmation, especially if any server component is added.
- **Portugal**: Lei n.º 58/2019 implements the GDPR. The CNPD is the supervisory authority.
- **EU AI Act**: the MVP uses no AI model. If an on-device or cloud model is added, a fraud-warning assistant for consumers is unlikely to be high-risk under Annex III, but the transparency obligations of Article 50 and the general-purpose model provider's terms should be checked at that point.

## Known limitations

- Device testing of the listener has not happened yet; the field mapping is based on Android documentation.
- The de-duplication fingerprint, although salted and in memory, is still derived from content. It is the minimum we found that prevents repeated alerts.
- If the user's phone has malware with accessibility access, it can read the same screens our app shows. This is outside our control.
