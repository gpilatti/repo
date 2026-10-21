# Threat model and scam scenarios

This document lists the scam patterns the app targets in Portugal, the sources that document them, what the app can and cannot observe, and the expected result. Test fixtures are in `core/src/test/resources/scenarios/` (tuning corpus) and `core/src/test/resources/holdout/` (not used for tuning).

## About the sources

Research date: 2026-10-21. From the research sandbox, official Portuguese pages (policiajudiciaria.pt, bportugal.pt, mbway.pt, cgd.pt, ctt.pt) could not be opened directly. Every source below was identified through search results, and the wording is quoted from search snippets. Marked:

- **[official]**: the URL is the official body's own page; content seen as a search snippet.
- **[press]**: press coverage quoting an official body.

**Before user-facing text cites any of these, open each page and confirm the wording and date.** The fixtures are synthetic: no real messages, names, numbers or accounts were used. Phone numbers use the unallocated `+351 900 000 xxx` range, IBANs are all zeros, and domains use the reserved `.example` ending except where a test needs a real ending such as `.top`.

## What the app can and cannot observe, in general

| Observable | WhatsApp notification | Email notification | Shared or pasted text |
|---|---|---|---|
| Message text | Usually full for short messages | Subject and first lines only | Full |
| Sender saved as contact | Inferred: title is a name or a number | No | No |
| Sender email address | No | No | Only if the user includes it |
| Link destination behind link text | No | No | No (plain text only) |
| Conversation history | No | No | No |
| Voice call refusal, profile photo | No | No | No |
| Account takeover of a known contact | Only from content | Only from content | Only from content |
| OTP messages on Android 15+ | Hidden by Android | Hidden by Android | Yes, if the user pastes them |

## Scenarios

Expected levels: `NO_WARNING` (Sem sinais de alerta), `CAUTION` (Merece atenção), `HIGH` (Risco alto), `CRITICAL` (Risco muito alto). Automatic notifications are shown for HIGH and CRITICAL only.

### 1. Fake MB WAY or bank email asking for card or account details

- **Sources**: PJ, "Alerta de mensagem phishing: parceria da PJ com a MBWAY", https://www.policiajudiciaria.pt/alerta-de-mensagem-phishing-parceria-da-pj-com-a-mbway/ [official]; Público and Observador, 2023-02-09 [press]. PJ: "entre a Polícia Judiciária e a SIBS não foi feita qualquer parceria com este fim".
- **Narrative**: an email claims a cooperation agreement between MB WAY and the PJ and asks the recipient to update Multibanco card details through a link.
- **Observable**: bank or payment brand and authority named together; "atualizar os dados do cartão"; link to a non-official domain; sender name versus domain mismatch (if shared with sender details).
- **Contextual**: none.
- **Unavailable**: full email headers; real link behind link text in a notification.
- **Expected**: CRITICAL.
- **Action**: do not open the link; contact the bank through the number on the card. PJ: ignore, mark as spam, delete.
- **False positives**: genuine bank emails that mention "atualizar dados" and link to the bank's real domain (official domains are recognised).
- **Tests**: S01-01, S01-02, S01-03.

### 2. Fake bank security notification with a malicious link

- **Sources**: CGD security pages, "Fraude de SMS e chamadas falsas" (13-08-2026 and 02-10-2026), https://www.cgd.pt/Ajuda/Seguranca/Pages/02-10-2026-fraude-sms-chamadas-falsas.aspx [official]; PGR Gabinete Cibercrime alert on Novo Banco phishing, 2024-07-17 [official]; PJ "Operação Token out" [official]. CGD: it does not send SMS with links and never asks for codes.
- **Narrative**: "acesso bloqueado", "validar dados"; a fake page; then often a call from a fake employee asking for SMS codes.
- **Observable**: bank name; threat of blocking or suspension; lookalike, shortened or IP-address link; request for a code.
- **Unavailable**: spoofed SMS sender identity; the follow-up phone call.
- **Expected**: HIGH to CRITICAL.
- **Action**: do not open the link; never give codes; call the bank on the official number.
- **False positives**: genuine bank notices that link to the official domain.
- **Tests**: S02-01 to S02-04, D-01, D-06, D-08.

### 3. "Olá pai, olá mãe": a relative on a new number asking for money

- **Sources**: PJ, "Burlas 'Olá Pai… olá Mãe'", https://www.policiajudiciaria.pt/burlas-ola-pai-ola-mae/ [official]; PSP coverage [press]; Millennium bcp and Cofidis warnings with example wording [official, company pages]. PJ advice: call your child by voice on the number you already have, even if the profile has their photo.
- **Narrative**: unknown number, "perdi o telefone", "este é o meu novo número", then an urgent payment request by MB WAY or transfer, often to someone else's account.
- **Observable**: family address; new-number story; money request; urgency; secrecy; IBAN.
- **Contextual**: WhatsApp shows a number instead of a saved name.
- **Unavailable**: refusal of voice calls; payee name; profile photo.
- **Expected**: first message (new number only) CAUTION; with a money request CRITICAL.
- **Action**: call the relative on the number already saved; do not use the new number; do not send money.
- **False positives**: a relative who genuinely changed numbers (CAUTION, with advice to call the old number, is the intended outcome).
- **Tests**: S03-01 to S03-05.

### 4. Fake police, government or tax authority

- **Sources**: AT alerts on fake "penhora" SMS, February 2026 (CM Jornal, Sábado) [press]; Segurança Social alerts, 2024 and 2026 [press]; PJ, "Alerta campanha de phishing em nome da Polícia Judiciária" [official]; PGR, "Falsas convocatórias policiais", 2022 [official]; Banco de Portugal communiqué on spoofed phone number [official].
- **Narrative**: debt, seizure, fine or criminal case; pay through a link or reply.
- **Observable**: authority name; threat; payment demand; link; free-mail sender (if shared).
- **Unavailable**: spoofed caller ID; attachments in notifications.
- **Expected**: HIGH to CRITICAL.
- **Action**: check in Portal das Finanças or Segurança Social Direta, never through the link. Police do not send notices of this kind by email.
- **False positives**: genuine AT or Segurança Social reminders without links or payment requests (B-16 in holdout).
- **Tests**: S04-01 to S04-04.

### 5. Delivery company asking for a small payment

- **Sources**: CTT, "Procedimentos para Alertas de Phishing", https://www.ctt.pt/transversais/alertas-de-phishing [official], with examples dated 11/02/2026 and 28/04/2026; PGR alert on CTT phishing, 2023-07-05 [official].
- **Narrative**: parcel held for customs or failed delivery; pay EUR 1 to 3 through a link.
- **Observable**: courier name; "encomenda retida"; fee; link not on ctt.pt.
- **Unavailable**: whether the user is expecting a parcel.
- **Expected**: HIGH to CRITICAL.
- **Action**: do not pay through the link; check on ctt.pt with the tracking number.
- **False positives**: genuine delivery confirmations (B-04, B-07); payment references the user requested in the CTT customs portal (not covered, see limitations).
- **Tests**: S05-01 to S05-03.

### 6. Fake refund, prize, inheritance or investment

- **Sources**: Ministério Público and PSP alerts on crypto ads with celebrities (RTP) [press]; PJ advice to check with CMVM and Banco de Portugal [press]; AT alerts on fake IRS refunds [press]; Europol and EBF #CyberScams campaign [official].
- **Observable**: prize, refund, inheritance or guaranteed-return wording; link; crypto or gift card; request to install an app.
- **Expected**: HIGH to CRITICAL.
- **Action**: do not pay fees to receive money; do not install apps; check with CMVM or Banco de Portugal.
- **False positives**: genuine insurance or tax refund notices without a link (B-14).
- **Tests**: S06-01 to S06-04.

### 7. Impersonated contact asking for secrecy and an immediate transfer

- **Sources**: PJ alert on rising CEO fraud, February 2026 (Renascença) [press]; Banco de Portugal on IBAN-change fraud [official]; PJ on hijacked email accounts [official].
- **Observable**: transfer request; secrecy; urgency; IBAN.
- **Unavailable**: whether the sender's account is the real one.
- **Expected**: CRITICAL.
- **Action**: confirm by phone on a known number; talk to someone before paying.
- **Tests**: S07-01 to S07-03.

### 8. Request for an OTP, PIN, password or recovery code

- **Sources**: MB WAY security pages, https://www.mbway.pt/seguranca-mb-way/ and https://www.mbway.pt/evite-burlas-com-mb-way/ [official]: never add a number that is not yours; the PIN is personal "nem como forma de pagamento nem como garantia". Banco de Portugal on second-hand marketplaces [official]. WhatsApp code takeover: only press sources found (Observador 2021, PSP coverage 2026); no official Portuguese page was found.
- **Observable**: request to send back a code; "por engano"; ATM instructions with someone else's number; remote access apps.
- **Unavailable**: on Android 15+, the WhatsApp code notification itself is hidden by Android.
- **Expected**: CRITICAL.
- **Action**: never share codes, even with someone you know.
- **False positives**: **known**: MB WAY withdrawal codes can legitimately be shared with the family member who will withdraw the money (MB WAY guidance). A message such as "Mãe, manda-me o código de levantamento" will be rated CRITICAL. This is accepted for now because the same pattern is used by scammers; the explanation recommends confirming by phone. Needs user research.
- **Tests**: S08-01 to S08-04, D-03.

### 9. Familiar contact whose account was compromised

- **Sources**: PSP warning on WhatsApp account takeover, September 2026 (Postal) [press]; PJ on email credential capture [official].
- **Observable**: content only: urgent money, code requests, voting links.
- **Contextual**: the sender **is** a saved contact, so "unknown sender" is absent. The app must not treat a familiar name as trust.
- **Expected**: HIGH to CRITICAL.
- **Action**: call the person by voice; agree a family code word (PSP advice).
- **Tests**: S09-01 to S09-03.

### 10. Legitimate but unusual messages (controls)

- **Sources**: Banco de Portugal: "o seu banco nunca lhe solicitará os seus dados de acesso… por telefone, e-mail ou SMS" (clientebancario.bportugal.pt) [official]; CGD, MB WAY, CTT and AT statements on what they do and do not send [official].
- **Purpose**: check that urgency, money, banks or codes alone do not cause alarms. Examples: an OTP with "não partilhe este código", a bank safety notice, a family member asking to be repaid for dinner, an urgent work meeting, a condominium payment notice.
- **Expected**: NO_WARNING, at most CAUTION.
- **Tests**: B-01 to B-14, A-01, A-02, holdout H-10 to H-16.

## Attacks against the app itself

| Threat | Mitigation | Test |
|---|---|---|
| Message tells an AI to classify it as safe | Deterministic rules decide; AI can only add grounded findings; tool-manipulation text is itself a signal | `InferenceTest`, D-03 |
| Lookalike letters (Cyrillic "С") | Folding of common confusables | D-06 |
| Link with `brand.pt@evil.example` | User-info trick detected | `LinkTest` |
| Punycode domain | Flagged | D-04 |
| Malformed link to crash the parser | Parser returns null; finding is "link could not be read" | D-05, `LinkTest` |
| Very long message to slow the phone | Bounded to 8,000 characters; flagged as incomplete | `RuleTest` |
| Notification flood | De-duplication; analysis is cheap; only supported apps are read | `AlertAndIngestionTest` |

See [SECURITY_REVIEW.md](SECURITY_REVIEW.md) for the full review.
